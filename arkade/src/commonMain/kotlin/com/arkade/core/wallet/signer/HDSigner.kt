package com.arkade.core.wallet.signer

import com.arkade.core.bitcoin.Network
import com.arkade.core.encodePubKeyByNetwork
import com.arkade.core.wallet.Wallet.Companion.getAccountKeyPath
import com.arkade.core.wallet.Wallet.Companion.masterKeyFromSecret
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.XonlyPublicKey
import fr.acinq.bitcoin.psbt.Psbt
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HDSigner private constructor(
    mnemonics: String,
    private val network: Network,
) : SignerImpl() {
    private val mutex = Mutex()
    private val extendedKey = masterKeyFromSecret(mnemonics)
    private val accountKeyPath = getAccountKeyPath(network).first
    private val accountXPrivateKey = extendedKey.first.derivePrivateKey(accountKeyPath)
    private val changeXPrivateKey = accountXPrivateKey.derivePrivateKey(0)
    override lateinit var privateKey: PrivateKey

    override suspend fun sign(
        descriptor: String,
        psbt: Psbt,
        inputIndexes: Array<Int>,
    ): Transaction =
        mutex.withLock {
            deriveChildPrivateKey(descriptor)
            super.sign(descriptor, psbt, inputIndexes)
        }

    override suspend fun signMessage(
        descriptor: String,
        message: ByteArray,
        signatureType: SignatureType,
    ): ByteArray =
        mutex.withLock {
            deriveChildPrivateKey(descriptor)
            super.signMessage(descriptor, message, signatureType)
        }

    override suspend fun signerSession(): SignerSession {
        TODO("Not yet implemented")
    }

    override fun xOnlyPublicKey(descriptor: String): XonlyPublicKey {
        deriveChildPrivateKey(descriptor)
        return super.xOnlyPublicKey(descriptor)
    }

    override fun accountDescriptor(): String {
        val (_, coinType) = getAccountKeyPath(network)
        val accountPublicKey = encodePubKeyByNetwork(accountXPrivateKey.extendedPublicKey, network)
        val fingerprint = extendedKey.second
        require(fingerprint.length == 8) { "Invalid fingerprint length: expected 8 but is ${fingerprint.length}" }
        val accountDescriptor = "tr([$fingerprint/86'/$coinType'/0']$accountPublicKey/0/*)"
        return accountDescriptor
    }

    // Should be called before sign, signMessage or any function using privateKey
    private fun deriveChildPrivateKey(descriptor: String) {
        val childKeyIndex = descriptor.substringAfterLast('/').substringBefore(')').toLongOrNull()
        requireNotNull(childKeyIndex) { "Invalid descriptor: $descriptor" }
        privateKey = changeXPrivateKey.derivePrivateKey(childKeyIndex).privateKey
    }

    companion object {
        fun fromMnemonic(
            mnemonics: String,
            network: Network,
        ): Signer = HDSigner(mnemonics, network)
    }
}
