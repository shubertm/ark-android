package com.arkade.storage

import com.arkade.storage.db.Database
import com.arkade.storage.db.DatabaseConstructor
import com.arkade.storage.db.entities.VtxoEntity

class VtxoStorageImpl(
    testDb: Database? = null,
) : VtxoStorage {
    private val db = testDb ?: DatabaseConstructor.initialize()
    private val vtxoDao = db.vtxoDao()

    override suspend fun save(vtxo: VtxoEntity) = vtxoDao.save(vtxo)

    override suspend fun getAll(): List<VtxoEntity> = vtxoDao.getAll()

    override suspend fun getByOutPoint(outPoint: String): List<VtxoEntity> = vtxoDao.getByOutPoint(outPoint)
}
