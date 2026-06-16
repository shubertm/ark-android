package com.arkade.storage

import androidx.room.RoomDatabase
import com.arkade.di.ArkadeDI
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.ContractEntity
import org.koin.core.parameter.parametersOf

class ContractStorageImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : ContractStorage {
    private val db = ArkadeDI.arkadeKoin.get<Database> { parametersOf(databaseBuilder) }
    private val contractDao = db.contractDao()

    override suspend fun save(contract: ContractEntity) = contractDao.save(contract)

    override suspend fun get(scriptPubKey: String): ContractEntity? = contractDao.get(scriptPubKey)

    override suspend fun getAll(): List<ContractEntity> = contractDao.getAll()
}
