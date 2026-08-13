package com.arkade.storage

import androidx.room.RoomDatabase
import com.arkade.core.contracts.ContractState
import com.arkade.di.ArkadeDI
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.ContractEntity
import org.koin.core.parameter.parametersOf

/**
 * Room-backed implementation of [ContractStorage].
 *
 * Resolves a [Database] instance from the Koin DI container using the provided [databaseBuilder],
 * then delegates all storage operations to [ContractDao].
 *
 * @param databaseBuilder the Room database builder used to obtain the [Database] singleton.
 */
class ContractStorageImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : ContractStorage {
    private val db = ArkadeDI.arkadeKoin.get<Database> { parametersOf(databaseBuilder) }
    private val contractDao = db.contractDao()

    override suspend fun save(contract: ContractEntity) = contractDao.save(contract)

    override suspend fun get(scriptPubKey: String): ContractEntity? = contractDao.get(scriptPubKey)

    override suspend fun getAll(
        walletIds: Array<String>?,
        scripts: Array<String>?,
        contractTypes: Array<String>?,
        isActive: Boolean?,
    ): List<ContractEntity> =
        contractDao.getAll(
            walletIds,
            scripts,
            contractTypes,
            if (isActive == true) ContractState.ACTIVE else null,
        )

    override suspend fun getAll(
        walletId: String?,
        scripts: Array<String>?,
        contractTypes: Array<String>?,
        isActive: Boolean?,
    ): List<ContractEntity> =
        contractDao.getAll(
            walletId,
            scripts,
            contractTypes,
            if (isActive == true) ContractState.ACTIVE else null,
        )

    override suspend fun deleteAll(walletId: String) = contractDao.deleteAll(walletId)

    override suspend fun deleteAll() = contractDao.deleteAll()
}
