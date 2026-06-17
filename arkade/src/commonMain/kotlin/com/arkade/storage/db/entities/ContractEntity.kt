package com.arkade.storage.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.arkade.core.bitcoin.Network
import com.arkade.core.contracts.ArkContract
import com.arkade.core.contracts.ContractState
import com.arkade.utils.StringMapTypeConverter
import kotlin.time.Clock

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
    @TypeConverters(StringMapTypeConverter::class)
    var metadata: Map<String, String> = mapOf()

    companion object {
        fun fromContract(
            contract: ArkContract,
            state: ContractState = ContractState.ACTIVE,
            walletId: String,
            network: Network,
        ): ContractEntity =
            ContractEntity(
                contract.getScriptPubKey(network),
                contract.type,
                state,
                contract.getAdditionalData(),
                walletId,
                Clock.System.now().epochSeconds,
            )
    }
}
