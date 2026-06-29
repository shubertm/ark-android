package com.arkade.storage

import com.arkade.storage.db.entities.IntentEntity

interface IntentStorage {
    suspend fun save(intent: IntentEntity)

    suspend fun getAll(walletId: String): List<IntentEntity>

    suspend fun deleteAll(walletId: String)
}
