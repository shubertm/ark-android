package com.arkade.core.services

import com.arkade.core.ArkServerInfo
import com.arkade.core.batches.BatchEvent
import com.arkade.core.batches.BatchSession
import com.arkade.core.intents.ArkIntent
import com.arkade.core.wallet.Wallet
import com.arkade.network.ArkadeClient
import com.arkade.repositories.contracts.ContractRepo
import com.arkade.utils.Log
import com.arkade.utils.debug
import com.arkade.utils.error
import com.arkade.utils.info
import com.arkade.utils.warning
import fr.acinq.bitcoin.Crypto.sha256
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Coordinates the lifecycle of batches on behalf of the wallet's registered intents.
 *
 * Consumes the server's batch event stream, matches each new batch to the currently
 * [activeIntents] it concerns, and creates/drives one [BatchSession] per matched intent to
 * finalize the batch and cooperatively sign the resulting VTXO tree.
 *
 * @property client Used to obtain the batch event stream and server info, and to confirm intent
 * registrations.
 * @property wallet Used to look up VTXOs/contracts for an intent's inputs and to sign
 * transactions on behalf of [BatchSession]s.
 * @property contractsRepo Used to resolve the [com.arkade.core.contracts.ArkContract] backing
 * each VTXO referenced by an intent.
 */
