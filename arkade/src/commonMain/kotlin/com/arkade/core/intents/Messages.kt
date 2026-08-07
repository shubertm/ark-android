package com.arkade.core.intents

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class RegisterIntentMessage(
    val type: String,
    @SerialName("onchain_output_indexes")
    val onChainOutputsIndexes: List<Int>,
    @SerialName("valid_at")
    val validAt: Long,
    @SerialName("expire_at")
    val expiresAt: Long,
    @SerialName("cosigners_public_keys")
    val cosignersPublicKeys: List<String>,
) {
    companion object {
        fun fromString(string: String): RegisterIntentMessage = Json.decodeFromString(string)
    }
}

@Serializable
data class DeleteIntentMessage(
    val type: String,
    @SerialName("expire_at")
    val expiresAt: Long,
) {
    companion object {
        fun fromString(string: String): DeleteIntentMessage = Json.decodeFromString(string)
    }
}
