package com.arkade.core.contracts

import com.arkade.core.bitcoin.Network

class GenericArkContractParser(
    override val type: String,
    private val parseImpl: (Map<String, String>, Network) -> ArkContract,
) : ArkContractParser {
    override fun parse(
        data: Map<String, String>,
        network: Network,
    ): ArkContract = parseImpl(data, network)
}
