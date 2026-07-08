package com.arkade.core.wallet.addresses

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class HDAddressProvider(
    private val accountDescriptor: String,
    private val getLastUsedIndex: () -> Int,
    private val updateLastUsedIndex: (Int) -> Unit = {},
) : AddressProvider {
    private val mutex = Mutex()

    override suspend fun getNextSigningDescriptor(): String = getNextDescriptorFromIndex(accountDescriptor, getLastUsedIndex())

    override suspend fun isOurDescriptor(descriptor: String): Boolean {
        val index = descriptor.substringAfterLast("/").toIntOrNull() ?: return false
        val expectedDescriptor = getNextDescriptorFromIndex(accountDescriptor, index)
        return descriptor == expectedDescriptor
    }

    private suspend fun getNextDescriptorFromIndex(
        accountDescriptor: String,
        index: Int,
    ): String =
        mutex.withLock {
            val newIndex = index + 1
            updateLastUsedIndex(newIndex)
            accountDescriptor.replace("/*", "/$newIndex")
        }
}
