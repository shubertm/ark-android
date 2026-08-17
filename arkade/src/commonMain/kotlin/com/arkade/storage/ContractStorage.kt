package com.arkade.storage

import com.arkade.core.contracts.ContractState
import com.arkade.storage.db.entities.ContractEntity

/**
 * Storage interface for [ContractEntity] persistence operations.
 *
 * [ContractStorage] abstracts DAO access, allowing higher layers such as [ContractRepo]
 * to operate on contracts without depending directly on Room.
 *
 * @see ContractStorageImpl
 */
interface ContractStorage {
    /**
     * Upserts a [ContractEntity] into the database.
     *
     * If a contract with the same `scriptPubKey` already exists, it is replaced.
     *
     * @param contract the entity to save or update.
     */
    suspend fun save(contract: ContractEntity)

    /**
     * Retrieves a single [ContractEntity] by its primary key.
     *
     * @param scriptPubKey the hex-encoded P2TR scriptPubKey identifying the contract.
     * @return the matching [ContractEntity], or `null` if none exists.
     */
    suspend fun get(scriptPubKey: String): ContractEntity?

    /**
     * Retrieves all stored [ContractEntity] rows for the given wallet.
     *
     * @param walletIds the identifiers of the wallets whose contracts should be retrieved.
     * @return a list of persisted contract entities for [walletIds], or an empty list if none exist.
     */
    suspend fun getAll(
        walletIds: Array<String>? = null,
        scripts: Array<String>? = null,
        contractTypes: Array<String>? = null,
        state: ContractState? = null,
    ): List<ContractEntity>

    /**
     * Retrieves all stored [ContractEntity] rows for the given wallet.
     *
     * @param walletId the identifier of the wallet whose contracts should be retrieved.
     * @return a list of persisted contract entities for [walletId], or an empty list if none exist.
     */
    suspend fun getAll(
        walletId: String? = null,
        scripts: Array<String>? = null,
        contractTypes: Array<String>? = null,
        state: ContractState? = null,
    ): List<ContractEntity>

    /**
     * Deletes all stored [ContractEntity] rows for the given wallet.
     *
     * @param walletId the identifier of the wallet whose contracts should be deleted.
     */
    suspend fun deleteAll(walletId: String)

    suspend fun deleteAll()
}
