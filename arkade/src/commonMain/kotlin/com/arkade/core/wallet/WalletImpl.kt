package com.arkade.core.wallet

import com.arkade.core.Vtxo
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.core.intents.ArkIntent
import com.arkade.core.wallet.addresses.AddressProvider
import com.arkade.core.wallet.addresses.HDAddressProvider
import com.arkade.core.wallet.addresses.SingleKeyAddressProvider
import com.arkade.core.wallet.signer.HDSigner
import com.arkade.core.wallet.signer.Signer
import com.arkade.core.wallet.signer.SingleKeySigner
import com.arkade.repositories.WalletRepo
import fr.acinq.bitcoin.Transaction
import fr.acinq.bitcoin.psbt.Psbt

class WalletImpl(
    override val repo: WalletRepo,
    override val id: String,
    override val secret: String,
    override val destination: String?,
    override val type: Wallet.Type,
    override val accountDescriptor: String,
    override var lastUsedIndex: Int,
    override val network: Network,
) : Wallet {
    override val signer: Signer =
        when (type) {
            Wallet.Type.SINGLE_KEY -> SingleKeySigner.fromNSec(secret)
            Wallet.Type.HD -> HDSigner.fromMnemonic(secret, network)
        }
    private val addressProvider: AddressProvider =
        when (type) {
            Wallet.Type.SINGLE_KEY -> SingleKeyAddressProvider(accountDescriptor)
            Wallet.Type.HD ->
                HDAddressProvider(
                    accountDescriptor,
                    getLastUsedIndex = { lastUsedIndex },
                    updateLastUsedIndex = { updateLastUsedIndex(it) },
                )
        }

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
        runCatching { update() }.onFailure {
            if (lastUsedIndex == index) lastUsedIndex = oldLastUsedIndex
        }
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

    override suspend fun saveIntent(intent: ArkIntent) {
        require(intent.walletId == id) { "Wallet should own intent" }
        repo.saveIntent(intent)
    }

    override suspend fun getIntents(): List<ArkIntent> = repo.getIntents(id)

    override suspend fun deleteIntents() = repo.deleteIntents(id)

    override suspend fun sign(
        descriptor: String,
        psbt: Psbt,
        inputIndexes: Array<Int>,
    ): Transaction = signer.sign(descriptor, psbt, inputIndexes)

    override suspend fun signMessage(
        descriptor: String,
        message: ByteArray,
    ): ByteArray = signer.signMessage(descriptor, message)
}
