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
    suspend fun init() {
        val serverInfo = arkServerInfo
        val sweepTapScript = csvSigScript(serverInfo.sessionDuration.inWholeSeconds, serverInfo.forfeitPubKey)
    }

    override fun onBatchStarted() {
        TODO("Not yet implemented")
    }

    override fun onBatchFinalized() {
        TODO("Not yet implemented")
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
