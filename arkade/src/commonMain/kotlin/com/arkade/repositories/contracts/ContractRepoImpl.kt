package com.arkade.repositories.contracts

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

/**
 * Room-backed implementation of [ContractRepo].
 *
 * Obtains a [ContractStorage] from the Koin DI container using the provided [databaseBuilder],
 * and delegates persistence operations to it. Contract deserialization on retrieval is handled
 * by [ArkContractParserImpl], which maps stored type strings and additional data back to typed
 * [ArkContract] instances.
 *
 * @param databaseBuilder the Room database builder used to resolve the [ContractStorage] dependency.
 */
class ContractRepoImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : ContractRepo {
    private val storage: ContractStorage = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }
    private val contractParser: ArkContractParserImpl = ArkadeDI.arkadeKoin.get()

    /**
     * Converts [contract] to a [ContractEntity] and upserts it via [ContractStorage].
     *
     * @param contract the contract to persist.
     * @param state the lifecycle state to record.
     * @param walletId the owning wallet identifier.
     * @param network the network used to derive the contract's `scriptPubKey`.
     */
    override suspend fun save(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
        network: Network,
    ) {
        val contractEntity = ContractEntity.fromContract(contract, state, network)
        storage.save(contractEntity)
    }

    /**
     * Fetches a single [ContractEntity] by [scriptPubKey] and parses it back to an [ArkContract].
     *
     * @param scriptPubKey the hex-encoded P2TR scriptPubKey identifying the contract.
     * @return the reconstructed [ArkContract].
     * @throws IllegalArgumentException if no contract with the given [scriptPubKey] is found.
     */
    override suspend fun get(scriptPubKey: String): ArkContract {
        val contractEntity = storage.get(scriptPubKey)
        requireNotNull(contractEntity) { "Contract not found" }
        return contractParser.parse(contractEntity.additionalData, contractEntity.type, contractEntity.walletId)
    }

    /**
     * Fetches all [ContractEntity] rows for the given wallets and parses each back to an [ArkContract].
     *
     * @param walletIds the identifiers of the wallets whose contracts should be retrieved.
     * @return a non-empty list of contracts for the given wallets.
     */
    override suspend fun getAll(
        walletIds: Array<String>?,
        scripts: Array<String>?,
        contractTypes: Array<String>?,
        isActive: Boolean?,
    ): List<ArkContract> {
        val contractEntities =
            storage.getAll(
                walletIds,
                scripts,
                contractTypes,
                isActive,
            )
        return contractEntities.map {
            contractParser.parse(it.additionalData, it.type, it.walletId)
        }
    }

    /**
     * Deletes all [ContractEntity] rows for the given wallet from storage.
     *
     * @param walletId the identifier of the wallet whose contracts should be deleted.
     */
    override suspend fun deleteAll(walletId: String) = storage.deleteAll(walletId)

    override suspend fun deleteAll() = storage.deleteAll()
}
