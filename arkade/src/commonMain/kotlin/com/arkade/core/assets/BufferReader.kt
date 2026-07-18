package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput
import fr.acinq.bitcoin.io.readNBytes

fun ByteArrayInput.readVarInt(): ULong {
    var result = 0UL
    var shift = 0
    var byte: Int
    do {
        byte = read()
        require(byte != -1) { "Unexpected end of input" }
        result = result or (((byte and 0x7F).toULong() shl shift))
        shift += 7
    } while ((byte and 0x80) != 0)

    return result
}

fun ByteArrayInput.readVarBytes(): ByteArray? {
    val size = readVarInt().toInt()
    return readNBytes(size)
}

fun ByteArrayInput.readUInt16LE(): Int {
    val data = requireNotNull(readNBytes(2))
    val low = data[0].toInt() and 0xFF
    val high = data[1].toInt() and 0xFF
    return low or (high shl 8)
}
