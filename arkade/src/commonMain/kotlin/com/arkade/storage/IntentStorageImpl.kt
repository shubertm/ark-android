package com.arkade.storage

import androidx.room.RoomDatabase
import com.arkade.di.ArkadeDI
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.IntentEntity
import org.koin.core.parameter.parametersOf

class IntentStorageImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : IntentStorage {
    private val db: Database = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }
    private val intentDao = db.intentDao()

    override suspend fun save(intent: IntentEntity) = intentDao.save(intent)

    override suspend fun getAll(): List<IntentEntity> = intentDao.getAll()

    override suspend fun deleteAll() = intentDao.deleteAll()
}
