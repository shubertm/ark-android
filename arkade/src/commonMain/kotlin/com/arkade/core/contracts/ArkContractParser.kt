package com.arkade.core.contracts

import com.arkade.core.bitcoin.Network

interface ArkContractParser {
    val type: String

    fun parse(
        data: Map<String, String>,
        network: Network,
    ): ArkContract

    companion object {
        fun getAdditionalData(contract: String): Map<String, String> {
            val parts = contract.split("&")
            val data =
                parts.associate {
                    val (key, value) = it.split("=")
                    key to value
                }
            return data
        }
    }
}
