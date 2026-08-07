package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayOutput

fun ByteArrayOutput.writeUInt16LE(value: Int) {
    require(value in 0..0xFFFF) { "Value out of uint16 range: $value" }
    write(value.toByte().toInt())
    write((value shr 8).toByte().toInt())
}
