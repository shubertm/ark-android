package com.arkade.core.wallet.addresses

interface AddressProvider {
    suspend fun isOurDescriptor(descriptor: String): Boolean

    suspend fun getNextSigningDescriptor(): String
}
