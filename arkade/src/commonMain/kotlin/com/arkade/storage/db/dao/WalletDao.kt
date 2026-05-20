package com.arkade.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.arkade.storage.db.entities.WalletEntity

@Dao
interface WalletDao {
    /**
     * Inserts the given wallet into the `wallets` table, replacing any existing row that has the
     * same primary key.
     *
     * @param wallet The [WalletEntity] to insert; if a row with the same primary key exists it
     * will be replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(wallet: WalletEntity)

    /**
     * Loads a wallet by its id.
     *
     * @param id The wallet's unique identifier.
     * @return The matching [WalletEntity] if found, `null` otherwise.
     */
    @Query("SELECT * FROM wallets WHERE id = :id")
    suspend fun loadById(id: String): WalletEntity?

    /**
     * Loads a wallet by its fingerprint.
     *
     * @param fingerprint The wallet's fingerprint.
     * @return The matching [WalletEntity] if found, `null` otherwise.
     */
    @Query("SELECT * FROM wallets WHERE fingerprint = :fingerprint")
    suspend fun loadByFingerprint(fingerprint: String): WalletEntity?

    /**
     * Retrieves all wallet records from the database.
     *
     * @return A list of [WalletEntity] containing every row from the `wallets` table; an empty
     * list if no rows exist.
     */
    @Query("SELECT * FROM wallets")
    suspend fun loadAll(): List<WalletEntity>

    /**
     * Deletes the wallet row with the given id from the `wallets` table.
     *
     * @param id The wallet's primary key; if no row matches this id the operation is a no-op.
     */
    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun delete(id: String)

    /**
     * Updates an existing row in the `wallets` table using the entity's primary key.
     *
     * @param wallet The [WalletEntity] containing updated values; the row with the same primary
     * key will be replaced if present.
     */
    @Update
    suspend fun update(wallet: WalletEntity)
}
