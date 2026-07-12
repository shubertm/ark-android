package com.arkade.core.wallet.signer

import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.PrivateKey
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.XonlyPublicKey
import fr.acinq.bitcoin.psbt.Psbt
import fr.acinq.bitcoin.utils.getOrElse

/**
 * Signing abstraction for a wallet's key material.
 *
 * A [Signer] knows how to sign PSBT transaction inputs and arbitrary messages for a given
 * output descriptor, and how to expose the public key material needed to build such
 * descriptors.
 */
interface Signer {
    /**
     * Signs the given [psbt] using the key derived for [descriptor].
     *
     * @param descriptor The output descriptor identifying which key to sign with.
     * @param psbt The PSBT to sign.
     * @param inputIndices The indexes of the inputs to sign; if empty, all inputs are signed.
     * @return The fully signed [Transaction].
     */
    suspend fun sign(
        descriptor: String,
        psbt: Psbt,
        inputIndices: Array<Int>,
    ): Transaction

    /**
     * Signs the given [psbt] using the key derived for [descriptor].
     *
     * @param descriptor The output descriptor identifying which key to sign with.
     * @param psbt The PSBT to sign.
     * @param outpoints The outpoints of the inputs to sign; if empty, all inputs are signed.
     * @return The fully signed [Transaction].
     */
    suspend fun sign(
        descriptor: String,
        psbt: Psbt,
        outpoints: Array<OutPoint>,
    ): Transaction

    /**
     * Signs an arbitrary [message] using the key derived for [descriptor].
     *
     * @param descriptor The output descriptor identifying which key to sign with.
     * @param message The message bytes to sign.
     * @param signatureType The signature scheme to use, defaults to [SignatureType.SCHNORR].
     * @return The resulting signature bytes.
     */
    suspend fun signMessage(
        descriptor: String,
        message: ByteArray,
        signatureType: SignatureType = SignatureType.SCHNORR,
    ): ByteArray

    /**
     * Creates a new [SignerSession].
     *
     * @return A new [SignerSession] instance.
     */
    suspend fun signerSession(): SignerSession

    /**
     * Derives the x-only public key associated with [descriptor].
     *
     * @param descriptor The output descriptor identifying which key to derive.
     * @return The corresponding [XonlyPublicKey].
     */
    suspend fun xOnlyPublicKey(descriptor: String): XonlyPublicKey

    /**
     * Returns this signer's account-level output descriptor.
     *
     * @return The account descriptor string.
     */
    fun accountDescriptor(): String
}

/**
 * Base [Signer] implementation shared by concrete signer types.
 *
 * Implements PSBT input signing and message signing on top of a single [privateKey], which
 * subclasses are responsible for deriving and keeping up to date for the descriptor currently
 * being operated on.
 */
abstract class
SignerImpl : Signer {
    /**
     * The private key currently used for signing and public key derivation. Subclasses must
     * ensure this is set to the key corresponding to the descriptor being operated on before
     * delegating to this class's implementations.
     */
    protected abstract var privateKey: PrivateKey

    /**
     * Signs the requested inputs of [psbt] with [privateKey].
     *
     * @param descriptor The output descriptor identifying which key to sign with.
     * @param psbt The PSBT to sign.
     * @param inputIndexes The indexes of the inputs to sign; if empty, all inputs are signed.
     * @return The fully signed [Transaction].
     * @throws IllegalStateException if signing any of the targeted inputs fails.
     */
    override suspend fun sign(
        descriptor: String,
        psbt: Psbt,
        inputIndices: Array<Int>,
    ): Transaction {
        var signedTx = psbt
        val indices =
            if (inputIndices.isEmpty()) {
                signedTx.inputs.indices
            } else {
                inputIndices.asList()
            }

        signedTx = signAll(signedTx, indices)
        return signedTx.global.tx
    }

    /**
     * Signs the requested inputs of [psbt] with [privateKey], identified by their outpoints.
     *
     * @param descriptor The output descriptor identifying which key to sign with.
     * @param psbt The PSBT to sign.
     * @param outpoints The outpoints of the inputs to sign; if empty, all inputs are signed.
     * @return The fully signed [Transaction].
     * @throws IllegalStateException if signing any of the targeted inputs fails.
     */
    override suspend fun sign(
        descriptor: String,
        psbt: Psbt,
        outpoints: Array<OutPoint>,
    ): Transaction {
        var signedTx = psbt
        if (outpoints.isEmpty()) {
            signedTx = signAll(signedTx)
            return signedTx.global.tx
        }

        outpoints.forEach { outpoint ->
            val result =
                signedTx.sign(privateKey, outpoint).getOrElse {
                    throw IllegalStateException("Failed to sign transaction")
                }
            signedTx = result.psbt
        }
        return signedTx.global.tx
    }

    /**
     * Signs [message] with [privateKey] using the requested [signatureType].
     *
     * @param descriptor The output descriptor identifying which key to sign with.
     * @param message The message bytes to sign.
     * @param signatureType The signature scheme to use.
     * @return The resulting signature bytes.
     * @throws IllegalArgumentException if [signatureType] is [SignatureType.SCHNORR] and
     * [message] is not exactly 32 bytes long.
     */
    override suspend fun signMessage(
        descriptor: String,
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

    /**
     * Derives the x-only public key of [privateKey].
     *
     * @param descriptor The output descriptor identifying which key to derive.
     * @return The corresponding [XonlyPublicKey].
     */
    override suspend fun xOnlyPublicKey(descriptor: String): XonlyPublicKey = privateKey.xOnlyPublicKey()

    /**
     * Signs each of [indices] on [psbt] with [privateKey], in order, folding the result of each
     * signature into the PSBT passed to the next.
     *
     * @param psbt The PSBT to sign.
     * @param indices The input indices to sign; defaults to all of [psbt]'s inputs.
     * @return The PSBT with all requested inputs signed.
     * @throws IllegalStateException if signing any of the targeted inputs fails.
     */

    private fun signAll(
        psbt: Psbt,
        indices: Iterable<Int> = psbt.inputs.indices,
    ): Psbt {
        var signedTx = psbt
        indices.forEach { index ->
            val result =
                signedTx.sign(privateKey, index).getOrElse {
                    throw IllegalStateException("Failed to sign transaction")
                }
            signedTx = result.psbt
        }
        return signedTx
    }
}

/**
 * The signature scheme used when signing an arbitrary message via [Signer.signMessage].
 */
enum class SignatureType {
    /** BIP-340 Schnorr signature; requires a 32-byte message (typically a digest). */
    SCHNORR,

    /** ECDSA signature. */
    ECDSA,
}
