package com.arkade.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arkade.storage.db.entities.ContractEntity

/**
 * Room DAO for [ContractEntity] persistence.
 *
 * Provides suspend functions for upserting, querying by primary key, listing contracts by wallet,
 * and deleting contracts by wallet from the `contracts` table.
 */
@Dao
interface ContractDao {
    /**
     * Upserts a [ContractEntity] into the `contracts` table.
     *
     * If a row with the same `scriptPubKey` already exists, it is replaced.
     *
     * @param contract the entity to insert or replace.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(contract: ContractEntity)

    /**
     * Returns the [ContractEntity] whose `scriptPubKey` matches [scriptPubKey], or `null` if absent.
     *
     * @param scriptPubKey the primary key to look up.
     * @return the matching entity, or `null`.
     */
    @Query("SELECT * FROM contracts WHERE scriptPubKey = :scriptPubKey")
    suspend fun get(scriptPubKey: String): ContractEntity?

    /**
     * Returns all [ContractEntity] rows in the `contracts` table belonging to [walletId].
     *
     * @param walletId the wallet identifier to filter by.
     * @return a list of [ContractEntity] instances for the given wallet, or an empty list.
     */
    @Query("SELECT * FROM contracts WHERE walletId = :walletId")
    suspend fun getAll(walletId: String): List<ContractEntity>

    /**
     * Deletes all rows from the `contracts` table belonging to [walletId].
     *
     * @param walletId the wallet identifier whose contracts should be deleted.
     */
    @Query("DELETE FROM contracts WHERE walletId = :walletId")
    suspend fun deleteAll(walletId: String)
}
