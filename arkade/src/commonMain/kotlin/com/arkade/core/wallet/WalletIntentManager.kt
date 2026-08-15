package com.arkade.core.wallet

import com.arkade.core.intents.ArkIntent
import com.arkade.core.intents.IntentState
import com.arkade.repositories.intents.IntentRepo

interface WalletIntentManager {
    val intentRepo: IntentRepo

    suspend fun saveIntent(intent: ArkIntent)

    suspend fun getIntents(walletId: String): List<ArkIntent>

    suspend fun getIntents(
        walletId: String,
        states: Array<IntentState>,
    ): List<ArkIntent>

    suspend fun deleteIntents(walletId: String)
}
