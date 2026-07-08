package com.arkade.core.wallet

import com.arkade.core.bitcoin.Network
import com.arkade.core.wallet.signer.HDSigner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class HDSignerTest : SignerTest() {
    private val secret = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    private val signer = HDSigner.fromMnemonic(secret, Network.TESTNET)

    @Test
    fun should_sign_transaction_successfully() {
        runTest { testSigningTransaction(signer) }
    }

    @Test
    fun should_sign_message_using_schnorr_correctly() {
        runTest { testSigningMessageUsingSchnorr(signer) }
    }
}
