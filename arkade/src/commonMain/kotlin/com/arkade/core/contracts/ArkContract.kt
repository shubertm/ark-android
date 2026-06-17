package com.arkade.core.contracts

import com.arkade.core.ArkAddress
import com.arkade.core.UNSPENDABLE_PUBKEY
import com.arkade.core.bitcoin.Network
import com.arkade.core.buildScriptTree
import com.arkade.core.taproot.Parity
import com.arkade.core.taproot.TaprootSpendingInfo
import com.arkade.core.taproot.pubKeyFromTaprootDescriptor
import com.arkade.core.toXOnlyPubKey

abstract class ArkContract(
    protected val serverDescriptor: String,
) {
    abstract val type: String

    override fun toString(): String {
        val data = getAdditionalData().toMutableMap()
        data.remove("arkcontract")
        val dataString =
            data.entries.joinToString("&") {
                "${it.key}=${it.value}"
            }
        val arkContract = "arkcontract=$type"
        return if (dataString.isEmpty()) arkContract else "$arkContract&$dataString"
    }

    open fun getArkAddress(network: Network): ArkAddress {
        val taprootSpendingInfo = getTaprootSpendingInfo()
        return ArkAddress.create(
            network,
            pubKeyFromTaprootDescriptor(serverDescriptor).hexToByteArray(),
            taprootSpendingInfo.outputKey.value.toByteArray(),
        )
    }

    open fun getScriptPubKey(network: Network): String = getArkAddress(network).toP2TRScriptPubkey().toHexString()

    protected fun getTaprootSpendingInfo(): TaprootSpendingInfo {
        val unSpendablePubKey = UNSPENDABLE_PUBKEY.toXOnlyPubKey()

        val leafScripts = getTapLeafScripts()

        val scriptTree = buildScriptTree(leafScripts)
        val merkleRoot = scriptTree.hash()

        val (outputKey, isOdd) = unSpendablePubKey.outputKey(merkleRoot)

        val taprootSpendingInfo =
            TaprootSpendingInfo(
                unSpendablePubKey,
                outputKey,
                Parity.fromBooleanIsOdd(isOdd),
                merkleRoot,
                scriptTree,
            )
        return taprootSpendingInfo
    }

    abstract fun getTapLeafScripts(): List<ByteArray>

    abstract fun getAdditionalData(): Map<String, String>
}
