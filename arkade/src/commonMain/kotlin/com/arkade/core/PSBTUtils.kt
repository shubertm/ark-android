package com.arkade.core

import com.arkade.core.batches.CosignerPublicKeyData
import fr.acinq.bitcoin.PublicKey
import fr.acinq.bitcoin.psbt.Input

private const val PSBT_IN_TAPSCRIPT_SIG: Byte = 0x14
private const val PSBT_IN_TAPLEAF_SCRIPT: Byte = 0x15
private const val VTXO_TAPROOT_TREE = "taptree"
private const val VTXO_TREE_EXPIRY = "expiry"
private const val COSIGNER = "cosigner"
private const val CONDITION_WITNESS = "condition"
private const val ARK_PSBT_FIELD_KEY_TYPE: Byte = 222.toByte()

/**
 * Extracts Ark's proprietary "cosigner" public keys stored in this [Input]'s unknown fields.
 *
 * Ark VTXO tree PSBTs carry the public keys of the cosigners that co-signed a node's
 * script using proprietary PSBT input fields keyed by [ARK_PSBT_FIELD_KEY_TYPE] and the
 * [COSIGNER] identifier, followed by a one-byte cosigner index. This scans [Input.unknown]
 * for such entries and decodes each matching value as a [PublicKey].
 *
 * @return The list of [CosignerPublicKeyData] found, in no particular order.
 */
fun Input.getArkFieldsCosigners(): List<CosignerPublicKeyData> {
    val cosignerBytes = COSIGNER.encodeToByteArray()
    val cosignerPrefix = byteArrayOf(ARK_PSBT_FIELD_KEY_TYPE) + cosignerBytes

    fun ByteArray.startsWith(prefix: ByteArray): Boolean {
        if (this.size < prefix.size) return false
        return this.take(prefix.size).toByteArray().contentEquals(prefix)
    }

    return this.unknown
        .filter { (key, _) -> key.toByteArray().startsWith(cosignerPrefix) }
        .map { (key, value) ->
            val index = key.toByteArray().last()
            CosignerPublicKeyData(index, PublicKey(value))
        }
}
