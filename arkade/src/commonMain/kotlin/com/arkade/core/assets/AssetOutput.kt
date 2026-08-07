package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput

class AssetOutput(
    val vout: Int,
    val amount: Long,
) {
    private val type = Type.LOCAL

    fun validate() {
        require(vout >= 0) { "invalid vout: $vout" }
        require(amount > 0) { "asset output amount must be greater than 0" }
    }

    private enum class Type {
        UNSPECIFIED,
        LOCAL,
        ;

        fun toByte(): Byte = this.ordinal.toByte()
    }

    companion object {
        fun fromBytesInput(input: ByteArrayInput): AssetOutput {
            val type = input.read().toByte()
            require(type != Type.UNSPECIFIED.toByte()) { "output type unspecified" }
            require(type == Type.LOCAL.toByte()) { "invalid asset output type: $type" }

            val vout = input.readUInt16LE()
            val amount = input.readVarIntToLong()
            val output = AssetOutput(vout, amount)
            output.validate()
            return output
        }
    }
}
