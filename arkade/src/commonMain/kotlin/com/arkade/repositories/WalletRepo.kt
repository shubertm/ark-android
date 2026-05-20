package com.arkade.repositories

import com.arkade.core.wallet.Wallet

interface WalletRepo {
    /**
     * Persists the given wallet in the repository.
     *
     * @param wallet The wallet to persist. Implementations should store the wallet
     * so it can be retrieved later.
     */
    suspend fun saveWallet(wallet: Wallet)

    /**
     * Retrieve a wallet by its string identifier.
     *
     * @param id The wallet's unique identifier.
     * @return The [Wallet] with the given identifier if it exists, `null` otherwise.
     */
    suspend fun loadWalletById(id: String): Wallet?

    /**
     * Retrieve a wallet by its `fingerprint`.
     *
     * @param fingerprint The wallet's fingerprint.
     * @return The [Wallet] with the given `fingerprint` if it exists, `null` otherwise.
     */
    suspend fun loadWalletByFingerprint(fingerprint: String): Wallet?

    /**
     * Loads all wallets available in the repository.
     *
     * @return A list of [Wallet] objects; empty if no wallets are found.
     */
    suspend fun loadWallets(): List<Wallet>

    /**
     * Deletes the wallet identified by the given `id`.
     *
     * @param id The string identifier of the wallet to remove.
     */
    suspend fun deleteWallet(id: String)

    /**
     * Updates an existing wallet using the data in the provided [Wallet].
     *
     * @param wallet [Wallet] containing updated values; its identifier is used to locate the
     * existing record to modify.
     */
    suspend fun updateWallet(wallet: Wallet)
}
