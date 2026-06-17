package com.arkade.core.taproot

import com.arkade.core.toXOnlyPubKey
import fr.acinq.bitcoin.OP_1
import fr.acinq.bitcoin.OP_PUSHDATA
import fr.acinq.bitcoin.Script

fun getTaprootScriptPubKey(outputKey: ByteArray): ByteArray {
    val asm = listOf(OP_1, OP_PUSHDATA(outputKey))
    val scriptPubKey = Script.write(asm)
    return scriptPubKey
}

fun taprootDescriptorFromPubKey(pubKey: String): String {
    val xOnlyPubKey = pubKey.toXOnlyPubKey()
    return "tr(${xOnlyPubKey.value.toHex()})"
}

fun pubKeyFromTaprootDescriptor(descriptor: String): String = descriptor.substringAfter("(").substringBefore(")")
