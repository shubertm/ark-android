package com.arkade.core.wallet.addresses

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [AddressProvider] for HD (hierarchical deterministic) wallets.
 *
 * Derives per-index descriptors from a wildcard account descriptor (containing `/‍*`) by
 * substituting the wildcard with a concrete child index. The last used index is tracked
 * externally via [getLastUsedIndex] and [updateLastUsedIndex], and access to it is
 * serialized with a [Mutex] to avoid concurrent callers deriving the same index.
 *
 * @param accountDescriptor The wildcard account descriptor (e.g. containing <code>&#47;*</code>) used to
 * derive per-index descriptors.
 * @param getLastUsedIndex Supplies the current last-used index.
 * @param updateLastUsedIndex Persists a new last-used index; defaults to a no-op.
 */
class HDAddressProvider(
    private val accountDescriptor: String,
    private val getLastUsedIndex: () -> Int,
    private val updateLastUsedIndex: suspend (Int) -> Boolean,
) : AddressProvider {
    private val mutex = Mutex()

    /**
     * Atomically increments the last used index and returns the descriptor derived from the
     * new index.
     *
     * @return The descriptor for the newly reserved index.
     */
    override suspend fun getNextSigningDescriptor(): String =
        mutex.withLock {
            val newIndex = getLastUsedIndex() + 1
            if (updateLastUsedIndex(newIndex)) {
                getNextDescriptorFromIndex(accountDescriptor, newIndex)
            } else {
                throw IllegalStateException("Cannot create next descriptor, failed to update last used index")
            }
        }

    /**
     * Checks whether [descriptor] matches the descriptor derived from the index encoded in
     * its suffix.
     *
     * @param descriptor The descriptor to check, expected to end with `/<index>)`.
     * @return `true` if the parsed index derives a descriptor equal to [descriptor],
     * `false` if it doesn't match or the index can't be parsed.
     */
    override suspend fun isOurDescriptor(descriptor: String): Boolean {
        val index =
            descriptor
                .substringAfterLast("/")
                .substringBefore(")")
                .toIntOrNull() ?: return false
        val expectedDescriptor = getNextDescriptorFromIndex(accountDescriptor, index)
        return descriptor == expectedDescriptor
    }

    /**
     * Derives the descriptor for a given index by replacing the wildcard `/‍*` in
     * [accountDescriptor] with `/<index>`.
     *
     * @param accountDescriptor The wildcard account descriptor.
     * @param index The child index to substitute for the wildcard.
     * @return The descriptor for [index].
     */
    private fun getNextDescriptorFromIndex(
        accountDescriptor: String,
        index: Int,
    ): String = accountDescriptor.replace("/*", "/$index")
}
