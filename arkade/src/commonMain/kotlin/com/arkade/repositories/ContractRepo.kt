package com.arkade.repositories

import com.arkade.core.contracts.ArkContract

interface ContractRepo {
    suspend fun save(contract: ArkContract)

    suspend fun get(id: String): ArkContract

    suspend fun getAll(): List<ArkContract>
}
