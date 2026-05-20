package com.arkade.storage.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Initializes an in-memory Room test database configured for JVM tests.
 *
 * The returned database is built using `DatabaseConstructor.initialize()`
 * as the Room factory, runs query coroutines on `Dispatchers.IO`, and uses `BundledSQLiteDriver()`
 * as the SQLite implementation.
 *
 * @return A configured in-memory `Database` instance suitable for testing.
 */
actual fun initializeTestDb(): Database =
    Room
        .inMemoryDatabaseBuilder<Database>(
            factory = { DatabaseConstructor.initialize() },
        ).setQueryCoroutineContext(Dispatchers.IO)
        .setDriver(BundledSQLiteDriver())
        .build()
