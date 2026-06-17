package com.arkade.storage

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
     * Retrieves all stored [ContractEntity] rows belonging to [walletId].
     *
     * @param walletId the owning wallet's identifier.
     * @return a list of contract entities owned by [walletId]; empty if none exist.
     */
    suspend fun getAll(walletId: String): List<ContractEntity>

    /**
     * Deletes all stored [ContractEntity] rows belonging to [walletId].
     *
     * @param walletId the owning wallet's identifier.
     */
    suspend fun deleteAll(walletId: String)
}
