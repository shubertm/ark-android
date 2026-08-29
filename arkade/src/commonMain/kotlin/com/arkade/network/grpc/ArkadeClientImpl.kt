package com.arkade.network.grpc

import ark.v1.ConfirmRegistrationRequest
import ark.v1.DeleteIntentRequest
import ark.v1.FinalizeTxRequest
import ark.v1.GetEventStreamRequest
import ark.v1.GetEventStreamResponse
import ark.v1.GetInfoRequest
import ark.v1.GetPendingTxRequest
import ark.v1.GetTransactionsStreamRequest
import ark.v1.GetTransactionsStreamResponse
import ark.v1.GrpcArkServiceClient
import ark.v1.ModifyTopics
import ark.v1.RegisterIntentRequest
import ark.v1.SubmitSignedForfeitTxsRequest
import ark.v1.SubmitTreeNoncesRequest
import ark.v1.SubmitTreeSignaturesRequest
import ark.v1.TxNotification
import ark.v1.UpdateStreamTopicsRequest
import com.arkade.core.ArkServerInfo
import com.arkade.core.Error
import com.arkade.core.LockedVTXOException
import com.arkade.core.SpentVTXOException
import com.arkade.core.assets.Asset
import com.arkade.core.batches.BatchEvent
import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Network
import com.arkade.core.fees.FeeInfo
import com.arkade.core.intents.ArkIntent
import com.arkade.core.toBlockHeight
import com.arkade.core.txs.ArkTransaction
import com.arkade.core.txs.Notification
import com.arkade.core.txs.Transaction
import com.arkade.core.txs.TxEvent
import com.arkade.core.vtxos.Vtxo
import com.arkade.network.ArkadeClient
import com.arkade.network.Config
import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.squareup.wire.GrpcClient
import com.squareup.wire.bidirectionalStream
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.TxHash
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of [ArkadeClient] using gRPC
 * @param netConfig is the network configuration
 */
