package com.arkade.core.wallet

import com.arkade.storage.db.Database
import com.arkade.storage.db.DatabaseConstructor
import com.arkade.storage.db.entities.WalletEntity

internal class StorageImpl(
    testDb: Database? = null,
) : Storage {
    private val db = testDb ?: DatabaseConstructor.initialize()
    private val walletDao = db.walletDao()

    /**
     * Retrieve a wallet entity by its identifier.
     *
     * @param id The wallet's unique identifier.
     * @return The matching WalletEntity if found, `null` if no wallet exists with the given `id`.
     */
    override suspend fun loadWalletById(id: String): WalletEntity? = walletDao.loadById(id)

    /**
     * Retrieve a wallet entity by its `fingerprint`.
     *
     * @param fingerprint The wallet's fingerprint.
     * @return The matching WalletEntity if found, `null` if no wallet exists with the given `fingerprint`.
     */
    override suspend fun loadWalletByFingerprint(fingerprint: String): WalletEntity? = walletDao.loadByFingerprint(fingerprint)

    /**
     * Retrieves all persisted wallet entities.
     *
     * @return A list of `WalletEntity` objects representing all stored wallets; an empty list if
     * no wallets exist.
     */
    override suspend fun loadWallets(): List<WalletEntity> = walletDao.loadAll()

    /**
     * Persists the given wallet entity to the underlying storage.
     *
     * @param wallet The wallet entity to save.
     */
    override suspend fun saveWallet(wallet: WalletEntity) = walletDao.save(wallet)

    /**
     * Deletes the wallet with the given identifier from persistent storage.
     *
     * @param id The wallet's identifier to remove.
     */
    override suspend fun deleteWallet(id: String) = walletDao.delete(id)

    /**
     * Updates an existing wallet record in the persistent store.
     *
     * @param wallet The wallet entity containing updated fields; its `id` is used to identify
     * which record to update.
     */
    override suspend fun updateWallet(wallet: WalletEntity) = walletDao.update(wallet)
}
