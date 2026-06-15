package com.arkade.core.contracts

import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContractParser.Companion.getAdditionalData

class ArkContractParserImpl {
    private val parsers = mutableListOf<ArkContractParser>()

    init {
        // Register custom contract parsers here
    }

    fun parse(
        contractData: Map<String, String>,
        type: String,
        network: Network,
    ): ArkContract {
        val parser =
            parsers.find { it.type == type }
                ?: throw IllegalArgumentException("Unknown contract type: $type")
        return parser.parse(contractData, network)
    }

    fun parse(
        contract: String,
        network: Network,
    ): ArkContract {
        require(contract.startsWith(ARK_CONTRACT)) {
            "Invalid contract format: $contract. Must start with '$ARK_CONTRACT'"
        }
        val data = getAdditionalData(contract)
        val type = data[ARK_CONTRACT]
        requireNotNull(type) { "Contract type not found in additional data" }
        return parse(data, type, network)
    }

    fun registerParser(parser: ArkContractParser) {
        parsers.add(parser)
    }

    companion object {
        private const val ARK_CONTRACT = "arkcontract="
    }
}
