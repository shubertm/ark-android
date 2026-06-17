package com.arkade.core.contracts

class GenericArkContractParser(
    override val type: String,
    private val parseImpl: (Map<String, String>) -> ArkContract,
) : ArkContractParser {
    override fun parse(data: Map<String, String>): ArkContract = parseImpl(data)
}
