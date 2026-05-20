package com.arkade.repositories

import com.arkade.core.wallet.Storage
import com.arkade.core.wallet.Wallet
import com.arkade.di.ArkadeDI
import com.arkade.storage.db.Database
import org.koin.core.parameter.parametersOf

internal class WalletRepoImpl(
    private val testDb: Database? = null,
) : WalletRepo {
    private val storage: Storage = ArkadeDI.arkadeKoin.get { parametersOf(testDb) }

    /**
     * Persists the given wallet into the repository's storage.
     *
     * @param wallet The domain wallet to save.
     */
    override suspend fun saveWallet(wallet: Wallet) {
        storage.saveWallet(wallet.toRoomEntity())
    }

    /**
     * Loads a wallet by its identifier.
     *
     * @param id The wallet's unique identifier.
     * @return The corresponding `Wallet` if found, `null` otherwise.
     */
    override suspend fun loadWalletById(id: String): Wallet? {
        val entity = storage.loadWalletById(id)
        return entity?.toWallet(this)
    }

    /**
     * Loads a wallet by its `fingerprint`.
     *
     * @param fingerprint The wallet's fingerprint.
     * @return The corresponding `Wallet` if found, `null` otherwise.
     */
    override suspend fun loadWalletByFingerprint(fingerprint: String): Wallet? {
        val entity = storage.loadWalletByFingerprint(fingerprint)
        return entity?.toWallet(this)
    }

    /**
     * Load all wallets from storage and convert each stored entity into a domain Wallet.
     *
     * @return A list of [Wallet] objects; empty list if no wallets are stored.
     */
    override suspend fun loadWallets(): List<Wallet> =
        storage.loadWallets().map { entity ->
            entity.toWallet(this)
        }

    /**
     * Deletes the wallet with the specified identifier from persistent storage.
     *
     * @param id The wallet's unique identifier.
     */
    override suspend fun deleteWallet(id: String) = storage.deleteWallet(id)

    /**
     * Updates the persisted representation of an existing wallet.
     *
     * @param wallet The wallet containing updated fields to persist.
     */
    override suspend fun updateWallet(wallet: Wallet) {
        storage.updateWallet(wallet.toRoomEntity())
    }
}
