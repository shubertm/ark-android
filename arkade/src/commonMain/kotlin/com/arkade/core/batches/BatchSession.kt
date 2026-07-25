package com.arkade.core.batches

import com.arkade.core.ArkServerInfo
import com.arkade.core.ArkTransactionBuilder
import com.arkade.core.coins.ArkCoin
import com.arkade.core.csvSigScript
import com.arkade.core.intents.ArkIntent
import com.arkade.core.wallet.Wallet
import com.arkade.network.ArkadeClient
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.getOrElse
import kotlin.io.encoding.Base64

/**
 * Drives a single batch through its lifecycle by reacting to [BatchEvent]s and implementing
 * [BatchEventHandler].
 *
 * A session is created per batch that [inputs] have been registered for via [intent], and is
 * responsible for finalizing the batch (building/signing forfeit transactions and signing the
 * commitment transaction for boarding inputs) and, eventually, cooperatively signing the
 * resulting VTXO tree.
 *
 * @property client Used to fetch server info and submit signed forfeit/commitment transactions.
 * @property wallet Used to sign forfeit transactions and the commitment transaction.
 * @property intent The intent that registered [inputs] for this batch.
 * @property inputs The coins being spent/registered in this batch.
 * @property batchStartedEvent The event that started this batch, providing [batchId] and expiry.
 */
class BatchSession(
    private val client: ArkadeClient,
    private val wallet: Wallet,
    private val intent: ArkIntent,
    private val inputs: List<ArkCoin>,
    private val batchStartedEvent: BatchEvent.BatchStartedEvent,
) : BatchEventHandler {
    private val batchId = batchStartedEvent.id
    private lateinit var sweepTapScript: ByteArray
    private val connectors: MutableList<TxTreeNode> = mutableListOf()

    private lateinit var serverInfo: ArkServerInfo

    var isComplete = false
        private set

    /**
     * Fetches [serverInfo] from [client] and derives [sweepTapScript] from the batch's expiry
     * and the server's forfeit public key.
     *
     * Must be called before [processEvent] handles a [BatchEvent.BatchFinalizationEvent].
     */
    suspend fun init() {
        serverInfo = client.getInfo()
        sweepTapScript = csvSigScript(batchStartedEvent.batchExpiry.inWholeSeconds, serverInfo.forfeitPubKey)
    }

    /**
     * Dispatches [event] to the corresponding [BatchEventHandler] callback.
     *
     * @param event The batch event to process.
     * @return `true` if the batch has been finalized under a different id than [batchId] and
     * this session should stop processing further events; `false` otherwise.
     * @throws UnsupportedOperationException if the batch fails with id [batchId].
     */
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

                is BatchEvent.BatchFailedEvent -> onBatchFailed(event)

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

    /**
     * Builds and signs the forfeit/commitment transactions required to finalize the batch, then
     * submits them via [client].
     *
     * For each input in [inputs] that [ArkCoin.requiresForfeit], this validates [connectors]
     * (when present) into a [TxTree], pairs the coin with the next available connector leaf,
     * builds a forfeit transaction via [ArkTransactionBuilder.constructForfeitTx], signs it with
     * [wallet], and Base64-encodes it. For boarding inputs (`isUnrolled`), the commitment PSBT's
     * witness data is updated and signed by [wallet] for each corresponding input.
     *
     * If any forfeit transaction was signed or the commitment transaction was signed, both are
     * submitted to the server via [ArkadeClient.submitForfeitTxs].
     *
     * @param event The finalization event carrying the unsigned commitment transaction.
     * @param connectors The connector tree nodes to use for funding forfeit transaction inputs.
     * @throws IllegalStateException if the commitment tx cannot be read, a connector leaf has
     * no outputs, or updating a boarding input's witness data fails.
     * @throws IllegalArgumentException if a forfeit-requiring coin has no connector available.
     */
    override suspend fun onBatchFinalization(
        event: BatchEvent.BatchFinalizationEvent,
        connectors: List<TxTreeNode>,
    ) {
        val commitmentPSBT =
            Psbt
                .read(event.commitmentTx.encodeToByteArray())
                .getOrElse { throw IllegalStateException("Failed to read commitment tx") }
        var connectorsGraph: TxTree? = null
        if (connectors.isNotEmpty()) {
            connectorsGraph = TxTree.create(connectors)
            TreeValidator.validateConnectorsTxGraph(commitmentPSBT, connectorsGraph)
        }

        val signedForfeitTxs: MutableList<String> = mutableListOf()

        val connectorsLeaves = connectorsGraph?.leaves()?.toList() ?: listOf()
        var connectorIndex = 0

        for (vtxoCoin in inputs) {
            if (!vtxoCoin.requiresForfeit()) {
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
            if (connectorOutput == null) {
                throw IllegalStateException("Connector leaf at index $connectorIndex has no outputs")
            }

            val connectorTxId = connectorLeaf.global.tx.txid

            connectorIndex++

            val forfeitTx =
                ArkTransactionBuilder.constructForfeitTx(
                    coin = vtxoCoin,
                    connector = connectorOutput,
                    connectorTxId = connectorTxId,
                    forfeitDestination = serverInfo.forfeitAddress,
                )

            val signedForfeitTx = wallet.sign(vtxoCoin.signerDescriptor, forfeitTx, arrayOf(0))
            val signedForfeitTxBytes = Transaction.write(signedForfeitTx)
            signedForfeitTxs.add(Base64.encode(signedForfeitTxBytes))
        }

        var signedCommitmentPSBT: Psbt? = null
        val boardingCoins = inputs.filter { it.isUnrolled }
        if (boardingCoins.isNotEmpty()) {
            signedCommitmentPSBT = commitmentPSBT
            for (boardingCoin in boardingCoins) {
                val outpoint = boardingCoin.outpoint
                val boardingInput = commitmentPSBT.getInput(outpoint)
                requireNotNull(boardingInput) { "Boarding input $outpoint not found in commitment tx" }

                signedCommitmentPSBT =
                    signedCommitmentPSBT
                        ?.updateWitnessInput(
                            outpoint,
                            boardingCoin.txOut,
                        )?.getOrElse { throw IllegalStateException("Failed to update boarding input witness") }

                signedCommitmentPSBT =
                    Psbt(wallet.sign(boardingCoin.signerDescriptor, signedCommitmentPSBT!!, arrayOf(outpoint)))
            }
        }

        val signedCommitmentTx = signedCommitmentPSBT?.global?.tx

        if (signedForfeitTxs.isNotEmpty() || signedCommitmentTx != null) {
            val signedCommitmentTxBytes = Transaction.write(signedCommitmentTx!!)
            val signedCommitmentTxBase64 = Base64.encode(signedCommitmentTxBytes)
            client.submitForfeitTxs(signedForfeitTxs, signedCommitmentTxBase64)
        }
    }

    override suspend fun onBatchFailed(event: BatchEvent.BatchFailedEvent) {
        if (event.id == batchId) {
            isComplete = true
            throw UnsupportedOperationException("Batch failed: ${event.reason}")
        }
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
