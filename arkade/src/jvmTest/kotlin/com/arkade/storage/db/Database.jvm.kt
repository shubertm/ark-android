package com.arkade.storage.db

import androidx.room.Room

/**
 * Initializes an in-memory Room test database configured for JVM tests.
 *
 * The returned database is built using `DatabaseConstructor.initialize()`
 * as the Room factory, runs query coroutines on `Dispatchers.IO`, and uses `BundledSQLiteDriver()`
 * as the SQLite implementation.
 *
 * @return A configured in-memory `Database` instance suitable for testing.
 */
actual fun initializeTestDatabaseBuilder() =
    Room
        .inMemoryDatabaseBuilder<Database>(
            factory = { DatabaseConstructor.initialize() },
        )
