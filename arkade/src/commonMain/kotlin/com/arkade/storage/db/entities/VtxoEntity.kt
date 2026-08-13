package com.arkade.storage.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.arkade.core.assets.Asset
import com.arkade.core.toBlockHeight
import com.arkade.core.vtxos.Vtxo
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
        return Vtxo.Data.normalized(
            outpoint = outpoint,
            amount = amount.toBigDecimal(),
            script = script,
            createdAt = createdAt,
            expiresAt = expiresAt,
            expiresAtHeight = expiresAtHeight,
            isPreConfirmed = isPreConfirmed,
            isSwept = isSwept,
            isUnrolled = isUnrolled,
            isSpent = isSpent,
            spentBy = spentByTxId,
            settledBy = settledByTxId,
            arkTxId = arkTxId,
            commitmentTxIds = commitmentTxIds,
            assets = assets,
        )
    }

    companion object {
        fun fromVtxo(vtxo: Vtxo.Data): VtxoEntity {
            val outpoint = vtxo.outpoint.toString()
            val commitmentTxIds = Json.encodeToString(vtxo.commitmentTxIds)
            val assets = Json.encodeToString(vtxo.assets)
            return VtxoEntity(
                outpoint = outpoint,
                script = vtxo.script,
                spentByTxId = vtxo.spentBy,
                settledByTxId = vtxo.settledBy,
                amount = vtxo.amount.longValue(),
                createdAt = vtxo.createdAt,
                expiresAt = vtxo.expiresAt,
                expiresAtHeight = vtxo.expiresAt.toBlockHeight(),
                isPreConfirmed = vtxo.isPreConfirmed,
                isUnrolled = vtxo.isUnrolled,
                isSwept = vtxo.isSwept,
                isSpent = vtxo.isSpent,
                commitmentTxIdsJson = commitmentTxIds,
                arkTxId = vtxo.arkTxId,
                assetsJson = assets,
            )
        }
    }
}
