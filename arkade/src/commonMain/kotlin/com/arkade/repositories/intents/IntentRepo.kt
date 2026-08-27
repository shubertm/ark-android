package com.arkade.repositories.intents

import com.arkade.core.intents.ArkIntent
import com.arkade.core.intents.IntentState

interface IntentRepo {
    var intentChanged: suspend (intent: ArkIntent) -> Unit

    suspend fun save(intent: ArkIntent)

    suspend fun getAll(walletId: String): List<ArkIntent>

    suspend fun getAll(
        walletId: String,
        states: Array<IntentState>,
    ): List<ArkIntent>

    suspend fun deleteAll(walletId: String)
}
