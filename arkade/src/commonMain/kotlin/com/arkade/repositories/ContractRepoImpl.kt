package com.arkade.repositories

import androidx.room.RoomDatabase
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ArkContractParserImpl
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
        network: Network,
    ) {
        val contractEntity = ContractEntity.fromContract(contract, state, walletId, network)
        storage.save(contractEntity)
    }

    override suspend fun get(scriptPubKey: String): ArkContract {
        val contractEntity = storage.get(scriptPubKey)
        requireNotNull(contractEntity) { "Contract not found" }
        return ArkContractParserImpl().parse(contractEntity.additionalData, contractEntity.type)
    }

    override suspend fun getAll(): List<ArkContract> {
        val contractEntities = storage.getAll()
        require(contractEntities.isNotEmpty()) { "No contracts found" }
        return contractEntities.map {
            ArkContractParserImpl().parse(it.additionalData, it.type)
        }
    }
}
