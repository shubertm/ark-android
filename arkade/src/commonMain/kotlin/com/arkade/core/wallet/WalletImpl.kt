package com.arkade.core.wallet

import com.arkade.core.Vtxo
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.core.intents.ArkIntent
import com.arkade.repositories.WalletRepo

class WalletImpl(
    override val repo: WalletRepo,
    override val id: String,
    override val secret: String,
    override val destination: String?,
    override val type: Wallet.Type,
    override val accountDescriptor: String,
    override var lastUsedIndex: Int,
) : Wallet {
    /**
     * Persists this wallet to the configured repository.
     */
    override suspend fun save() = repo.saveWallet(this)

    /**
     * Delete this wallet from the repository.
     */
    override suspend fun delete() = repo.deleteWallet(id)

    /**
     * Persists the wallet's current state to the configured repository.
     */
    override suspend fun update() = repo.updateWallet(this)

    /**
     * Set the wallet's lastUsedIndex to `index` and attempt to persist the change.
     *
     * Validates that `index` is greater than or equal to the current `lastUsedIndex`, updates
     * the in-memory value, and calls `update()` to persist; if persistence fails, the
     * previous `lastUsedIndex` is restored.
     *
     * @param index The new last-used index; must be greater than or equal to the current value.
     * @throws IllegalArgumentException if `index` is less than the current `lastUsedIndex`.
     */
    override suspend fun updateLastUsedIndex(index: Int) {
        require(index >= lastUsedIndex) { "Invalid last used index" }
        val oldLastUsedIndex = lastUsedIndex
        lastUsedIndex = index
        runCatching { update() }.onFailure { lastUsedIndex = oldLastUsedIndex }
    }

    override suspend fun saveVtxo(vtxo: Vtxo.Data) = repo.saveVtxo(vtxo)

    override suspend fun getVtxos(): List<Vtxo.Data> = repo.getVtxos()

    override suspend fun deleteVtxos() = repo.deleteVtxos()

    /**
     * Persists [contract] for this wallet by delegating to the repository with this wallet's [id].
     *
     * @param contract the contract to persist.
     * @param state the lifecycle state to associate with the contract.
     * @param network the Bitcoin network used to derive the contract's `scriptPubKey`.
     */
    override suspend fun saveContract(
        contract: ArkContract,
        state: ContractState,
        network: Network,
    ) {
        repo.saveContract(contract, state, id, network)
    }

    /**
     * Retrieves all contracts stored for this wallet.
     *
     * @return a list of [ArkContract] instances associated with this wallet's [id].
     */
    override suspend fun getContracts() = repo.getContracts(id)

    /**
     * Deletes all contracts stored for this wallet.
     */
    override suspend fun deleteContracts() = repo.deleteContracts(id)

    override suspend fun saveIntent(intent: ArkIntent) = repo.saveIntent(intent)

    override suspend fun getIntents(): List<ArkIntent> = repo.getIntents()

    internal suspend fun deleteIntents() = repo.deleteIntents()
}
