package com.arkade.storage

import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.ContractEntity

class ContractStorageImpl(
    db: Database,
) : ContractStorage {
    private val contractDao = db.contractDao()

    override suspend fun save(contract: ContractEntity) = contractDao.save(contract)

    override suspend fun get(id: String): ContractEntity? = contractDao.get(id)

    override suspend fun getAll(): List<ContractEntity> = contractDao.getAll()
}
