package com.arkade.repositories.intents

import com.arkade.core.intents.ArkIntent

interface IntentRepo {
    suspend fun save(intent: ArkIntent)

    suspend fun getAll(walletId: String): List<ArkIntent>

    suspend fun deleteAll(walletId: String)
}
