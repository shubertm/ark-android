package com.arkade.repositories

import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState

interface ContractRepo {
    suspend fun save(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
    )

    suspend fun get(scriptPubKey: String): ArkContract

    suspend fun getAll(): List<ArkContract>
}
