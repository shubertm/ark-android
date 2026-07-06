package com.arkade.core.wallet

import com.arkade.core.taproot.getTaprootScriptPubKey
import com.arkade.core.wallet.signer.HDSigner
import fr.acinq.bitcoin.ByteVector
import fr.acinq.bitcoin.ByteVector32
import fr.acinq.bitcoin.Crypto
import fr.acinq.bitcoin.Crypto.sha256
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.Satoshi
import fr.acinq.bitcoin.ScriptFlags
import fr.acinq.bitcoin.SigHash
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.TxIn
import fr.acinq.bitcoin.TxOut
import fr.acinq.bitcoin.psbt.Psbt
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class HDSignerTest {
    private val secret = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    private val signer = HDSigner.fromMnemonic(secret)

    @Test
    fun should_sign_transaction_successfully() {
        runTest {
            val (outputKey, _) = signer.xOnlyPublicKey().outputKey(Crypto.TaprootTweak.KeyPathTweak)
            val scriptPubKey = getTaprootScriptPubKey(outputKey.value.toByteArray())

            val prevOut =
                TxOut(
                    Satoshi(10000),
                    scriptPubKey,
                )
            val prevTx =
                Transaction(
                    3,
                    listOf(),
                    listOf(prevOut),
                    0,
                )

            val input =
                TxIn(
                    OutPoint(prevTx.txid, 0),
                    0,
                )
            val output =
                TxOut(
                    Satoshi(6000),
                    scriptPubKey,
                )
            val tx =
                Transaction(
                    3,
                    listOf(input),
                    listOf(output),
                    0,
                )

            val psbt =
                with(Psbt(tx)) {
                    updateWitnessInput(
                        input.outPoint,
                        output,
                        sighashType = SigHash.SIGHASH_ALL,
                        taprootInternalKey = signer.xOnlyPublicKey(),
                    )
                }.right!!

            val singedTx = signer.sign(psbt, arrayOf(0))

            singedTx.correctlySpends(listOf(prevTx), ScriptFlags.MANDATORY_SCRIPT_VERIFY_FLAGS)
        }
    }

    @Test
    fun should_sign_message_using_schnorr_correctly() {
        runTest {
            val message = sha256("message".encodeToByteArray())
            val signature = signer.signMessage(message)
            assertTrue {
                Crypto.verifySignatureSchnorr(ByteVector32(message), ByteVector(signature), signer.xOnlyPublicKey())
            }
        }
    }
}
