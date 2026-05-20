package com.arkade.core.wallet

import com.arkade.storage.db.entities.WalletEntity

interface Storage {
    /**
     * Loads the wallet with the given identifier.
     *
     * @param id The wallet's identifier.
     * @return [WalletEntity] if a wallet with the specified `id` exists, `null` otherwise.
     */
    suspend fun loadWalletById(id: String): WalletEntity?

    /**
     * Loads the wallet with the given fingerprint.
     *
     * @param fingerprint The wallet's fingerprint.
     * @return [WalletEntity] if a wallet with the specified `fingerprint` exists, `null` otherwise.
     */
    suspend fun loadWalletByFingerprint(fingerprint: String): WalletEntity?

    /**
     * Loads all persisted wallets.
     *
     * @return A list of stored [WalletEntity] objects; an empty list if no wallets are found.
     */
    suspend fun loadWallets(): List<WalletEntity>

    /**
     * Persists the given wallet entity into storage.
     *
     * @param wallet The [WalletEntity] to be saved.
     */
    suspend fun saveWallet(wallet: WalletEntity)

    /**
     * Deletes the persisted wallet with the specified id.
     *
     * @param id The identifier of the wallet to delete.
     */
    suspend fun deleteWallet(id: String)

    /**
     * Updates an existing wallet record with the provided wallet data.
     *
     * @param wallet The [WalletEntity] whose fields will replace the stored record identified by its `id`.
     */
    suspend fun updateWallet(wallet: WalletEntity)
}
