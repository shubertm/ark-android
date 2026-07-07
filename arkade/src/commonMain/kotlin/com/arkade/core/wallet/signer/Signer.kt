package com.arkade.core.wallet.signer

import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.XonlyPublicKey
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.getOrElse

interface Signer {
    suspend fun sign(
        psbt: Psbt,
        inputIndexes: Array<Int>,
    ): Transaction

    suspend fun signMessage(
        message: ByteArray,
        signatureType: SignatureType = SignatureType.SCHNORR,
    ): ByteArray

    suspend fun signerSession(): SignerSession

    fun xOnlyPublicKey(): XonlyPublicKey
}

abstract class SignerImpl : Signer {
    protected abstract val privateKey: PrivateKey

    override suspend fun sign(
        psbt: Psbt,
        inputIndexes: Array<Int>,
    ): Transaction {
        var signedTx = psbt
        if (inputIndexes.isEmpty()) {
            val txInputs = signedTx.global.tx.txIn
            txInputs.forEach { input ->
                val result =
                    signedTx.sign(privateKey, input.outPoint.index.toInt()).getOrElse {
                        throw IllegalStateException("Failed to sign transaction: $it")
                    }
                signedTx = result.psbt
            }
            return signedTx.global.tx
        }

        inputIndexes.forEach { index ->
            val result =
                signedTx.sign(privateKey, index).getOrElse {
                    throw IllegalStateException("Failed to sign transaction: $it")
                }
            signedTx = result.psbt
        }
        return signedTx.global.tx
    }

    override suspend fun signMessage(
        message: ByteArray,
        signatureType: SignatureType,
    ): ByteArray =
        when (signatureType) {
            SignatureType.SCHNORR -> {
                require(message.size == 32) { "Invalid message size for Schnorr signing: ${message.size}" }
                Crypto
                    .signSchnorr(
                        ByteVector32(message),
                        privateKey,
                        taprootTweak = null,
                        auxrand32 = null,
                    ).toByteArray()
            }
            SignatureType.ECDSA -> Crypto.sign(message, privateKey).toByteArray()
        }

    override fun xOnlyPublicKey(): XonlyPublicKey = privateKey.xOnlyPublicKey()
}

enum class SignatureType {
    SCHNORR,
    ECDSA,
}
