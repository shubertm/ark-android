package com.arkade.core.services

import com.arkade.core.ArkServerInfo
import com.arkade.core.batches.BatchEvent
import com.arkade.core.batches.BatchSession
import com.arkade.core.intents.ArkIntent
import com.arkade.network.ArkadeClient
import com.arkade.repositories.contracts.ContractsRepo
import com.arkade.repositories.wallet.WalletRepo
import com.arkade.utils.Log
import com.arkade.utils.error
import com.arkade.utils.info
import com.arkade.utils.warning
import fr.acinq.bitcoin.Crypto.sha256
import kotlinx.coroutines.flow.catch

class BatchManagementService(
    private val client: ArkadeClient,
    private val walletRepo: WalletRepo,
    private val contractsRepo: ContractsRepo,
) {
    private var streamId: String? = null
    private val activeIntents: MutableMap<String, ArkIntent> = mutableMapOf()
    private val activeBatchSessions: MutableMap<String, BatchSession> = mutableMapOf()

    suspend fun start() {
        client
            .getBatchEventStream()
            .catch { exception ->
                Log.error(LOG_TAG, "Error in batch stream: $exception")
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
            }
        }
    }

    private suspend fun handleBatchStartedForAllIntents(event: BatchEvent.BatchStartedEvent) {
        val intentHashMap =
            activeIntents.keys.associateBy { intentId ->
                sha256(intentId.encodeToByteArray()).toHexString()
            }

        val selectedIntentIds =
            event.intentIdHashes.map { intentHash ->
                intentHashMap.getValue(intentHash)
            }

        if (selectedIntentIds.isEmpty()) return

        val walletIds =
            selectedIntentIds
                .mapNotNull { intentId ->
                    activeIntents.getOrElse(intentId) { null }?.walletId
                }.distinct()
                .toTypedArray()

        if (walletIds.isEmpty()) return

        val serverInfo = client.getInfo()

        selectedIntentIds.forEach { intentId ->
            val intent = activeIntents.getOrElse(intentId) { null }
            if (intent == null || activeBatchSessions.containsKey(intentId)) {
                return@forEach
            }

            try {
                setupBatchSession(intentId, intent, serverInfo, event)
            } catch (e: Exception) {
                Log.warning(LOG_TAG, "Failed to handle batch started event for intent $intentId: $e")
            }
        }
    }

    private suspend fun setupBatchSession(
        intentId: String,
        intent: ArkIntent,
        serverInfo: ArkServerInfo,
        event: BatchEvent.BatchStartedEvent,
    ) {
        try {
            val walletIds = arrayOf(intent.walletId)
            val vtxos =
                walletRepo.getVtxos(
                    outpoints = intent.vtxos,
                    walletIds = walletIds,
                    includeSpent = true,
                )

            val vtxosScripts =
                vtxos
                    .mapNotNull { vtxo ->
                        vtxo.data?.script
                    }.toHashSet()
                    .toTypedArray()

            val contracts =
                contractsRepo.getContracts(
                    walletIds,
                    vtxosScripts,
                )

            val spendableCoins =
                intent.vtxos.map { outpoint ->
                    val vtxo =
                        vtxos.find { vtxo ->
                            vtxo.data?.outpoint == outpoint
                        }

                    if (vtxo == null) {
                        Log.error(LOG_TAG, "VTXO $outpoint not found in storage for intent $intentId")
                        throw IllegalArgumentException("VTXO $outpoint not found in storage for intent $intentId")
                    }

                    val contract =
                        contracts.find { contract ->
                            contract.script == vtxo.data?.script
                        }
                    if (contract == null) {
                        Log.error(LOG_TAG, "Contract for VTXO $outpoint not found in storage for intent $intentId")
                        throw IllegalArgumentException("Contract for VTXO $outpoint not found in storage for intent $intentId")
                    }
                }
        } catch (e: Exception) {
        }
    }

    companion object {
        private const val LOG_TAG = "BatchManagementService"
    }
}
