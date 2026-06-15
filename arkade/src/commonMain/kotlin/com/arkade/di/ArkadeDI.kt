package com.arkade.di

import org.koin.dsl.koinApplication

internal object ArkadeDI {
    internal val arkadeKoin =
        koinApplication {
            modules(databaseModule, storageModule, repoModule)
        }.koin
}
