package com.arkade.storage.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.arkade.storage.db.entities.ContractEntity

@Dao
interface ContractDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(contract: ContractEntity)

    @Query("SELECT * FROM contracts WHERE scriptPubKey = :scriptPubKey")
    suspend fun get(scriptPubKey: String): ContractEntity?

    @Query("SELECT * FROM contracts")
    suspend fun getAll(): List<ContractEntity>
}
