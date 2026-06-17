package com.arkade.di

import org.koin.dsl.koinApplication

/**
 * Singleton object that owns the Koin application used throughout the Arkade SDK.
 *
 * [ArkadeDI] is the single source of truth for dependency resolution in Arkade. It
 * initialises a Koin application with the four core modules:
 * - [databaseModule] — Room [Database] singleton.
 * - [storageModule] — Storage layer factories ([WalletStorage], [VtxoStorage], [ContractStorage]).
 * - [repoModule] — Repository layer factories ([WalletRepo], [VtxoRepo], [ContractRepo]).
 * - [parsersModule] — Contract parser singleton ([ArkContractParserImpl]).
 *
 * All SDK components that require dependency injection should resolve bindings via [arkadeKoin].
 */
internal object ArkadeDI {
    /**
     * The Koin instance used for dependency resolution across the Arkade SDK.
     *
     * Configured with [databaseModule], [storageModule], [repoModule], and [parsersModule].
     */
    internal val arkadeKoin =
        koinApplication {
            modules(databaseModule, storageModule, repoModule, parsersModule)
        }.koin
}
