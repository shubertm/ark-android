package com.arkade.repositories.contracts

import com.arkade.core.contracts.ArkContract

interface ContractsRepo {
    fun getContracts(
        walletIds: Array<String> = emptyArray(),
        scripts: Array<String> = emptyArray(),
        contractTypes: Array<String> = emptyArray(),
        isActive: Boolean = false,
    ): List<ArkContract>
}
