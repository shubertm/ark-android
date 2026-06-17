package com.arkade.core.taproot

import com.arkade.core.toXOnlyPubKey
import fr.acinq.bitcoin.OP_1
import fr.acinq.bitcoin.OP_PUSHDATA
import fr.acinq.bitcoin.Script

/**
 * Constructs a P2TR (Pay-to-Taproot) `scriptPubKey` from a Taproot output key.
 *
 * The resulting script is `OP_1 <outputKey>`, which is the standard witness version 1
 * script used for Taproot outputs (BIP-341).
 *
 * @param outputKey the 32-byte x-only Taproot output public key.
 * @return the serialized P2TR scriptPubKey as a byte array.
 */
fun getTaprootScriptPubKey(outputKey: ByteArray): ByteArray {
    val asm = listOf(OP_1, OP_PUSHDATA(outputKey))
    val scriptPubKey = Script.write(asm)
    return scriptPubKey
}

/**
 * Creates a Taproot descriptor string from a compressed or x-only public key hex string.
 *
 * Converts the input [string] to its x-only representation and wraps it in the `tr(…)` descriptor
 * format, e.g. `tr(a19310a999207dbd9a03d20f649e37c7a578a07d75e6fa19aa3f33fc6b15622c)`.
 *
 * @param string a hex-encoded compressed (33-byte) or x-only (32-byte) public key.
 * @return a Taproot descriptor string `tr(<xOnlyPubKeyHex>)`.
 */
fun parseTaprootDescriptor(string: String): String {
    if (string.startsWith("tr(") && string.endsWith(")")) {
        return "tr(${string.removeSurrounding("tr(", ")").toXOnlyPubKey().value.toHex()})"
    }
    val xOnlyPubKey = string.toXOnlyPubKey()
    return "tr(${xOnlyPubKey.value.toHex()})"
}

/**
 * Extracts the x-only public key hex string from a Taproot descriptor.
 *
 * Parses the inner value from a descriptor of the form `tr(<pubKeyHex>)` by extracting
 * the substring between the first `(` and the last `)`.
 *
 * @param descriptor a Taproot descriptor string (e.g. `tr(<xOnlyPubKeyHex>)`).
 * @return the hex-encoded x-only public key contained in the descriptor.
 */
fun pubKeyFromTaprootDescriptor(descriptor: String): String {
    val pubKey =
        descriptor
            .substringAfter("(")
            .substringBefore(")")
            .toXOnlyPubKey()
    return pubKey.value.toHex()
}
