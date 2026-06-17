package com.arkade.storage

import androidx.room.RoomDatabase
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

    /**
     * Upserts [contract] by delegating to [ContractDao.save].
     *
     * @param contract the entity to insert or replace.
     */
    override suspend fun save(contract: ContractEntity) = contractDao.save(contract)

    /**
     * Returns the [ContractEntity] matching [scriptPubKey], or `null` if absent.
     *
     * @param scriptPubKey the hex-encoded P2TR scriptPubKey to look up.
     * @return the matching entity, or `null`.
     */
    override suspend fun get(scriptPubKey: String): ContractEntity? = contractDao.get(scriptPubKey)

    /**
     * Returns all contract entities belonging to [walletId].
     *
     * @param walletId the owning wallet's identifier.
     * @return a list of contract entities; empty if none exist for [walletId].
     */
    override suspend fun getAll(walletId: String): List<ContractEntity> = contractDao.getAll(walletId)

    /**
     * Deletes all contract entities belonging to [walletId].
     *
     * @param walletId the owning wallet's identifier.
     */
    override suspend fun deleteAll(walletId: String) = contractDao.deleteAll(walletId)
}
