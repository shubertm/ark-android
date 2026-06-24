package com.arkade.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arkade.storage.db.entities.VtxoEntity

@Dao
interface VtxoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(vtxo: VtxoEntity)

    @Query(
        "SELECT * FROM vtxos " +
            "WHERE " +
            "(:outpoints IS NULL OR outpoint IN (:outpoints)) " +
            "OR " +
            "(:includeSpent IS NULL OR isSpent = :includeSpent)",
    )
    suspend fun getAll(
        outpoints: Array<String>? = null,
        includeSpent: Boolean? = null,
    ): List<VtxoEntity>

    @Query("SELECT * FROM vtxos WHERE outpoint = :outpoint")
    suspend fun getByOutPoint(outpoint: String): List<VtxoEntity>

    @Query("DELETE FROM vtxos")
    suspend fun deleteAll()
}
