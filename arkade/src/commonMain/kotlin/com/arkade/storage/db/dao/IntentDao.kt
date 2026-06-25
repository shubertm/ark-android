package com.arkade.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arkade.storage.db.entities.IntentEntity

@Dao
interface IntentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(intent: IntentEntity)

    @Query("SELECT * FROM intents WHERE walletId = :walletId")
    suspend fun getAll(walletId: String): List<IntentEntity>

    @Query("DELETE FROM intents WHERE walletId = :walletId")
    suspend fun deleteAll(walletId: String)
}
