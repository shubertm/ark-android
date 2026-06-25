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

    @Query("SELECT * FROM intents")
    suspend fun getAll(): List<IntentEntity>

    @Query("DELETE FROM intents")
    suspend fun deleteAll()
}
