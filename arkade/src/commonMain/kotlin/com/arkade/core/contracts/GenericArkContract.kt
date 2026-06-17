package com.arkade.core.contracts

class GenericArkContract(
    serverDescriptor: String,
    private val tapLeafScripts: List<ByteArray>,
    private val additionalData: Map<String, String>? = null,
) : ArkContract(serverDescriptor) {
    override val type: String = "generic"

    override fun getTapLeafScripts(): List<ByteArray> = tapLeafScripts

    override fun getAdditionalData(): Map<String, String> = additionalData ?: emptyMap()
}
