package com.arkade.core.wallet.addresses

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HDAddressProvider(
    private val accountDescriptor: String,
    private val getLastUsedIndex: () -> Int,
    private val updateLastUsedIndex: (Int) -> Unit = {},
) : AddressProvider {
    private val mutex = Mutex()

    override suspend fun getNextSigningDescriptor(): String =
        mutex.withLock {
            val newIndex = getLastUsedIndex() + 1
            updateLastUsedIndex(newIndex)
            getNextDescriptorFromIndex(accountDescriptor, newIndex)
        }

    override suspend fun isOurDescriptor(descriptor: String): Boolean {
        val index =
            descriptor
                .substringAfterLast("/")
                .substringBefore(")")
                .toIntOrNull() ?: return false
        val expectedDescriptor = getNextDescriptorFromIndex(accountDescriptor, index)
        return descriptor == expectedDescriptor
    }

    private fun getNextDescriptorFromIndex(
        accountDescriptor: String,
        index: Int,
    ): String = accountDescriptor.replace("/*", "/$index")
}
