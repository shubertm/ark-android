package com.arkade.core.wallet

import com.arkade.repositories.WalletRepo

class WalletImpl(
    override val repo: WalletRepo,
    override val id: String,
    override val secret: String,
    override val destination: String?,
    override val type: Wallet.Type,
    override val accountDescriptor: String,
    override var lastUsedIndex: Int,
) : Wallet {
    /**
     * Persists this wallet to the configured repository.
     */
    override suspend fun save() = repo.saveWallet(this)

    /**
     * Delete this wallet from the repository.
     */
    override suspend fun delete() = repo.deleteWallet(id)

    /**
     * Persists the wallet's current state to the configured repository.
     */
    override suspend fun update() = repo.updateWallet(this)

    /**
     * Set the wallet's lastUsedIndex to `index` and attempt to persist the change.
     *
     * Validates that `index` is greater than or equal to the current `lastUsedIndex`, updates
     * the in-memory value, and calls `update()` to persist; if persistence fails, the
     * previous `lastUsedIndex` is restored.
     *
     * @param index The new last-used index; must be greater than or equal to the current value.
     * @throws IllegalArgumentException if `index` is less than the current `lastUsedIndex`.
     */
    override suspend fun updateLastUsedIndex(index: Int) {
        require(index >= lastUsedIndex) { "Invalid last used index" }
        val oldLastUsedIndex = lastUsedIndex
        lastUsedIndex = index
        runCatching { update() }.onFailure { lastUsedIndex = oldLastUsedIndex }
    }
}
