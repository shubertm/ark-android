package com.arkade.repositories

import com.arkade.core.Vtxo
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.core.wallet.Wallet
import com.arkade.core.wallet.WalletIntentManager

interface WalletRepo : WalletIntentManager {
    val vtxoRepo: VtxoRepo

    /** The [ContractRepo] used by this wallet repository for contract persistence operations. */
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

    suspend fun saveVtxo(vtxo: Vtxo.Data)

    suspend fun getVtxos(): List<Vtxo.Data>

    suspend fun deleteVtxos()

    /**
     * Persists an [ArkContract] for the given wallet.
     *
     * @param contract the contract to persist.
     * @param state the [ContractState] to associate with this contract.
     * @param walletId the identifier of the wallet that owns the contract.
     * @param network the Bitcoin network used to derive the contract's `scriptPubKey`.
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
     * @param scriptPubKey the hex-encoded scriptPubKey that identifies the contract.
     * @return the [ArkContract] matching the given [scriptPubKey].
     * @throws IllegalArgumentException if no contract with the given [scriptPubKey] exists.
     */
    suspend fun getContract(scriptPubKey: String): ArkContract

    /**
     * Retrieves all [ArkContract] instances belonging to the specified wallet.
     *
     * @param walletId the identifier of the wallet whose contracts should be retrieved.
     * @return a list of contracts associated with the given [walletId].
     */
    suspend fun getContracts(walletId: String): List<ArkContract>

    /**
     * Deletes all [ArkContract] instances belonging to the specified wallet.
     *
     * @param walletId the identifier of the wallet whose contracts should be deleted.
     */
    suspend fun deleteContracts(walletId: String)
}
