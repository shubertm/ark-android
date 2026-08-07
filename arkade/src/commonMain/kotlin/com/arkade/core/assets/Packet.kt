package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput

class Packet(
    val groups: List<AssetGroup>,
) : ExtensionPacket {
    override val type: Byte = PACKET_TYPE

    fun validate() {
        require(groups.isNotEmpty()) { "Missing assets" }

        val seenAssetIds: HashSet<String> = hashSetOf()

        groups.forEach { group ->
            val assetId = group.assetId
            if (assetId != null) {
                val isNotSeen = seenAssetIds.add(group.assetId.toString())
                require(isNotSeen) { "Duplicate asset group for asset: ${group.assetId}" }
            }

            val controlAsset = group.controlAsset

            if (controlAsset != null && controlAsset.type == AssetRef.Type.BY_GROUP) {
                val groupIndex = requireNotNull(controlAsset.groupIndex) { "Missing group control asset index" }
                if (groupIndex >= groups.size) {
                    throw IllegalArgumentException(
                        "Invalid control asset group index: ${controlAsset.groupIndex} out of range [0, ${groups.size - 1}]",
                    )
                }
            }
        }
    }

    override fun serializePacketData(): ByteArray {
        TODO("Not yet implemented")
    }

    companion object {
        const val PACKET_TYPE: Byte = 0x00

        fun fromBytes(bytes: ByteArray): Packet {
            val input = ByteArrayInput(bytes)
            return fromBytesInput(input)
        }

        private fun fromBytesInput(input: ByteArrayInput): Packet {
            require(input.availableBytes > 0) { "Missing packet bytes" }

            val count = input.readVarInt().toInt()
            val groups: MutableList<AssetGroup> = mutableListOf()
            for (i in 0 until count) {
                groups.add(AssetGroup.fromBytesInput(input))
            }

            require(input.availableBytes == 0) { "Invalid packet length, left ${input.availableBytes} bytes" }

            val packet = Packet(groups)
            packet.validate()
            return packet
        }
    }
}
