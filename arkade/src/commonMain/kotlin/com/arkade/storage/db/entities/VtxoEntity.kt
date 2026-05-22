package com.arkade.storage.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arkade.core.Vtxo
import com.arkade.core.assets.Asset
import com.arkade.core.toBlockHeight
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import fr.acinq.bitcoin.OutPoint
import fr.acinq.bitcoin.TxId
import kotlinx.serialization.json.Json

@Entity(
    tableName = "vtxos",
)
data class VtxoEntity(
    @PrimaryKey
    val outpoint: String,
    val script: String,
    val spentByTxId: String?,
    val settledByTxId: String?,
    val amount: Long,
    val createdAt: Long,
    val expiresAt: Long,
    val expiresAtHeight: Long,
    val isPreConfirmed: Boolean,
    val isUnrolled: Boolean,
    val isSwept: Boolean,
    val isSpent: Boolean,
    val commitmentTxIdsJson: String?,
    val arkTxId: String?,
    val assetsJson: String?,
) {
    fun toVtxo(): Vtxo.Data {
        val (txId, index) = outpoint.split(":")
        val outpoint = OutPoint(TxId(txId), index.toLong())
        val commitmentTxIds = commitmentTxIdsJson?.let { Json.decodeFromString<List<String>>(it) } ?: emptyList()
        val assets = assetsJson?.let { Json.decodeFromString<List<Asset>>(it) } ?: emptyList()
        return Vtxo.Data(
            outpoint,
            amount.toBigDecimal(),
            script,
            createdAt,
            expiresAt,
            isPreConfirmed,
            isSwept,
            isUnrolled,
            isSpent,
            spentByTxId,
            settledByTxId,
            arkTxId,
            commitmentTxIds,
            assets,
        )
    }

    companion object {
        fun fromVtxo(vtxo: Vtxo.Data): VtxoEntity {
            val outpoint = vtxo.outpoint.toString()
            val commitmentTxIds = Json.encodeToString(vtxo.commitmentTxIds)
            val assets = Json.encodeToString(vtxo.assets)
            return VtxoEntity(
                outpoint,
                vtxo.script,
                vtxo.spentBy,
                vtxo.settledBy,
                vtxo.amount.longValue(),
                vtxo.createdAt,
                vtxo.expiresAt,
                vtxo.expiresAt.toBlockHeight(),
                vtxo.isPreConfirmed,
                vtxo.isUnrolled,
                vtxo.isSwept,
                vtxo.isSpent,
                commitmentTxIds,
                vtxo.arkTxId,
                assets,
            )
        }
    }
}
