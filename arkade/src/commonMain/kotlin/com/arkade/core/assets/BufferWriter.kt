package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayOutput

fun ByteArrayOutput.writeUInt16LE(value: Int) {
    write(value.toByte().toInt())
    write((value shr 8).toByte().toInt())
}
