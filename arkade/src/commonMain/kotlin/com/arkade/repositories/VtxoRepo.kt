package com.arkade.repositories

import com.arkade.core.Vtxo
import fr.acinq.bitcoin.OutPoint

interface VtxoRepo {
    suspend fun save(vtxo: Vtxo.Data)

    suspend fun getAll(): List<Vtxo.Data>

    suspend fun getByOutPoint(outpoint: OutPoint): List<Vtxo.Data>
}
