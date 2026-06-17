package com.arkade.core.contracts

/**
 * A flexible, general-purpose [ArkContract] that accepts arbitrary tap leaf scripts and metadata.
 *
 * [GenericArkContract] is useful when a specific contract type is not available but the
 * tap leaf scripts and optional additional data are already known. It is also used internally
 * by [ArkContractParserImpl] as the base for delegating to typed parsers such as
 * [ArkBoardingContract].
 *
 * The [type] is always `"generic"` and cannot be overridden.
 *
 * @param serverDescriptor a Taproot descriptor for the Ark server's public key.
 * @param tapLeafScripts the list of serialized tap leaf scripts for this contract's script tree.
 * @param additionalData optional key/value metadata. Returns an empty map if `null`.
 */
class GenericArkContract(
    serverDescriptor: String,
    private val tapLeafScripts: List<ByteArray>,
    private val additionalData: Map<String, String>? = null,
) : ArkContract(serverDescriptor) {
    override val type: String = "generic"

    override fun getTapLeafScripts(): List<ByteArray> = tapLeafScripts

    override fun getAdditionalData(): Map<String, String> = additionalData ?: emptyMap()
}
