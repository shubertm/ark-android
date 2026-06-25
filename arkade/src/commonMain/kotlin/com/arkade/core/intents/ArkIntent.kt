package com.arkade.core.intents

import ark.v1.Intent
import com.arkade.core.checkSha256Hash
import fr.acinq.bitcoin.OutPoint

/**
 * Intent used to register an intent with the Ark server
 */
data class ArkIntent(
    val txId: String,
    val id: String?,
    val walletId: String,
    val state: IntentState,
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
    val vtxos: List<OutPoint>,
    val signerDescriptor: String?,
) {
    init {
        require(checkSha256Hash(txId)) { "Invalid TxId" }
        if (commitmentTxId != null) require(checkSha256Hash(commitmentTxId)) { "Invalid commitment TxId" }
    }

    internal fun toIntent(): Intent = Intent(registerProof, registerProofMessage)
}
