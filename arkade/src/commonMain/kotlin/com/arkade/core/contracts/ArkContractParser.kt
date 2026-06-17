package com.arkade.core.contracts

/**
 * Interface for parsing a specific type of [ArkContract] from serialized key/value data.
 *
 * Each implementation is responsible for a single contract [type]. Implementations are
 * registered with [ArkContractParserImpl], which dispatches parsing to the appropriate
 * parser based on the `arkcontract` value found in the contract string.
 *
 * @see ArkContractParserImpl
 * @see GenericArkContractParser
 */
interface ArkContractParser {
    /**
     * The contract type string this parser handles (e.g. `"Boarding"`, `"generic"`).
     * Must match the [ArkContract.type] value of the contracts this parser produces.
     */
    val type: String

    /**
     * Parses an [ArkContract] from the given key/value data map.
     *
     * @param data a map of contract parameters, typically produced by [getAdditionalData].
     * @return the parsed [ArkContract] instance.
     * @throws IllegalArgumentException if required keys are missing or data is invalid.
     */
    fun parse(data: Map<String, String>): ArkContract

    companion object {
        /**
         * Splits a serialized contract string into a key/value map.
         *
         * The input is expected to be an `&`-separated sequence of `key=value` pairs
         * (e.g. `"arkcontract=Boarding&server=tr(...)&exit_delay=144"`). Each pair must
         * contain exactly one `=` separator.
         *
         * @param contract the raw contract string to parse.
         * @return a map of all key/value pairs found in the string.
         * @throws IllegalArgumentException if any entry does not contain exactly two segments
         *   separated by `=`.
         */
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
