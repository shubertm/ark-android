package com.arkade.core.contracts

import com.arkade.core.ArkAddress
import com.arkade.core.ArkServerInfo
import com.arkade.core.UNSPENDABLE_PUBKEY
import com.arkade.core.buildScriptTree
import com.arkade.core.taproot.Parity
import com.arkade.core.taproot.TaprootSpendingInfo
import com.arkade.core.toXOnlyPubKey

abstract class ArkContract(
    private val serverInfo: ArkServerInfo,
) {
    abstract val type: String

    val serverPubKey = serverInfo.signerPubKey.value.toByteArray()

    override fun toString(): String {
        val data = getAdditionalData().toMutableMap()
        data.remove("arkcontract")
        val dataString =
            data.entries.joinToString("&") {
                "${it.key}=${it.value}"
            }
        return "arkcontract=$type&$dataString"
    }

    fun getArkAddress(): ArkAddress {
        val taprootSpendingInfo = getTaprootSpendingInfo()
        return ArkAddress.create(
            serverInfo.network,
            serverPubKey,
            taprootSpendingInfo.outputKey.value.toByteArray(),
        )
    }

    fun getScriptPubKey(): String = getArkAddress().toP2TRScriptPubkey().toHexString()

    private fun getTaprootSpendingInfo(): TaprootSpendingInfo {
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
