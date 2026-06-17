package com.arkade.core.contracts

/**
 * A flexible [ArkContractParser] implementation backed by a user-supplied parsing function.
 *
 * [GenericArkContractParser] allows registering a typed contract parser without needing to
 * create a dedicated class. The parsing logic is provided as a lambda at construction time.
 *
 * This is the mechanism used by [ArkContractParserImpl] to register [ArkBoardingContract]
 * and other contract types:
 * ```kotlin
 * registerParser(GenericArkContractParser(ArkBoardingContract.TYPE, ArkBoardingContract::parse))
 * ```
 *
 * @param type the contract type string that this parser handles.
 * @param parseImpl a function that takes a key/value data map and returns an [ArkContract].
 */
class GenericArkContractParser(
    override val type: String,
    private val parseImpl: (Map<String, String>) -> ArkContract,
) : ArkContractParser {
    override fun parse(data: Map<String, String>): ArkContract = parseImpl(data)
}
