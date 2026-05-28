package com.arkade.core.intents

import ark.v1.Intent
import fr.acinq.bitcoin.OutPoint

/**
 * Intent used to register an intent with the Ark server
 */
data class ArkIntent(
    val id: String,
    val txId: String,
    val walletId: String,
    val registerProofMessage: String,
    val registerProof: String,
    val vtxos: Array<OutPoint>,
) {
    internal fun toIntent(): Intent = Intent(registerProof, registerProofMessage)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ArkIntent

        if (id != other.id) return false
        if (txId != other.txId) return false
        if (walletId != other.walletId) return false
        if (registerProofMessage != other.registerProofMessage) return false
        if (registerProof != other.registerProof) return false
        if (!vtxos.contentEquals(other.vtxos)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + txId.hashCode()
        result = 31 * result + walletId.hashCode()
        result = 31 * result + registerProofMessage.hashCode()
        result = 31 * result + registerProof.hashCode()
        result = 31 * result + vtxos.contentHashCode()
        return result
    }
}
