package com.arkade.repositories.vtxos

import androidx.room.RoomDatabase
import com.arkade.core.vtxos.Vtxo
import com.arkade.di.ArkadeDI
import com.arkade.storage.VtxoStorage
import com.arkade.storage.db.Database
import com.arkade.storage.db.entities.VtxoEntity
import fr.acinq.bitcoin.OutPoint
import org.koin.core.parameter.parametersOf

class VtxoRepoImpl(
    databaseBuilder: RoomDatabase.Builder<Database>,
) : VtxoRepo {
    private val vtxoStorage: VtxoStorage = ArkadeDI.arkadeKoin.get { parametersOf(databaseBuilder) }

    override suspend fun save(vtxo: Vtxo.Data) {
        val vtxoEntity = VtxoEntity.fromVtxo(vtxo)
        vtxoStorage.save(vtxoEntity)
    }

    override suspend fun getAll(
        outpoints: Array<OutPoint>?,
        includeSpent: Boolean?,
    ): List<Vtxo.Data> {
        val vtxoEntities =
            vtxoStorage.getAll(
                outpoints?.map { it.toString() }?.toTypedArray(),
                includeSpent,
            )
        return vtxoEntities.map { it.toVtxo() }
    }

    override suspend fun getByOutPoint(outpoint: OutPoint): List<Vtxo.Data> {
        val outpointString = outpoint.toString()
        val vtxoEntities = vtxoStorage.getByOutPoint(outpointString)
        return vtxoEntities.map { it.toVtxo() }
    }

    override suspend fun deleteAll() = vtxoStorage.deleteAll()
}
