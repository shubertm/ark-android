package com.arkade.repositories

import androidx.room.RoomDatabase
import com.arkade.core.Vtxo
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.core.wallet.Wallet
import com.arkade.di.ArkadeDI
import com.arkade.storage.WalletStorage
import com.arkade.storage.db.Database
import org.koin.core.parameter.parametersOf

internal class WalletRepoImpl(
    private val databaseBuilder: RoomDatabase.Builder<Database>,
) : WalletRepo {
    private val storage: WalletStorage = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }
    override val vtxoRepo: VtxoRepo = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }
    override val contractRepo: ContractRepo = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }

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

    override suspend fun getVtxos(): List<Vtxo.Data> = vtxoRepo.getAll()

    override suspend fun deleteVtxos() = vtxoRepo.deleteAll()

    override suspend fun saveContract(
        contract: ArkContract,
        state: ContractState,
        walletId: String,
        network: Network,
    ) = contractRepo.save(contract, state, walletId, network)

    override suspend fun getContract(scriptPubKey: String): ArkContract = contractRepo.get(scriptPubKey)

    override suspend fun getContracts(walletId: String): List<ArkContract> = contractRepo.getAll(walletId)

    override suspend fun deleteContracts(walletId: String) = contractRepo.deleteAll(walletId)
}
