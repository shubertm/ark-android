package com.arkade.repositories

import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState

/**
 * Repository interface for persisting and retrieving [ArkContract] instances.
 *
 * [ContractRepo] abstracts the contract storage layer, allowing callers to save and
 * retrieve contracts without depending on the underlying database implementation.
 *
 * @see ContractRepoImpl
 */
interface ContractRepo {
    /**
     * Persists an [ArkContract] to the local database.
     *
     * The contract's `scriptPubKey` (derived from [ArkContract.getScriptPubKey] for the given
     * [network]) is used as the primary key. Saving a contract with the same `scriptPubKey`
     * replaces any existing entry.
     *
     * @param contract the contract to persist.
     * @param state the [ContractState] to associate with this contract.
     * @param walletId the identifier of the wallet that owns this contract.
     * @param network the Bitcoin network used to derive the `scriptPubKey`.
     */
    suspend fun save(
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
    suspend fun get(scriptPubKey: String): ArkContract

    /**
     * Retrieves all persisted [ArkContract] instances belonging to [walletId].
     *
     * @param walletId the owning wallet's identifier.
     * @return a list of contracts owned by [walletId]; empty if none exist.
     */
    suspend fun getAll(walletId: String): List<ArkContract>

    /**
     * Deletes all persisted [ArkContract] instances belonging to [walletId].
     *
     * @param walletId the owning wallet's identifier.
     */
    suspend fun deleteAll(walletId: String)
}
