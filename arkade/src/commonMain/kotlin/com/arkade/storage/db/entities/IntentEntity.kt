package com.arkade.storage.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arkade.core.intents.ArkIntent
import com.arkade.core.intents.IntentState
import com.arkade.core.intents.IntentVtxo
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.TxId
import kotlin.time.Clock.System.now

@Entity(tableName = "intents")
data class IntentEntity(
    @PrimaryKey
    val intentTxId: String,
    val intentId: String?,
    val walletId: String,
    val state: IntentState,
    val vtxos: List<IntentVtxo> = emptyList(),
    val validFrom: Long?,
    val validUntil: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val registerProof: String,
    val registerProofMessage: String,
    val deleteProof: String,
    val deleteProofMessage: String,
    val batchId: String?,
    val commitmentTxId: String?,
    val cancellationReason: String?,
    val signerDescriptor: String?,
) {
    fun toIntent(): ArkIntent =
        ArkIntent(
            txId = intentTxId,
            id = intentId,
            walletId = walletId,
            state = state,
            validFrom = validFrom,
            validUntil = validUntil,
            createdAt = createdAt,
            updatedAt = updatedAt,
            registerProof = registerProof,
            registerProofMessage = registerProofMessage,
            deleteProof = deleteProof,
            deleteProofMessage = deleteProofMessage,
            batchId = batchId,
            commitmentTxId = commitmentTxId,
            cancellationReason = cancellationReason,
            vtxos = vtxos.map { OutPoint(TxId(it.vtxoTxId), it.vtxoTxOutIndex) },
            signerDescriptor = signerDescriptor,
        )

    companion object {
        fun fromIntent(intent: ArkIntent): IntentEntity =
            IntentEntity(
                intentTxId = intent.txId,
                intentId = intent.id,
                walletId = intent.walletId,
                state = intent.state,
                vtxos = intent.vtxos.map { IntentVtxo(intent.txId, it.txid.toString(), it.index, now().epochSeconds) },
                validFrom = intent.validFrom,
                validUntil = intent.validUntil,
                createdAt = intent.createdAt,
                updatedAt = intent.updatedAt,
                registerProof = intent.registerProof,
                registerProofMessage = intent.registerProofMessage,
                deleteProof = intent.deleteProof,
                deleteProofMessage = intent.deleteProofMessage,
                batchId = intent.batchId,
                commitmentTxId = intent.commitmentTxId,
                cancellationReason = intent.cancellationReason,
                signerDescriptor = intent.signerDescriptor,
            )
    }
}
