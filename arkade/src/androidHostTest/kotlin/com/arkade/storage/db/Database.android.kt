package com.arkade.storage.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Creates an in-memory Room Database instance configured for Android host tests.
 *
 * This function obtains the Android test application context, builds an in-memory
 * Room database for the `Database` class, sets Room's query coroutine context to
 * `Dispatchers.IO`, and forces the bundled SQLite driver.
 *
 * @return A `Database` instance backed by an in-memory Room database configured
 *         for tests; queries execute on `Dispatchers.IO` and the database uses
 *         the bundled SQLite driver.
 */
actual fun initializeTestDb(): Database {
    val context =
        androidx.test.core.app.ApplicationProvider
            .getApplicationContext<Context>()
    return Room
        .inMemoryDatabaseBuilder(
            context,
            Database::class.java,
        ).setQueryCoroutineContext(Dispatchers.IO)
        .setDriver(BundledSQLiteDriver())
        .build()
}
