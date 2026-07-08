package com.arkade.core.wallet.signer

import com.arkade.core.bitcoin.Network
import com.arkade.core.wallet.Wallet
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt

class HDSigner private constructor(
    mnemonics: String,
    network: Network,
) : SignerImpl() {
    private val extendedKey = Wallet.masterKeyFromSecret(mnemonics)
    private val accountKeyPath = Wallet.getAccountKeyPath(network).first
    private val accountXPrivateKey = extendedKey.first.derivePrivateKey(accountKeyPath)
    override val privateKey = accountXPrivateKey.derivePrivateKey(0).privateKey

    override suspend fun sign(
        psbt: Psbt,
        inputIndexes: Array<Int>,
    ): Transaction = super.sign(psbt, inputIndexes)

    override suspend fun signMessage(
        message: ByteArray,
        signatureType: SignatureType,
    ): ByteArray = super.signMessage(message, signatureType)

    override suspend fun signerSession(): SignerSession {
        TODO("Not yet implemented")
    }

    companion object {
        fun fromMnemonic(
            mnemonics: String,
            network: Network,
        ): Signer = HDSigner(mnemonics, network)
    }
}
