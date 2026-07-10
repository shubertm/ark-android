package com.arkade.core.wallet

import com.arkade.core.wallet.addresses.SingleKeyAddressProvider
import com.arkade.core.wallet.signer.SingleKeySigner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SingleKeySignerTest : SignerTest() {
    val signer = SingleKeySigner.fromNSec("nsec1wr49duqpjavggh78ewu9zlcuvw5huh6x5kqweqwnmjgw78kqqt6qsk0w9k")
    val addressProvider = SingleKeyAddressProvider(signer.accountDescriptor())

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
}
