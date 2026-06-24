package com.arkade.repositories.vtxos

import com.arkade.core.vtxos.Vtxo
import fr.acinq.bitcoin.OutPoint

interface VtxoRepo {
    suspend fun save(vtxo: Vtxo.Data)

    suspend fun getAll(
        outpoints: Array<OutPoint>? = null,
        includeSpent: Boolean? = null,
    ): List<Vtxo.Data>

    suspend fun getByOutPoint(outpoint: OutPoint): List<Vtxo.Data>

    suspend fun deleteAll()
}
