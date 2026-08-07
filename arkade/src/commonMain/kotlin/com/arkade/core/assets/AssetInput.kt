package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput
import fr.acinq.bitcoin.io.readNBytes

class AssetInput(
    val type: Type,
    val vin: Int,
    val amount: Long,
    val txId: ByteArray? = null,
) {
    enum class Type {
        UNSPECIFIED,
        LOCAL,
        INTENT,
        ;

        companion object {
            fun fromByte(value: Byte): Type =
                when (value) {
                    0.toByte() -> LOCAL
                    1.toByte() -> INTENT
                    2.toByte() -> UNSPECIFIED
                    else -> throw IllegalArgumentException("Invalid asset input type: $value")
                }
        }
    }

    companion object {
        fun fromBytesInput(input: ByteArrayInput): AssetInput =
            when (val type = Type.fromByte(input.read().toByte())) {
                Type.LOCAL -> {
                    val vin = input.readUInt16LE()
                    val amount = input.readVarIntToLong()
                    AssetInput(Type.LOCAL, vin, amount)
                }
                Type.INTENT -> {
                    val txId = input.readNBytes(TX_HASH_SIZE)
                    val vin = input.readUInt16LE()
                    val amount = input.readVarIntToLong()
                    AssetInput(Type.INTENT, vin, amount, txId)
                }
                Type.UNSPECIFIED -> throw IllegalArgumentException("Asset input type unspecified")
            }
    }
}
