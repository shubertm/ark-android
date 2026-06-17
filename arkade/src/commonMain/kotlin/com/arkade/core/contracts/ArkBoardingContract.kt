package com.arkade.core.contracts

import com.arkade.core.ArkAddress
import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Network
import com.arkade.core.csvSigScript
import com.arkade.core.multisigScript
import com.arkade.core.taproot.getTaprootScriptPubKey
import com.arkade.core.taproot.pubKeyFromTaprootDescriptor
import com.arkade.core.toXOnlyPubKey

/**
 * An Ark boarding contract that allows a user to move on-chain Bitcoin funds into the Ark protocol.
 *
 * The contract produces a Taproot address with two spending paths:
 * - **Collaborative exit**: a 2-of-2 multisig between the server and the user, allowing
 *   cooperative settlement.
 * - **Unilateral exit**: a CSV-timelocked script spendable solely by the user after
 *   [exitDelay] blocks, providing a trustless fallback if the server becomes unresponsive.
 *
 * Because boarding contracts settle directly on-chain, [getArkAddress] is not supported.
 * Use [getOnChainAddress] to obtain the native Bitcoin P2TR address for this contract.
 *
 * @param serverDescriptor a Taproot descriptor (e.g. `tr(<xOnlyPubKeyHex>)`) for the Ark server's public key.
 * @param userDescriptor a Taproot descriptor for the user's public key.
 * @param exitDelay the CSV lock time (in blocks) for the unilateral exit path.
 */
class ArkBoardingContract(
    serverDescriptor: String,
    private val userDescriptor: String,
    private val exitDelay: Long,
) : ArkContract(serverDescriptor) {
    override val type: String = TYPE

    /**
     * Returns the on-chain Bitcoin P2TR address for this boarding contract on the given [network].
     *
     * @param network the Bitcoin network to derive the address for.
     * @return the native Bitcoin [Address] corresponding to this contract's Taproot output.
     */
    fun getOnChainAddress(network: Network): Address {
        val taprootSpendingInfo = getTaprootSpendingInfo()
        val scriptPubKey = getTaprootScriptPubKey(taprootSpendingInfo.outputKey.value.toByteArray())
        return Address.fromScriptPubKey(
            scriptPubKey,
            network,
        )
    }

    /**
     * Not supported for boarding contracts. Boarding contracts use on-chain Bitcoin addresses.
     *
     * @throws UnsupportedOperationException always. Use [getOnChainAddress] instead.
     */
    override fun getArkAddress(network: Network): ArkAddress =
        throw UnsupportedOperationException("Boarding contracts use on-chain Bitcoin addresses. Use getOnChainAddress(network) instead.")

    /**
     * Returns the P2TR `scriptPubKey` for this boarding contract as a hex string.
     *
     * Derived from the on-chain address via [getOnChainAddress].
     *
     * @param network the Bitcoin network.
     * @return the hex-encoded P2TR scriptPubKey.
     */
    override fun getScriptPubKey(network: Network): String = getOnChainAddress(network).toScriptPubKey().toHexString()

    /**
     * Returns the two Tapscript leaf scripts for this boarding contract.
     *
     * The first element is the collaborative exit multisig script (server + user).
     * The second element is the unilateral exit CSV script (user only, after [exitDelay] blocks).
     *
     * @return a list containing [collaborativeExitScript, unilateralExitScript].
     */
    override fun getTapLeafScripts(): List<ByteArray> {
        val serverPubKey = pubKeyFromTaprootDescriptor(serverDescriptor).toXOnlyPubKey()
        val userPubKey = pubKeyFromTaprootDescriptor(userDescriptor).toXOnlyPubKey()
        val collaborativeExitScript = multisigScript(serverPubKey, userPubKey)
        val unilateralExitScript = csvSigScript(exitDelay, userPubKey)
        return listOf(collaborativeExitScript, unilateralExitScript)
    }

    /**
     * Returns the additional data required to reconstruct this boarding contract.
     *
     * Keys: `server` (server Taproot descriptor), `user` (user Taproot descriptor),
     * `exit_delay` (CSV lock time as a string).
     *
     * @return a map with keys `server`, `user`, and `exit_delay`.
     */
    override fun getAdditionalData(): Map<String, String> =
        mapOf(
            "server" to serverDescriptor,
            "user" to userDescriptor,
            "exit_delay" to exitDelay.toString(),
        )

    companion object {
        /** The contract type identifier for boarding contracts. */
        const val TYPE = "Boarding"

        /**
         * Parses an [ArkBoardingContract] from a key/value data map.
         *
         * Expected keys: `server` (server Taproot descriptor), `user` (user Taproot descriptor),
         * and optionally `exit_delay` (defaults to `0` if absent or not parseable).
         *
         * @param data the key/value map produced by [ArkContractParser.getAdditionalData].
         * @return an [ArkBoardingContract] constructed from the provided data.
         * @throws IllegalArgumentException if `server` or `user` keys are missing.
         */
        fun parse(data: Map<String, String>): ArkContract {
            val serverPubKeyDescriptor = data["server"]
            val userPubKeyDescriptor = data["user"]
            val exitDelay = data["exit_delay"]?.toLong()
            requireNotNull(serverPubKeyDescriptor) { "Invalid server public key" }
            requireNotNull(userPubKeyDescriptor) { "Invalid user public key" }
            return ArkBoardingContract(
                serverPubKeyDescriptor,
                userPubKeyDescriptor,
                exitDelay ?: 0,
            )
        }
    }
}
