package com.arkade.core.services

import com.arkade.core.ArkServerInfo
import com.arkade.core.batches.BatchEvent
import com.arkade.core.batches.BatchSession
import com.arkade.core.intents.ArkIntent
import com.arkade.core.wallet.Wallet
import com.arkade.network.ArkadeClient
import com.arkade.repositories.contracts.ContractRepo
import com.arkade.utils.Log
import com.arkade.utils.error
import com.arkade.utils.info
import com.arkade.utils.warning
import fr.acinq.bitcoin.Crypto.sha256
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class BatchManagementService(
    private val client: ArkadeClient,
    private val wallet: Wallet,
    private val contractsRepo: ContractRepo,
) {
    private var streamId: String? = null
    private val activeIntents: MutableMap<String, ArkIntent> = mutableMapOf()
    private val activeBatchSessions: MutableMap<String, BatchSession> = mutableMapOf()
    private val batchIdToIntentIds = mutableMapOf<String, HashSet<String>>()

    suspend fun start() {
        client
            .getBatchEventStream()
            .catch { exception ->
                Log.error(LOG_TAG, "Errors in batch stream: $exception")
            }.collect { event ->
                processEvent(event)
            }
    }

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

    private suspend fun handleBatchStartedForAllIntents(event: BatchEvent.BatchStartedEvent) {
        val intentHashMap =
            activeIntents.mapKeys { entry ->
                sha256(entry.key.encodeToByteArray()).toHexString()
            }

        val selectedIntents =
            try {
                event.intentIdHashes.map { intentHash ->
                    intentHashMap.getValue(intentHash)
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
                    serverInfo,
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
    }
}