class ArkadeClientImpl(
    netConfig: Config,
) : ArkadeClient {
    private val grpcClient = gRPCClient(netConfig.arkadeUrl)

    private val arkadeServiceClient = GrpcArkServiceClient(grpcClient)

    override suspend fun getInfo(): ArkServerInfo {
        val infoResponse = arkadeServiceClient.GetInfo().execute(GetInfoRequest())

        return ArkServerInfo(
            infoResponse.version,
            PublicKey.fromHex(infoResponse.signer_pubkey).xOnly(),
            PublicKey.fromHex(infoResponse.forfeit_pubkey).xOnly(),
            Address.decode(infoResponse.forfeit_address),
            infoResponse.checkpoint_tapscript,
            Network.fromString(infoResponse.network),
            infoResponse.session_duration.seconds,
            infoResponse.unilateral_exit_delay.seconds,
            infoResponse.boarding_exit_delay.seconds,
            infoResponse.utxo_min_amount,
            infoResponse.utxo_max_amount,
            infoResponse.vtxo_min_amount,
            infoResponse.vtxo_max_amount,
            infoResponse.dust,
            FeeInfo.fromProtBuf(infoResponse.fees),
            infoResponse.scheduled_session,
            infoResponse.deprecated_signers,
            infoResponse.service_status,
            infoResponse.digest,
            infoResponse.max_tx_weight,
            infoResponse.max_op_return_outputs,
        )
    }

    override suspend fun registerIntent(intent: ArkIntent): String {
        try {
            val intentRequest = RegisterIntentRequest(intent.toIntent())
            val intentResponse =
                arkadeServiceClient
                    .RegisterIntent()
                    .execute(intentRequest)
            return intentResponse.intent_id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message
            val error = Error.fromMessage(msg)
            when (error) {
                (Error.DuplicatedInput) -> {
                    throw LockedVTXOException("VTXO is already locked by another intent", e)
                }
                (Error.SpentVtxo) -> {
                    throw SpentVTXOException("VTXO input was already spent in a batch: $msg", e)
                }
                else -> throw e
            }
        }
    }

    override suspend fun confirmIntentRegistration(intentId: String) {
        arkadeServiceClient.ConfirmRegistration().execute(
            ConfirmRegistrationRequest(intentId),
        )
    }

    override suspend fun deleteIntent(intent: ArkIntent) {
        arkadeServiceClient.DeleteIntent().execute(
            DeleteIntentRequest(intent.toIntent()),
        )
    }

    override suspend fun submitTransaction(
        signedArkTx: String,
        checkpointTxs: List<String>,
    ): ArkTransaction {
        val txRequest = ark.v1.SubmitTxRequest(signedArkTx, checkpointTxs)
        val txResponse = arkadeServiceClient.SubmitTx().execute(txRequest)
        return ArkTransaction.FullySigned(txResponse.ark_txid, txResponse.final_ark_tx, txResponse.signed_checkpoint_txs)
    }

    override suspend fun finalizeTransaction(
        arkTxid: String,
        finalCheckpointTxs: List<String>,
    ) {
        val request = FinalizeTxRequest(arkTxid, finalCheckpointTxs)
        arkadeServiceClient.FinalizeTx().execute(request)
    }

    override suspend fun submitTreeNonces(
        batchId: String,
        pubkey: String,
        treeNonces: Map<String, String>,
    ) {
        val request = SubmitTreeNoncesRequest(batchId, pubkey, treeNonces)
        arkadeServiceClient.SubmitTreeNonces().execute(request)
    }

    override suspend fun submitTreeSignatures(
        batchId: String,
        pubkey: String,
        treeSignatures: Map<String, String>,
    ) {
        val request = SubmitTreeSignaturesRequest(batchId, pubkey, treeSignatures)
        arkadeServiceClient.SubmitTreeSignatures().execute(request)
    }

    override suspend fun submitForfeitTxs(
        signedForfeitTxs: List<String>,
        signedCommitmentTx: String?,
    ) {
        var request = SubmitSignedForfeitTxsRequest(signedForfeitTxs)
        if (signedCommitmentTx != null) {
            request = request.copy(signed_commitment_tx = signedCommitmentTx)
        }
        arkadeServiceClient.SubmitSignedForfeitTxs().execute(request)
    }

    override fun getBatchEventStream(): Flow<BatchEvent> {
        val request = GetEventStreamRequest() // Supply topics
        return channelFlow {
            val receiveChannel =
                arkadeServiceClient.GetEventStream().bidirectionalStream(
                    this,
                ) { sendChannel, _ ->
                    sendChannel.send(request)
                }
            receiveChannel.consumeEach { response ->
                val batchEvent = response.getBatchEvent() ?: return@consumeEach
                send(batchEvent)
            }
        }
    }

    override fun getTransactionsStream(): Flow<TxEvent> {
        val request = GetTransactionsStreamRequest()
        return channelFlow {
            val receiveChannel =
                arkadeServiceClient.GetTransactionsStream().bidirectionalStream(this) { sendChannel, _ ->
                    sendChannel.send(request)
                }
            receiveChannel.consumeEach { response ->
                val txEvent = response.getTxEvent() ?: return@consumeEach
                send(txEvent)
            }
        }
    }

    override suspend fun getPendingTxs(intent: ArkIntent): List<ArkTransaction> {
        val request = GetPendingTxRequest(intent.toIntent())
        val response = arkadeServiceClient.GetPendingTx().execute(request)
        return response.pending_txs.map { pendingTx ->
            ArkTransaction.Pending(
                pendingTx.ark_txid,
                pendingTx.final_ark_tx,
                pendingTx.signed_checkpoint_txs,
            )
        }
    }

    override suspend fun updateStreamTopics(
        streamId: String,
        addTopics: List<String>,
        removeTopics: List<String>,
    ) {
        val modifyTopics = ModifyTopics(addTopics, removeTopics)
        val request = UpdateStreamTopicsRequest(streamId, modifyTopics)
        arkadeServiceClient.UpdateStreamTopics().execute(request)
    }
}

/**
 * Translates an event stream response to a [BatchEvent]
 * @return a [BatchEvent] or null if the response is not a batch event
 */
