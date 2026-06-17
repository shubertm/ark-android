package com.arkade.core.contracts

import com.arkade.core.contracts.ArkContractParser.Companion.getAdditionalData

/**
 * Registry and dispatcher for [ArkContractParser] implementations.
 *
 * [ArkContractParserImpl] maintains an ordered list of registered parsers and selects
 * the correct one based on the `arkcontract` type key found in a contract string or data map.
 * The [ArkBoardingContract] parser is registered automatically at construction time.
 *
 * Additional parsers can be registered at runtime via [registerParser]. Parsers are
 * matched by their [ArkContractParser.type] value; the first match wins.
 *
 * **Usage example:**
 * ```kotlin
 * val parser = ArkContractParserImpl()
 * val contract = parser.parse("arkcontract=Boarding&server=tr(...)&user=tr(...)&exit_delay=144")
 * ```
 */
class ArkContractParserImpl {
    private val parsers = mutableListOf<ArkContractParser>()

    init {
        // Register custom contract parsers here
        registerParser(
            GenericArkContractParser(ArkBoardingContract.TYPE, ArkBoardingContract::parse),
        )
    }

    /**
     * Parses an [ArkContract] from a pre-split key/value data map and an explicit [type].
     *
     * @param contractData the key/value map of contract parameters.
     * @param type the contract type string used to select the appropriate parser.
     * @return the [ArkContract] produced by the matching parser.
     * @throws IllegalArgumentException if no parser is registered for [type].
     */
    fun parse(
        contractData: Map<String, String>,
        type: String,
    ): ArkContract {
        val parser =
            parsers.find { it.type == type }
                ?: throw IllegalArgumentException("Unknown contract type: $type")
        return parser.parse(contractData)
    }

    /**
     * Parses an [ArkContract] from a raw contract string.
     *
     * The string must begin with `arkcontract=` and be formatted as `&`-separated `key=value`
     * pairs. The `arkcontract` key's value determines which registered parser is used.
     *
     * @param contract the raw contract string (e.g. `"arkcontract=Boarding&server=tr(...)"`).
     * @return the [ArkContract] produced by the matching parser.
     * @throws IllegalArgumentException if the string does not start with `arkcontract=`,
     *   the `arkcontract` type key is missing, or no parser is registered for the type.
     */
    fun parse(contract: String): ArkContract {
        require(contract.startsWith(ARK_CONTRACT)) {
            "Invalid contract format: $contract. Must start with '$ARK_CONTRACT'"
        }
        val data = getAdditionalData(contract)
        val type = data[ARK_CONTRACT]
        requireNotNull(type) { "Contract type not found in additional data" }
        return parse(data, type)
    }

    /**
     * Registers a new [ArkContractParser] with this instance.
     *
     * Parsers are matched in the order they were registered; registering a parser with the
     * same [ArkContractParser.type] as an existing one will shadow the earlier registration.
     *
     * @param parser the parser to add to the registry.
     */
    fun registerParser(parser: ArkContractParser) {
        parsers.add(parser)
    }

    companion object {
        private const val ARK_CONTRACT = "arkcontract="
    }
}
