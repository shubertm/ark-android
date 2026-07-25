package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput
import fr.acinq.bitcoin.io.ByteArrayOutput
import fr.acinq.bitcoin.io.readNBytes

class AssetId(
    val txId: ByteArray,
    val groupIndex: Int,
) {
    override fun toString(): String = serialize().toHexString().lowercase()

    fun serialize(): ByteArray {
        val output = ByteArrayOutput()
        serializeTo(output)
        return output.toByteArray()
    }

    fun serializeTo(output: ByteArrayOutput) {
        output.write(txId)
        output.writeUInt16LE(groupIndex)
    }

    companion object {
        fun fromBytesInput(input: ByteArrayInput): AssetId {
            require(input.availableBytes >= ASSET_ID_SIZE) { "Invalid asset id length: got ${input.availableBytes} bytes" }
            val txId = input.readNBytes(TX_HASH_SIZE)
            val index = input.readUInt16LE()
            requireNotNull(txId) { "Missing txId" }
            return AssetId(txId, index)
        }
    }
}
