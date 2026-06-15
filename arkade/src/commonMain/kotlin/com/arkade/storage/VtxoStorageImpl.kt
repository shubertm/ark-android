package com.arkade.storage

import androidx.room.RoomDatabase
import com.arkade.di.ArkadeDI
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.VtxoEntity
import org.koin.core.parameter.parametersOf

class VtxoStorageImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : VtxoStorage {
    private val db: Database = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }
    private val vtxoDao = db.vtxoDao()

    override suspend fun save(vtxo: VtxoEntity) = vtxoDao.save(vtxo)

    override suspend fun getAll(): List<VtxoEntity> = vtxoDao.getAll()

    override suspend fun getByOutPoint(outPoint: String): List<VtxoEntity> = vtxoDao.getByOutPoint(outPoint)

    override suspend fun deleteAll() = vtxoDao.deleteAll()
}
