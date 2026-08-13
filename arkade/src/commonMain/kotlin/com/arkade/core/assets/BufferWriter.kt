package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayOutput

/**
 * Writes [value] as an unsigned 16-bit integer encoded as two little-endian bytes.
 *
 * @param value The value to write; must fit in an unsigned 16-bit integer.
 * @throws IllegalArgumentException if [value] is outside the range `0..0xFFFF`.
 */
fun ByteArrayOutput.writeUInt16LE(value: Int) {
    require(value in 0..0xFFFF) { "Value out of uint16 range: $value" }
    write(value.toByte().toInt())
    write((value shr 8).toByte().toInt())
}
