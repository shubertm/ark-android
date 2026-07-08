package com.arkade.core.wallet.signer

import com.arkade.core.wallet.Wallet

class SingleKeySigner private constructor(
    secret: String,
) : SignerImpl() {
    override var privateKey = Wallet.getPrivateKeyFromNSec(secret)

    override suspend fun signerSession(): SignerSession {
        TODO("Not yet implemented")
    }

    override fun accountDescriptor(): String = "tr(${privateKey.publicKey().xOnly().value.toHex()})"

    companion object {
        fun fromNSec(secret: String): Signer = SingleKeySigner(secret)
    }
}
