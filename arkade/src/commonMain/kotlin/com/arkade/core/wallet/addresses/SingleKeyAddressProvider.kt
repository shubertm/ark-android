package com.arkade.core.wallet.addresses

/**
 * [AddressProvider] for single-key wallets.
 *
 * Since a single-key wallet always signs with the same key, this implementation always
 * returns the same [accountDescriptor] and compares descriptors by the embedded public key
 * rather than by a derived index.
 *
 * @param accountDescriptor The wallet's fixed output descriptor.
 */
class SingleKeyAddressProvider(
    private val accountDescriptor: String,
) : AddressProvider {
    /**
     * Checks whether [descriptor] carries the same public key as [accountDescriptor].
     *
     * The public key is extracted as the substring between `]` and `/` in each descriptor.
     *
     * @param descriptor The descriptor to check.
     * @return `true` if the public keys match, `false` otherwise.
     */
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

    /**
     * Returns the wallet's fixed [accountDescriptor], since single-key wallets have no
     * per-index derivation.
     *
     * @return The wallet's [accountDescriptor].
     */
    override suspend fun getNextSigningDescriptor(): String = accountDescriptor
}
