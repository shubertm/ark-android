package com.arkade.core.wallet

import com.arkade.core.bitcoin.Network
import com.arkade.core.wallet.addresses.HDAddressProvider
import com.arkade.core.wallet.signer.HDSigner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class HDSignerTest : SignerTest() {
    private val secret = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    private val signer = HDSigner.fromMnemonic(secret, Network.TESTNET)
    private var lastUsedIndex = 0
    val addressProvider =
        HDAddressProvider(
            signer.accountDescriptor(),
            getLastUsedIndex = {
                lastUsedIndex
            },
            updateLastUsedIndex = {
                lastUsedIndex = it
            },
        )

    @Test
    fun should_sign_transaction_successfully() {
        runTest {
            val descriptor = addressProvider.getNextSigningDescriptor()
            testSigningTransaction(descriptor, signer)
        }
    }

    @Test
    fun should_sign_message_using_schnorr_correctly() {
        runTest {
            val descriptor = addressProvider.getNextSigningDescriptor()
            testSigningMessageUsingSchnorr(descriptor, signer)
        }
    }

    @Test
    fun should_sign_transaction_with_different_descriptors() {
        runTest {
            val descriptors = hashSetOf<String>()
            for (i in 0..10) {
                val descriptor = addressProvider.getNextSigningDescriptor()
                assertTrue(descriptors.add(descriptor))
                testSigningTransaction(descriptor, signer)
            }
        }
    }
}
