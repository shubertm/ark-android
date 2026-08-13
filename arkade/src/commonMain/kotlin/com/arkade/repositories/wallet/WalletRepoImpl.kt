package com.arkade.repositories.wallet

import androidx.room.RoomDatabase
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.core.intents.ArkIntent
import com.arkade.core.vtxos.Vtxo
import com.arkade.core.wallet.Wallet
import com.arkade.di.ArkadeDI
import com.arkade.repositories.contracts.ContractRepo
import com.arkade.repositories.intents.IntentRepo
import com.arkade.repositories.vtxos.VtxoRepo
import com.arkade.storage.WalletStorage
import com.arkade.storage.db.Database
import fr.acinq.bitcoin.OutPoint
import org.koin.core.parameter.parametersOf

internal class WalletRepoImpl(
    private val databaseBuilder: RoomDatabase.Builder<Database>,
) : WalletRepo {
    private val storage: WalletStorage = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }
    override val vtxoRepo: VtxoRepo = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }
    override val contractRepo: ContractRepo = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }

    override val intentRepo: IntentRepo = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }

    /**
     * Persists the given wallet into the repository's storage.
     *
     * @param wallet The domain wallet to save.
     */
    override suspend fun saveWallet(wallet: Wallet) {
        storage.saveWallet(wallet.toRoomEntity())
    }

    /**
     * Loads a wallet by its identifier.
     *
     * @param id The wallet's unique identifier.
     * @return The corresponding `Wallet` if found, `null` otherwise.
     */
    override suspend fun loadWalletById(id: String): Wallet? {
        val entity = storage.loadWalletById(id)
        return entity?.toWallet(this)
    }

    /**
     * Loads a wallet by its `fingerprint`.
     *
     * @param fingerprint The wallet's fingerprint.
     * @return The corresponding `Wallet` if found, `null` otherwise.
     */
    override suspend fun loadWalletByFingerprint(fingerprint: String): Wallet? {
        val entity = storage.loadWalletByFingerprint(fingerprint)
        return entity?.toWallet(this)
    }

    /**
     * Load all wallets from storage and convert each stored entity into a domain Wallet.
     *
     * @return A list of [Wallet] objects; empty list if no wallets are stored.
     */
    override suspend fun loadWallets(): List<Wallet> =
        storage.loadWallets().map { entity ->
            entity.toWallet(this)
        }

    /**
     * Deletes the wallet with the specified identifier from persistent storage.
     *
     * @param id The wallet's unique identifier.
     */
    override suspend fun deleteWallet(id: String) = storage.deleteWallet(id)

    /**
     * Updates the persisted representation of an existing wallet.
     *
     * @param wallet The wallet containing updated fields to persist.
     */
    override suspend fun updateWallet(wallet: Wallet) {
        storage.updateWallet(wallet.toRoomEntity())
    }

    override suspend fun saveVtxo(vtxo: Vtxo.Data) = vtxoRepo.save(vtxo)

    override suspend fun saveVtxos(
        address: String,
        vtxos: List<Vtxo>,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun getVtxos(
        outpoints: Array<OutPoint>?,
        includeSpent: Boolean?,
    ): List<Vtxo.Data> =
        vtxoRepo.getAll(
            outpoints,
            includeSpent,
        )

    override suspend fun deleteVtxos() = vtxoRepo.deleteAll()

    /**
     * Persists an [ArkContract] by delegating to [contractRepo].
     *
     * @param contract the contract to persist.
     * @param state the lifecycle state to associate with the contract.
     * @param walletId the identifier of the wallet that owns the contract.
     * @param network the Bitcoin network used to derive the contract's `scriptPubKey`.
     */
    override suspend fun saveContract(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
        network: Network,
    ) = contractRepo.save(contract, state, walletId, network)

    /**
     * Retrieves a single [ArkContract] by its P2TR `scriptPubKey`.
     *
     * @param scriptPubKey the hex-encoded scriptPubKey identifying the contract.
     * @return the matching [ArkContract].
     * @throws IllegalArgumentException if no contract with the given [scriptPubKey] exists.
     */
    override suspend fun getContract(scriptPubKey: String): ArkContract = contractRepo.get(scriptPubKey)

    /**
     * Retrieves all contracts belonging to the specified wallets.
     *
     * @param walletIds the identifiers of the wallets whose contracts should be retrieved.
     * @return a list of [ArkContract] instances for the given [walletIds].
     */
    override suspend fun getContracts(
        walletIds: Array<String>?,
        scripts: Array<String>?,
        contractTypes: Array<String>?,
        isActive: Boolean?,
    ): List<ArkContract> =
        contractRepo.getAll(
            walletIds,
            scripts,
            contractTypes,
            isActive,
        )

    /**
     * Retrieves all contracts belonging to the specified wallet.
     *
     * @param walletId the identifier of the wallet whose contracts should be retrieved.
     * @return a list of [ArkContract] instances for the given [walletId].
     */
    override suspend fun getContracts(
        walletId: String?,
        scripts: Array<String>?,
        contractTypes: Array<String>?,
        isActive: Boolean?,
    ): List<ArkContract> =
        contractRepo.getAll(
            walletId,
            scripts,
            contractTypes,
            isActive,
        )

    /**
     * Deletes all contracts belonging to the specified wallet.
     *
     * @param walletId the identifier of the wallet whose contracts should be deleted.
     */
    override suspend fun deleteContracts(walletId: String) = contractRepo.deleteAll(walletId)

    override suspend fun deleteContracts() = contractRepo.deleteAll()

    override suspend fun saveIntent(intent: ArkIntent) = intentRepo.save(intent)

    override suspend fun getIntents(walletId: String): List<ArkIntent> = intentRepo.getAll(walletId)

    override suspend fun deleteIntents(walletId: String) = intentRepo.deleteAll(walletId)
}
