package com.arkade.storage.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.utils.StringMapTypeConverter
import kotlin.time.Clock

/**
 * Room database entity representing a persisted [ArkContract].
 *
 * Mapped to the `contracts` table with [scriptPubKey] as the primary key.
 * The [additionalData] and [metadata] maps are serialized to JSON via [StringMapTypeConverter].
 *
 * @property scriptPubKey the hex-encoded P2TR scriptPubKey, used as the primary key.
 * @property type the contract type string (e.g. `"Boarding"`, `"generic"`).
 * @property state the current [ContractState] lifecycle state.
 * @property additionalData contract-specific key/value parameters needed for reconstruction.
 * @property walletId the identifier of the wallet that owns this contract.
 * @property createdAt the creation timestamp as Unix epoch seconds.
 */
@Entity(tableName = "contracts")
data class ContractEntity(
    @PrimaryKey
    val scriptPubKey: String,
    val type: String,
    val state: ContractState,
    @TypeConverters(StringMapTypeConverter::class)
    val additionalData: Map<String, String>,
    val walletId: String,
    val createdAt: Long,
) {
    /**
     * Mutable key/value metadata associated with this contract.
     *
     * May be used to store arbitrary runtime annotations. Persisted as JSON via [StringMapTypeConverter].
     */
    @TypeConverters(StringMapTypeConverter::class)
    var metadata: Map<String, String> = mapOf()

    companion object {
        /**
         * Creates a [ContractEntity] from an [ArkContract].
         *
         * The `scriptPubKey` is derived by calling [ArkContract.getScriptPubKey] for the given [network].
         * The `createdAt` timestamp is set to the current system clock time.
         *
         * @param contract the contract to convert.
         * @param state the [ContractState] to record; defaults to [ContractState.ACTIVE].
         * @param walletId the identifier of the owning wallet.
         * @param network the Bitcoin network used to derive the `scriptPubKey`.
         * @return a [ContractEntity] populated from the given contract.
         */
        fun fromContract(
            contract: ArkContract,
            state: ContractState = ContractState.ACTIVE,
            network: Network,
        ): ContractEntity =
            ContractEntity(
                contract.getScriptPubKey(network),
                contract.type,
                state,
                contract.getAdditionalData(),
                contract.walletId,
                Clock.System.now().epochSeconds,
            )
    }
}