internal fun GetEventStreamResponse.getBatchEvent(): BatchEvent? =
    when {
        batch_started != null ->
            BatchEvent.BatchStartedEvent(
                batch_started.id,
                batch_started.batch_expiry.seconds,
                batch_started.intent_id_hashes,
            )
        batch_finalization != null ->
            BatchEvent.BatchFinalizationEvent(
                batch_finalization.id,
                batch_finalization.commitment_tx,
            )
        batch_finalized != null ->
            BatchEvent.BatchFinalizedEvent(
                batch_finalized.id,
                batch_finalized.commitment_txid,
            )
        batch_failed != null ->
            BatchEvent.BatchFailedEvent(
                batch_failed.id,
                batch_failed.reason,
            )
        tree_signing_started != null ->
            BatchEvent.TreeSigningStartedEvent(
                tree_signing_started.id,
                tree_signing_started.cosigners_pubkeys,
                tree_signing_started.unsigned_commitment_tx,
            )
        tree_nonces_aggregated != null ->
            BatchEvent.TreeNoncesAggregatedEvent(
                tree_nonces_aggregated.id,
                tree_nonces_aggregated.tree_nonces,
            )
        tree_tx != null ->
            BatchEvent.TreeTxEvent(
                tree_tx.id,
                tree_tx.batch_index,
                tree_tx.tx,
                tree_tx.txid,
                tree_tx.children,
                tree_tx.topic,
            )
        tree_signature != null ->
            BatchEvent.TreeSignatureEvent(
                tree_signature.id,
                tree_signature.signature,
                tree_signature.batch_index,
                tree_signature.txid,
                tree_signature.topic,
            )
        tree_nonces != null ->
            BatchEvent.TreeNoncesEvent(
                tree_nonces.id,
                tree_nonces.txid,
                tree_nonces.topic,
                tree_nonces.nonces,
            )
        heartbeat != null -> BatchEvent.HeartbeatEvent
        stream_started != null -> BatchEvent.StreamStartedEvent(stream_started.id)
        else -> null
    }

/**
 * Translates a transaction stream response to a [TxEvent]
 * @return a [TxEvent] or null if the response is not a transaction event
 */
internal fun GetTransactionsStreamResponse.getTxEvent(): TxEvent? =
    when {
        commitment_tx != null -> {
            TxEvent.CommitmentEvent(commitment_tx.getNotification())
        }
        ark_tx != null -> {
            TxEvent.ArkEvent(ark_tx.getNotification())
        }
        sweep_tx != null -> {
            TxEvent.SweepEvent(sweep_tx.getNotification())
        }
        heartbeat != null -> {
            TxEvent.HeartbeatEvent
        }
        else -> null
    }

/**
 * Translates a transaction notification to a [Notification]
 * @return a [Notification]
 */
internal fun TxNotification.getNotification(): Notification {
    val checkpointTxs =
        checkpoint_txs.mapValues { checkpointTx ->
            Transaction(checkpointTx.value.txid, checkpointTx.value.tx)
        }
    val spentVtxos =
        spent_vtxos
            .filter { spentVtxo ->
                spentVtxo.outpoint != null && spentVtxo.outpoint.txid.isNotBlank()
            }.map { spentVtxo ->
                spentVtxo.getVtxoData()
            }
    val spendableVtxos =
        spendable_vtxos
            .filter { spendableVtxo ->
                spendableVtxo.outpoint != null && spendableVtxo.outpoint.txid.isNotBlank()
            }.map { spendableVtxo ->
                spendableVtxo.getVtxoData()
            }
    val sweptVtxos =
        swept_vtxos.filter { outpoint -> outpoint.txid.isNotBlank() }.map { outpoint ->
            OutPoint(TxHash(outpoint.txid), outpoint.vout.toLong())
        }

    return Notification(
        txid,
        tx,
        checkpointTxs,
        spentVtxos,
        spendableVtxos,
        sweptVtxos,
    )
}

/**
 * Translates a VTXO from Ark server to [Vtxo.Data]
 * @return [Vtxo.Data]
 */
internal fun ark.v1.Vtxo.getVtxoData(): Vtxo.Data {
    val outpoint = OutPoint(TxHash(outpoint?.txid!!), outpoint.vout.toLong())
    val assets =
        assets.filter { asset -> asset.asset_id.isNotBlank() }.map { asset ->
            Asset(asset.asset_id, BigDecimal.fromLong(asset.amount))
        }
    return Vtxo.Data.normalized(
        outpoint,
        BigDecimal.fromLong(amount),
        script,
        created_at,
        expires_at,
        expires_at.toBlockHeight(),
        is_preconfirmed,
        is_swept,
        is_unrolled,
        is_spent,
        spent_by,
        settled_by,
        ark_txid,
        commitment_txids,
        assets,
    )
}

/**
 * Creates a gRPC client from the provided service url
 * @param baseUrl is the gRPC service url
 * @return a gRPC client
 */
expect fun gRPCClient(baseUrl: String): GrpcClient
