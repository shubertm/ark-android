package com.arkade.core.batches

import com.arkade.core.bitcoin.Network
import com.arkade.core.csvSigScript
import com.arkade.core.intents.ArkIntent
import com.arkade.core.vtxos.Vtxo
import com.arkade.core.wallet.Wallet
import com.arkade.network.ArkadeClient

class BatchSession(
    private val client: ArkadeClient,
    private val wallet: Wallet,
    private val network: Network,
    private val intent: ArkIntent,
    private val inputs: List<Vtxo>,
    private val batchStartedEvent: BatchEvent.BatchStartedEvent,
) : BatchEventHandler {
    suspend fun init() {
        val serverInfo = client.getInfo()
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
