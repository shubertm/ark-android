package com.arkade.core.bitcoin

import com.arkade.core.toXOnlyPubKey
import fr.acinq.bitcoin.XonlyPublicKey

class Descriptor private constructor() {
    private var xOnlyPubKey: XonlyPublicKey? = null
    private constructor (string: String) : this() {
        this.xOnlyPubKey =
            if (string.startsWith("tr(") && string.endsWith(")")) {
                string.removeSurrounding("tr(", ")").toXOnlyPubKey()
            } else {
                string.toXOnlyPubKey()
            }
    }

    private constructor(pubKey: XonlyPublicKey) : this() {
        this.xOnlyPubKey = pubKey
    }

    fun encodeTaproot(): String {
        val xOnlyPublicKey =
            requireNotNull(xOnlyPubKey) {
                "Missing x-only public key"
            }
        return "tr(${xOnlyPublicKey.value.toHex()})"
    }

    companion object {
        fun from(string: String): Descriptor = Descriptor(string)

        fun from(xPubKey: XonlyPublicKey): Descriptor = Descriptor(xPubKey)
    }
}
