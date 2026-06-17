package com.arkade.core.contracts

import com.arkade.core.ArkAddress
import com.arkade.core.bitcoin.Address
import com.arkade.core.bitcoin.Network
import com.arkade.core.csvSigScript
import com.arkade.core.multisigScript
import com.arkade.core.taproot.getTaprootScriptPubKey
import com.arkade.core.taproot.pubKeyFromTaprootDescriptor
import com.arkade.core.toXOnlyPubKey

class ArkBoardingContract(
    serverDescriptor: String,
    private val userDescriptor: String,
    private val exitDelay: Long,
) : ArkContract(serverDescriptor) {
    override val type: String = TYPE

    fun getOnChainAddress(network: Network): Address {
        val taprootSpendingInfo = getTaprootSpendingInfo()
        val scriptPubKey = getTaprootScriptPubKey(taprootSpendingInfo.outputKey.value.toByteArray())
        return Address.fromScriptPubKey(
            scriptPubKey,
            network,
        )
    }

    override fun getArkAddress(network: Network): ArkAddress =
        throw UnsupportedOperationException("Boarding contracts use on-chain Bitcoin addresses. Use getOnChainAddress(network) instead.")

    override fun getScriptPubKey(network: Network): String = getOnChainAddress(network).toScriptPubKey().toHexString()

    override fun getTapLeafScripts(): List<ByteArray> {
        val serverPubKey = pubKeyFromTaprootDescriptor(serverDescriptor).toXOnlyPubKey()
        val userPubKey = pubKeyFromTaprootDescriptor(userDescriptor).toXOnlyPubKey()
        val collaborativeExitScript = multisigScript(serverPubKey, userPubKey)
        val unilateralExitScript = csvSigScript(exitDelay, userPubKey)
        return listOf(collaborativeExitScript, unilateralExitScript)
    }

    override fun getAdditionalData(): Map<String, String> =
        mapOf(
            "server" to serverDescriptor,
            "user" to userDescriptor,
            "exit_delay" to exitDelay.toString(),
        )

    companion object {
        const val TYPE = "Boarding"

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
