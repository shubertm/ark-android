package com.arkade.core.wallet

import com.arkade.core.intents.ArkIntent
import com.arkade.repositories.intents.IntentRepo

interface WalletIntentManager {
    val intentRepo: IntentRepo

    suspend fun saveIntent(intent: ArkIntent)

    suspend fun getIntents(): List<ArkIntent>

    suspend fun deleteIntents()
}
