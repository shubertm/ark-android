package com.arkade.storage

import com.arkade.core.intents.IntentState
import com.arkade.storage.db.entities.IntentEntity

interface IntentStorage {
    suspend fun save(intent: IntentEntity)

    suspend fun getAll(walletId: String): List<IntentEntity>

    suspend fun getAll(
        walletId: String,
        states: Array<IntentState>,
    ): List<IntentEntity>

    suspend fun deleteAll(walletId: String)
}
