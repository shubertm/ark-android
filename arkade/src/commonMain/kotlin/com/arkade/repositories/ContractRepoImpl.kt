package com.arkade.repositories

import androidx.room.RoomDatabase
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.di.ArkadeDI
import com.arkade.storage.ContractStorage
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.ContractEntity
import org.koin.core.parameter.parametersOf

class ContractRepoImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : ContractRepo {
    private val storage: ContractStorage = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }

    override suspend fun save(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
    ) {
        val contractEntity = ContractEntity.fromContract(contract, state, walletId)
        storage.save(contractEntity)
    }

    /*override suspend fun get(scriptPubKey: String): ArkContract {
        TODO("Not yet implemented")
    }

    override suspend fun getAll(): List<ArkContract> {
        TODO("Not yet implemented")
    }*/
}
