package com.arkade.di

import androidx.room.RoomDatabase
import com.arkade.core.contracts.ArkContractParserImpl
import com.arkade.repositories.ContractRepo
import com.arkade.repositories.ContractRepoImpl
import com.arkade.repositories.VtxoRepo
import com.arkade.repositories.VtxoRepoImpl
import com.arkade.repositories.WalletRepo
import com.arkade.repositories.WalletRepoImpl
import com.arkade.storage.ContractStorage
import com.arkade.storage.ContractStorageImpl
import com.arkade.storage.VtxoStorage
import com.arkade.storage.VtxoStorageImpl
import com.arkade.storage.WalletStorage
import com.arkade.storage.WalletStorageImpl
import com.arkade.storage.db.Database
import com.arkade.storage.db.getDatabase
import org.koin.dsl.module

/**
 * Koin module that provides storage layer implementations.
 *
 * Registers factory bindings for [WalletStorage], [VtxoStorage], and [ContractStorage].
 * Each factory accepts a [RoomDatabase.Builder] parameter so that multiple database instances
 * (e.g. for testing) can coexist in the same DI container.
 */
val storageModule =
    module {
        factory<WalletStorage> { params -> WalletStorageImpl(params.get()) }
        factory<VtxoStorage> { params -> VtxoStorageImpl(params.get()) }
        factory<ContractStorage> { params -> ContractStorageImpl(params.get()) }
    }

/**
 * Koin module that provides repository layer implementations.
 *
 * Registers factory bindings for [WalletRepo], [VtxoRepo], and [ContractRepo].
 * Each factory accepts a [RoomDatabase.Builder] parameter forwarded to the respective
 * storage and DAO dependencies.
 */
val repoModule =
    module {
        factory<WalletRepo> { params -> WalletRepoImpl(params.get()) }
        factory<VtxoRepo> { params -> VtxoRepoImpl(params.get()) }
        factory<ContractRepo> { params -> ContractRepoImpl(params.get()) }
    }

/**
 * Koin module that provides the Room [Database] singleton.
 *
 * Accepts a [RoomDatabase.Builder] parameter and builds the [Database] instance via
 * [getDatabase]. Registered as a `single` so that only one [Database] is created per
 * builder instance within a Koin scope.
 */
val databaseModule =
    module {
        single { params ->
            val dbBuilder: RoomDatabase.Builder<Database> = params.get()
            dbBuilder.getDatabase()
        }
    }

/**
 * Koin module that provides contract parser implementations.
 *
 * Registers [ArkContractParserImpl] as a singleton so that the parser registry
 * (and any custom parsers added to it) is shared across all consumers in the DI graph.
 */
val parsersModule =
    module {
        single { ArkContractParserImpl() }
    }
