package com.arkade.core.wallet

import com.arkade.core.Vtxo
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.repositories.WalletRepo

/**
 * Default implementation of [Wallet].
 *
 * Delegates all persistence operations to the supplied [WalletRepo]. Contract and VTXO
 * operations are scoped to this wallet's [id] where the repository requires it.
 *
 * @param repo            The repository used for all persistence operations.
 * @param id              Unique identifier for this wallet.
 * @param secret          The wallet secret (mnemonic phrase or `nsec`-encoded key).
 * @param destination     Optional default payment destination.
 * @param type            The wallet flavour ([Wallet.Type.HD] or [Wallet.Type.SINGLE_KEY]).
 * @param accountDescriptor The Taproot account descriptor for address derivation.
 * @param lastUsedIndex   The highest address index used so far.
 */
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

    /**
     * Persists a single VTXO by delegating to the repository.
     *
     * @param vtxo The [Vtxo.Data] to save.
     */
    override suspend fun saveVtxo(vtxo: Vtxo.Data) = repo.saveVtxo(vtxo)

    /**
     * Retrieves all VTXOs from the repository.
     *
     * @return A list of [Vtxo.Data]; empty if none have been saved.
     */
    override suspend fun getVtxos(): List<Vtxo.Data> = repo.getVtxos()

    /**
     * Deletes all VTXOs from the repository.
     */
    override suspend fun deleteVtxos() = repo.deleteVtxos()

    /**
     * Persists [contract] for this wallet by delegating to the repository, scoped to [id].
     *
     * @param contract The contract to persist.
     * @param state    The [ContractState] to associate with this contract.
     * @param network  The Bitcoin network used to derive the contract's `scriptPubKey`.
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
     * @return A list of [ArkContract] instances owned by this wallet; empty if none exist.
     */
    override suspend fun getContracts() = repo.getContracts(id)

    /**
     * Deletes all contracts stored for this wallet.
     */
    override suspend fun deleteContracts() = repo.deleteContracts(id)
}
