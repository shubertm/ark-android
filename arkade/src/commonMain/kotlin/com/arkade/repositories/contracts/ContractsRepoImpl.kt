package com.arkade.repositories.contracts

import com.arkade.core.contracts.ArkContract

class ContractsRepoImpl : ContractsRepo {
    override fun getContracts(
        walletIds: Array<String>,
        scripts: Array<String>,
        contractTypes: Array<String>,
        isActive: Boolean,
    ): List<ArkContract> {
        TODO("Not yet implemented")
    }
}