class BatchManagementService(
    private val client: ArkadeClient,
    private val wallet: Wallet,
    private val contractsRepo: ContractRepo,
) {
    private var streamId: String? = null
    private val activeIntents: MutableMap<String, ArkIntent> = mutableMapOf()
    private val activeBatchSessions: MutableMap<String, BatchSession> = mutableMapOf()
    private val batchIdToIntentIds = mutableMapOf<String, HashSet<String>>()

    /**
     * Subscribes to the server's batch event stream and dispatches every event to
     * [processEvent] until cancelled.
     *
     * If the stream fails, it is resubscribed after a [EVENT_STREAM_RETRY_DELAY] delay, up to
     * 8 attempts; a [CancellationException] is rethrown immediately without retrying. This
     * function suspends for as long as the stream is being collected and does not return
     * normally under regular operation.
     */
    suspend fun start() {
        Log.debug(LOG_TAG, "Starting an event stream")

        streamId = null
        // Get all topics

        client
            .getBatchEventStream()
            .retryWhen { cause, retries ->
                if (cause is CancellationException) throw cause

                Log.error(LOG_TAG, "Error in batch event stream, restarting in 5 seconds")
                delay(EVENT_STREAM_RETRY_DELAY)
                retries <= 7
            }.collect { event ->
                processEvent(event)
            }
    }

    /**
     * Routes [event] to the appropriate handler based on its type.
     *
     * [BatchEvent.StreamStartedEvent] records the current [streamId]. A
     * [BatchEvent.BatchStartedEvent] is matched against [activeIntents] to set up new
     * [BatchSession]s. Every other event is forwarded to the sessions already associated with
     * its batch id via [handleBatchEvent].
     */
    private suspend fun processEvent(event: BatchEvent) {
        when (event) {
            is BatchEvent.StreamStartedEvent -> {
                streamId = event.id
                Log.info(LOG_TAG, "Batch stream started with id: $streamId")
            }
            is BatchEvent.BatchStartedEvent -> {
                handleBatchStartedForAllIntents(event)
            }
            else -> {
                handleBatchEvent(event)
            }
        }
    }

    /**
     * Selects the [activeIntents] concerned by [event] and creates a [BatchSession] for each.
     *
     * An intent is selected when the SHA-256 hash of its id is present in
     * [BatchEvent.BatchStartedEvent.intentIdHashes]. Intents that already have a session in
     * [activeBatchSessions] are skipped. For each remaining intent, [setupBatchSession] is
     * called; failures are logged and do not prevent other intents from being set up.
     *
     * @param event The batch-started event carrying the hashed ids of the intents included in
     * the new batch.
     */
    private suspend fun handleBatchStartedForAllIntents(event: BatchEvent.BatchStartedEvent) {
        val intentHashMap =
            activeIntents.mapKeys { entry ->
                sha256(entry.key.encodeToByteArray()).toHexString()
            }

        val selectedIntents =
            try {
                event.intentIdHashes.mapNotNull { intentHash ->
                    intentHashMap[intentHash]
                }
            } catch (e: Exception) {
                throw e
            }

        if (selectedIntents.isEmpty()) return

        val walletIds =
            selectedIntents
                .map { intent ->
                    intent.walletId
                }.distinct()
                .toTypedArray()

        if (walletIds.isEmpty()) return

        val serverInfo = client.getInfo()

        selectedIntents.forEach { intent ->
            val intentId = intent.id
            if (activeBatchSessions.containsKey(intentId)) {
                return@forEach
            }
            try {
                setupBatchSession(intent, serverInfo, event)
            } catch (e: Exception) {
                Log.warning(LOG_TAG, "Failed to handle batch started event for intent $intentId: $e")
            }
        }
    }

    /**
     * Builds and starts a [BatchSession] for [intent]'s VTXOs, then confirms the intent's
     * registration with the server.
     *
     * Loads [intent]'s VTXOs (including already-spent ones, so unrolled/swept coins are still
     * resolvable) and their backing contracts, converts each to an [com.arkade.core.coins.ArkCoin],
     * and creates and [BatchSession.init]-ializes a session for them. The session is registered
     * in [activeBatchSessions] and its batch id is associated with [intent]'s id in
     * [batchIdToIntentIds] before the registration is confirmed via
     * [ArkadeClient.confirmIntentRegistration] and the intent is persisted via
     * [Wallet.saveIntent].
     *
     * @param intent The intent whose inputs are being registered in this batch.
     * @param serverInfo The server info used to resolve each VTXO's script to its contract.
     * @param event The batch-started event that triggered this session.
     * @throws IllegalArgumentException if a VTXO or its backing contract cannot be found in
     * storage.
     */
    private suspend fun setupBatchSession(
        intent: ArkIntent,
        serverInfo: ArkServerInfo,
        event: BatchEvent.BatchStartedEvent,
    ) {
        val intentId = requireNotNull(intent.id)
        try {
            val walletIds = arrayOf(intent.walletId)
            val vtxos =
                wallet.getVtxos(
                    outpoints = intent.vtxos.toTypedArray(),
                    includeSpent = true,
                )

            val vtxosScripts =
                vtxos
                    .map { vtxo ->
                        vtxo.script
                    }.toHashSet()
                    .toTypedArray()

            val contracts =
                contractsRepo.getAll(
                    walletIds,
                    vtxosScripts,
                )

            val spendableCoins =
                intent.vtxos.map { outpoint ->
                    val vtxo =
                        vtxos.find { vtxo ->
                            vtxo.outpoint == outpoint
                        }

                    if (vtxo == null) {
                        Log.error(LOG_TAG, "VTXO $outpoint not found in storage for intent $intentId")
                        throw IllegalArgumentException("VTXO $outpoint not found in storage for intent $intentId")
                    }

                    val contract =
                        contracts.find { contract ->
                            contract.getScriptPubKey(serverInfo.network) == vtxo.script
                        }
                    if (contract == null) {
                        Log.error(LOG_TAG, "Contract for VTXO $outpoint not found in storage for intent $intentId")
                        throw IllegalArgumentException("Contract for VTXO $outpoint not found in storage for intent $intentId")
                    }

                    contract.toArkCoin(vtxo)
                }
            val batchSession =
                BatchSession(
                    client,
                    wallet,
                    intent,
                    spendableCoins,
                    event,
                )

            batchSession.init()

            activeBatchSessions[intentId] = batchSession

            val batchIntentIds = hashSetOf<String>()
            Mutex().withLock {
                batchIntentIds.add(intentId)
            }
            batchIdToIntentIds[event.id] = batchIntentIds

            try {
                client.confirmIntentRegistration(intentId)
                wallet.saveIntent(intent)
            } catch (e: Exception) {
                Log.error(LOG_TAG, "Failed to confirm intent registration for intent $intentId: $e")
                throw e
            }
        } catch (e: Exception) {
            Log.error(LOG_TAG, "Failed to setup batch session for intent $intentId: $e")
            throw e
        }
    }

    /**
     * Dispatches a non-startup [event] to the [BatchSession]s registered for its batch, via
     * [BatchEvent.getBatchId] and [batchIdToIntentIds].
     *
     * If no intent ids are known for the event's batch id, the event is logged and dropped.
     *
     * @param event The batch event to forward to its associated session(s).
     */
    private suspend fun handleBatchEvent(event: BatchEvent) {
        val batchId = event.getBatchId()
        val intentIds = batchIdToIntentIds[batchId]
        if (intentIds == null) {
            Log.warning(LOG_TAG, "No intent ids found for batch $batchId")
            return
        }
        for (id in intentIds) {
            val batchSession = activeBatchSessions[id]
            batchSession?.processEvent(event)
        }
    }

    /**
     * Extracts the batch id carried by this event, or `null` for event types not tied to a
     * specific batch (e.g. [BatchEvent.StreamStartedEvent], [BatchEvent.HeartbeatEvent]).
     */
    private fun BatchEvent.getBatchId(): String? =
        when (this) {
            is BatchEvent.BatchFinalizedEvent -> id
            is BatchEvent.BatchFinalizationEvent -> id
            is BatchEvent.BatchFailedEvent -> id
            is BatchEvent.TreeTxEvent -> id
            is BatchEvent.TreeNoncesEvent -> id
            is BatchEvent.TreeSignatureEvent -> id
            is BatchEvent.TreeNoncesAggregatedEvent -> id
            is BatchEvent.TreeSigningStartedEvent -> id
            else -> null
        }

    companion object {
        private const val LOG_TAG = "BatchManagementService"
        private const val EVENT_STREAM_RETRY_DELAY: Long = 5000
    }
}
