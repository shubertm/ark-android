package com.arkade.core.contracts

import com.arkade.core.ArkServerInfo

class GenericArkContract(
    private val serverInfo: ArkServerInfo,
    private val tapLeafScripts: List<ByteArray>,
    private val additionalData: Map<String, String>? = null,
) : ArkContract(serverInfo) {
    override val type: String = "generic"

    override fun getTapLeafScripts(): List<ByteArray> = tapLeafScripts

    override fun getAdditionalData(): Map<String, String> = additionalData ?: emptyMap()
}
