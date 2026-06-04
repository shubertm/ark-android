package com.arkade.storage

import com.arkade.storage.db.entities.VtxoEntity

interface VtxoStorage {
    suspend fun save(vtxo: VtxoEntity)

    suspend fun getAll(): List<VtxoEntity>

    suspend fun getByOutPoint(outPoint: String): List<VtxoEntity>

    suspend fun deleteAll()
}
