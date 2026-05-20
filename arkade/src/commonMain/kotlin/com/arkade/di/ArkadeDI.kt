package com.arkade.di

import org.koin.dsl.koinApplication

internal object ArkadeDI {
    internal val arkadeKoin =
        koinApplication {
            modules(storageModule, repoModule)
        }.koin
}
