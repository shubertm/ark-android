package com.arkade.core.contracts

interface ArkContractParser {
    val type: String

    fun parse(data: Map<String, String>): ArkContract

    companion object {
        fun getAdditionalData(contract: String): Map<String, String> {
            val parts = contract.split("&")
            val data =
                parts.associate {
                    val entry = it.split("=")
                    require(entry.size == 2) { "Invalid additional data format" }
                    entry[0] to entry[1]
                }
            return data
        }
    }
}
