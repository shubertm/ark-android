package com.arkade.core.assets

import fr.acinq.bitcoin.io.ByteArrayInput

class AssetMetadata(
    val key: ByteArray,
    val value: ByteArray,
) {
    fun validate() {
        require(key.isNotEmpty()) { "Missing metadata key" }
        require(value.isNotEmpty()) { "Missing metadata value" }
    }

    companion object {
        fun fromBytesInput(input: ByteArrayInput): AssetMetadata {
            val key =
                runCatching {
                    requireNotNull(input.readVarBytes())
                }.getOrElse { throw IllegalArgumentException("Invalid asset metadata length") }
            val value =
                runCatching {
                    requireNotNull(input.readVarBytes())
                }.getOrElse { throw IllegalArgumentException("Invalid asset metadata length") }

            val metadata = AssetMetadata(key, value)
            metadata.validate()
            return metadata
        }
    }
}
