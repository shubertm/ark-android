package com.arkade.di

import com.arkade.core.wallet.Storage
import com.arkade.core.wallet.StorageImpl
import com.arkade.repositories.vtxos.VtxoRepo
import com.arkade.repositories.vtxos.VtxoRepoImpl
import com.arkade.repositories.wallet.WalletRepo
import com.arkade.repositories.wallet.WalletRepoImpl
import com.arkade.storage.VtxoStorage
import com.arkade.storage.VtxoStorageImpl
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
