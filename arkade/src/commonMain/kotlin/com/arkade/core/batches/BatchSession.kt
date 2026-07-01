package com.arkade.core.batches

import com.arkade.core.ArkServerInfo
import com.arkade.core.coins.ArkCoin
import com.arkade.core.csvSigScript
import com.arkade.core.intents.ArkIntent
import com.arkade.core.wallet.Wallet

class BatchSession(
    private val arkServerInfo: ArkServerInfo,
    private val wallet: Wallet,
    private val intent: ArkIntent,
    private val inputs: List<ArkCoin>,
    private val batchStartedEvent: BatchEvent.BatchStartedEvent,
) : BatchEventHandler {
    private val batchId = batchStartedEvent.id
    private lateinit var sweepTapScript: ByteArray

    fun init() {
        val serverInfo = arkServerInfo
        sweepTapScript = csvSigScript(batchStartedEvent.batchExpiry.inWholeSeconds, serverInfo.forfeitPubKey)
    }

    fun processEvent(event: BatchEvent): Boolean {
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
                    onBatchFinalization()
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

    override fun onBatchFinalization() {
        TODO("Not yet implemented")
    }

    override fun onBatchFailed() {
        TODO("Not yet implemented")
    }

    override fun onTreeSigningStarted() {
        TODO("Not yet implemented")
    }

    override fun onTreeNoncesAggregated() {
        TODO("Not yet implemented")
    }

    override fun onTreeTx() {
        TODO("Not yet implemented")
    }

    override fun onTreeSignature() {
        TODO("Not yet implemented")
    }

    override fun onTreeNonces() {
        TODO("Not yet implemented")
    }
}
