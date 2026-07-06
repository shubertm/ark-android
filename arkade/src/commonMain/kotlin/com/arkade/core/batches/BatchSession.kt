package com.arkade.core.batches

import com.arkade.core.ArkServerInfo
import com.arkade.core.coins.ArkCoin
import com.arkade.core.csvSigScript
import com.arkade.core.intents.ArkIntent
import com.arkade.core.wallet.Wallet
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.getOrElse

class BatchSession(
    private val arkServerInfo: ArkServerInfo,
    private val wallet: Wallet,
    private val intent: ArkIntent,
    private val inputs: List<ArkCoin>,
    private val batchStartedEvent: BatchEvent.BatchStartedEvent,
) : BatchEventHandler {
    private val batchId = batchStartedEvent.id
    private lateinit var sweepTapScript: ByteArray
    private val connectors: MutableList<TxTreeNode> = mutableListOf()

    fun init() {
        val serverInfo = arkServerInfo
        sweepTapScript = csvSigScript(batchStartedEvent.batchExpiry.inWholeSeconds, serverInfo.forfeitPubKey)
    }

    suspend fun processEvent(event: BatchEvent): Boolean {
        try {
            when (event) {
                is BatchEvent.StreamStartedEvent -> {}

                is BatchEvent.BatchStartedEvent -> {}

                is BatchEvent.BatchFinalizedEvent -> {
                    if (event.id != batchId) {
                        return true
                    }
                }

                is BatchEvent.BatchFinalizationEvent -> {
                    onBatchFinalization(event, connectors)
                }

                is BatchEvent.BatchFailedEvent -> {
                    if (event.id == batchId) {
                        throw UnsupportedOperationException("Batch failed: ${event.reason}")
                    }
                }

                is BatchEvent.TreeSigningStartedEvent -> {
                    onTreeSigningStarted()
                }

                is BatchEvent.TreeNoncesAggregatedEvent -> {
                    onTreeNoncesAggregated()
                }

                is BatchEvent.TreeTxEvent -> {
                    onTreeTx()
                }

                is BatchEvent.TreeSignatureEvent -> {
                    onTreeSignature()
                }

                is BatchEvent.TreeNoncesEvent -> {
                    onTreeNonces()
                }

                is BatchEvent.HeartbeatEvent -> {}
            }
            return false
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun onBatchFinalization(
        event: BatchEvent.BatchFinalizationEvent,
        connectors: List<TxTreeNode>,
    ) {
        var connectorsGraph: TxTree? = null
        if (connectors.isNotEmpty()) {
            connectorsGraph = TxTree.create(connectors)
            val commitmentPSBT =
                Psbt.read(event.commitmentTx.encodeToByteArray()).getOrElse {
                    throw IllegalStateException("Failed to read commitment tx")
                }

            TreeValidator.validateConnectorsTxGraph(commitmentPSBT, connectorsGraph)
        }

        val signedForfeits: MutableList<String> = mutableListOf()

        val connectorsLeaves = connectorsGraph?.leaves()?.toList() ?: listOf()
        var connectorIndex = 0

        for (vtxoCoin in inputs) {
            if (vtxoCoin.requiresForfeit()) {
                continue
            }

            require(connectorsLeaves.isNotEmpty()) { "Connectors not received from operator" }

            require(connectorIndex < connectorsLeaves.size) {
                "Not enough connectors received. Need at least ${connectorIndex + 1}, got ${connectorsLeaves.size}"
            }

            val connectorLeaf = connectorsLeaves[connectorIndex]
            val connectorOutput =
                connectorLeaf.global.tx.txOut
                    .firstOrNull()
            if (connectorOutput != null) {
                throw IllegalStateException("Connector leaf at index $connectorIndex has no outputs")
            }

            val connectorTxId = connectorLeaf.global.tx.txid

            connectorIndex++
        }
    }

    override suspend fun onBatchFailed() {
        TODO("Not yet implemented")
    }

    override suspend fun onTreeSigningStarted() {
        TODO("Not yet implemented")
    }

    override suspend fun onTreeNoncesAggregated() {
        TODO("Not yet implemented")
    }

    override suspend fun onTreeTx() {
        TODO("Not yet implemented")
    }

    override suspend fun onTreeSignature() {
        TODO("Not yet implemented")
    }

    override suspend fun onTreeNonces() {
        TODO("Not yet implemented")
    }
}
