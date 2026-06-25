package com.arkade.repositories.intents

import com.arkade.core.intents.ArkIntent

interface IntentRepo {
    suspend fun save(intent: ArkIntent)

    suspend fun getAll(): List<ArkIntent>

    suspend fun deleteAll()
}
