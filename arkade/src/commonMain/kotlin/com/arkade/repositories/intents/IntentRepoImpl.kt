package com.arkade.repositories.intents

import androidx.room.RoomDatabase
import com.arkade.core.intents.ArkIntent
import com.arkade.core.intents.IntentState
import com.arkade.di.ArkadeDI
import com.arkade.storage.IntentStorage
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.IntentEntity
import org.koin.core.parameter.parametersOf

class IntentRepoImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : IntentRepo {
    private val storage: IntentStorage = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }

    override var intentChanged: suspend (ArkIntent) -> Unit = {}

    override suspend fun save(intent: ArkIntent) {
        val intentEntity = IntentEntity.fromIntent(intent)
        storage.save(intentEntity)
        intentChanged(intent)
    }

    override suspend fun getAll(walletId: String): List<ArkIntent> {
        val intentEntities = storage.getAll(walletId)
        return intentEntities.map { it.toIntent() }
    }

    override suspend fun getAll(
        walletId: String,
        states: Array<IntentState>,
    ): List<ArkIntent> {
        val intentEntities = storage.getAll(walletId, states)
        return intentEntities.map { it.toIntent() }
    }

    override suspend fun deleteAll(walletId: String) = storage.deleteAll(walletId)
}
