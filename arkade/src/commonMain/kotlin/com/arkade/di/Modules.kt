package com.arkade.di

import androidx.room.RoomDatabase
import com.arkade.core.contracts.ArkContractParserImpl
import com.arkade.repositories.contracts.ContractRepo
import com.arkade.repositories.contracts.ContractRepoImpl
import com.arkade.repositories.vtxos.VtxoRepo
import com.arkade.repositories.vtxos.VtxoRepoImpl
import com.arkade.repositories.wallet.WalletRepo
import com.arkade.repositories.wallet.WalletRepoImpl
import com.arkade.storage.ContractStorage
import com.arkade.storage.ContractStorageImpl
import com.arkade.storage.VtxoStorage
import com.arkade.storage.VtxoStorageImpl
import com.arkade.storage.WalletStorage
import com.arkade.storage.WalletStorageImpl
import com.arkade.storage.db.Database
import com.arkade.storage.db.getDatabase
import org.koin.dsl.module

val storageModule =
    module {
        factory<WalletStorage> { params -> WalletStorageImpl(params.get()) }
        factory<VtxoStorage> { params -> VtxoStorageImpl(params.get()) }
        factory<ContractStorage> { params -> ContractStorageImpl(params.get()) }
    }

val repoModule =
    module {
        factory<WalletRepo> { params -> WalletRepoImpl(params.get()) }
        factory<VtxoRepo> { params -> VtxoRepoImpl(params.get()) }
        factory<ContractRepo> { params -> ContractRepoImpl(params.get()) }
    }

val databaseModule =
    module {
        single { params ->
            val dbBuilder: RoomDatabase.Builder<Database> = params.get()
            dbBuilder.getDatabase()
        }
    }

/**
 * Koin module that registers contract parser singletons.
 *
 * Provides a single [ArkContractParserImpl] instance shared across the application.
 */
val parsersModule =
    module {
        single { ArkContractParserImpl() }
    }
