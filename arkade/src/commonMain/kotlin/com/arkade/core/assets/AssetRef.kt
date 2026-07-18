package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput

class AssetRef(
    val type: Type,
    val assetId: AssetId?,
    val groupIndex: Int?,
) {
    enum class Type {
        UNSPECIFIED,
        BY_ID,
        BY_GROUP,
        ;

        companion object {
            fun fromByte(value: Byte): Type =
                when (value) {
                    0.toByte() -> UNSPECIFIED
                    1.toByte() -> BY_ID
                    2.toByte() -> BY_GROUP
                    else -> throw IllegalArgumentException("Unknown asset ref type: $value")
                }
        }
    }

    companion object {
        fun fromBytesInput(input: ByteArrayInput): AssetRef {
            val type = Type.fromByte(input.read().toByte())
            return when (type) {
                Type.BY_ID -> {
                    val assetId = AssetId.fromBytesInput(input)
                    AssetRef(Type.BY_ID, assetId = assetId, null)
                }
                Type.BY_GROUP -> {
                    val groupIndex = input.readUInt16LE()
                    AssetRef(Type.BY_GROUP, null, groupIndex = groupIndex)
                }
                Type.UNSPECIFIED -> throw IllegalArgumentException("Asset ref type unspecified")
            }
        }
    }
}
