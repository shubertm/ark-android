package com.arkade.core.wallet.addresses

/**
 * Provides wallet-specific output descriptors used for receiving funds and signing.
 *
 * Implementations know how to derive the next descriptor to use for signing and how to
 * recognize descriptors that belong to the wallet they represent.
 */
interface AddressProvider {
    /**
     * Checks whether the given descriptor belongs to this wallet.
     *
     * @param descriptor The output descriptor to check.
     * @return `true` if `descriptor` is one of this wallet's own descriptors, `false` otherwise.
     */
    suspend fun isOurDescriptor(descriptor: String): Boolean

    /**
     * Retrieves the next output descriptor that should be used for signing.
     *
     * @return The next signing descriptor.
     */
    suspend fun getNextSigningDescriptor(): String
}
