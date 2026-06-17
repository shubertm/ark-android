package com.arkade.repositories

import com.arkade.core.Vtxo
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.core.wallet.Wallet

/**
 * Repository interface for all wallet-related persistence operations.
 *
 * [WalletRepo] is the single entry point used by [Wallet] implementations to read and write
 * wallet records, VTXOs, and Ark contracts. It composes [VtxoRepo] and [ContractRepo] so that
 * callers do not need to resolve sub-repositories directly.
 */
interface WalletRepo {
    /** Provides access to the underlying VTXO repository. */
    val vtxoRepo: VtxoRepo

    /** Provides access to the underlying contract repository. */
    val contractRepo: ContractRepo

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

    /**
     * Persists a single VTXO.
     *
     * @param vtxo The [Vtxo.Data] to save.
     */
    suspend fun saveVtxo(vtxo: Vtxo.Data)

    /**
     * Retrieves all persisted VTXOs.
     *
     * @return A list of [Vtxo.Data]; empty if none exist.
     */
    suspend fun getVtxos(): List<Vtxo.Data>

    /**
     * Deletes all persisted VTXOs.
     */
    suspend fun deleteVtxos()

    /**
     * Persists an [ArkContract] for the wallet identified by [walletId].
     *
     * @param contract  The contract to persist.
     * @param state     The [ContractState] to associate with this contract.
     * @param walletId  The ID of the owning wallet.
     * @param network   The Bitcoin network used to derive the `scriptPubKey`.
     */
    suspend fun saveContract(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
        network: Network,
    )

    /**
     * Retrieves a single [ArkContract] by its P2TR `scriptPubKey`.
     *
     * @param scriptPubKey Hex-encoded `scriptPubKey` identifying the contract.
     * @return The matching [ArkContract].
     * @throws IllegalArgumentException if no contract with the given key exists.
     */
    suspend fun getContract(scriptPubKey: String): ArkContract

    /**
     * Retrieves all [ArkContract] instances for the wallet identified by [walletId].
     *
     * @param walletId The wallet whose contracts are to be fetched.
     * @return A list of contracts; empty if none are found.
     */
    suspend fun getContracts(walletId: String): List<ArkContract>

    /**
     * Deletes all [ArkContract] instances for the wallet identified by [walletId].
     *
     * @param walletId The wallet whose contracts are to be deleted.
     */
    suspend fun deleteContracts(walletId: String)
}
