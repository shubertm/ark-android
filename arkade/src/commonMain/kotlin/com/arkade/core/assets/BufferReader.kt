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
        if (shift == 63) {
            require(byte == 0x01) { "VarInt is too big, non-canonical or contains unwanted trailing bits" }
            result = result or (byte.toULong() shl 63)
            return result
        }
        result = result or (((byte and 0x7F).toULong() shl shift))
        shift += 7
    } while ((byte and 0x80) != 0)

    return result
}

fun ByteArrayInput.readVarBytes(): ByteArray? {
    val size = readVarInt().toInt()
    require(size in 0..availableBytes) { "Invalid size for var bytes: $size" }
    return readNBytes(size)
}

fun ByteArrayInput.readUInt16LE(): Int {
    val data = requireNotNull(readNBytes(2))
    require(data.size == 2) { "Unexpected end of input" }
    val low = data[0].toInt() and 0xFF
    val high = data[1].toInt() and 0xFF
    return low or (high shl 8)
}
