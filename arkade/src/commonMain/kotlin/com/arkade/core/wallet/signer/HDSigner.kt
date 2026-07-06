package com.arkade.core.wallet.signer

import com.arkade.core.wallet.Wallet

class HDSigner private constructor(
    mnemonics: String,
) : SignerImpl() {
    private val extendedKey = Wallet.Companion.masterKeyFromSecret(mnemonics)
    override val privateKey = extendedKey.first.privateKey

    override suspend fun signerSession(): SignerSession {
        TODO("Not yet implemented")
    }

    companion object {
        fun fromMnemonic(mnemonics: String): Signer = HDSigner(mnemonics)
    }
}
