package com.arkade.core.wallet.signer

import com.arkade.core.wallet.Wallet

class SingleKeySigner private constructor(
    secret: String,
) : SignerImpl() {
    override val privateKey = Wallet.getPrivateKeyFromNSec(secret)

    override suspend fun signerSession(): SignerSession {
        TODO("Not yet implemented")
    }

    companion object {
        fun fromNSec(secret: String): Signer = SingleKeySigner(secret)
    }
}
