package com.arkade.storage

import com.arkade.storage.db.entities.ContractEntity

interface ContractStorage {
    suspend fun save(contract: ContractEntity)

    suspend fun get(scriptPubKey: String): ContractEntity?

    suspend fun getAll(): List<ContractEntity>
}
