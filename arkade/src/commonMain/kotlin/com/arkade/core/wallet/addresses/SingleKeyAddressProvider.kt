package com.arkade.core.wallet.addresses

class SingleKeyAddressProvider(
    private val accountDescriptor: String,
) : AddressProvider {
    override suspend fun isOurDescriptor(descriptor: String): Boolean {
        val expectedPublicKey =
            accountDescriptor
                .substringAfter("]")
                .substringBefore("/")
        val actualPublicKey =
            descriptor
                .substringAfter("]")
                .substringBefore("/")
        return expectedPublicKey == actualPublicKey
    }

    override suspend fun getNextSigningDescriptor(): String = accountDescriptor
}
