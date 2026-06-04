package com.arkade.di

import androidx.room.RoomDatabase
import com.arkade.core.wallet.Storage
import com.arkade.core.wallet.StorageImpl
import com.arkade.repositories.VtxoRepo
import com.arkade.repositories.VtxoRepoImpl
import com.arkade.repositories.WalletRepo
import com.arkade.repositories.WalletRepoImpl
import com.arkade.storage.VtxoStorage
import com.arkade.storage.VtxoStorageImpl
import com.arkade.storage.db.Database
import com.arkade.storage.db.getDatabase
import org.koin.dsl.module

val storageModule =
    module {
        factory<Storage> { params -> StorageImpl(params.get()) }
        factory<VtxoStorage> { params -> VtxoStorageImpl(params.get()) }
    }

val repoModule =
    module {
        factory<WalletRepo> { params -> WalletRepoImpl(params.get()) }
        factory<VtxoRepo> { params -> VtxoRepoImpl(params.get()) }
    }

val databaseModule =
    module {
        single { params ->
            val dbBuilder: RoomDatabase.Builder<Database> = params.get()
            dbBuilder.getDatabase()
        }
    }
