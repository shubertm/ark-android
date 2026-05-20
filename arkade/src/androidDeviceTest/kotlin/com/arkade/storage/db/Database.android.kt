package com.arkade.storage.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Creates an in-memory Room `Database` instance configured for tests.
 *
 * This database is built using the test application `Context`, uses the
 * `IO` coroutine dispatcher for query execution, and is backed by the
 * bundled SQLite driver so it does not persist to disk.
 *
 * @return A `Database` instance backed by an in-memory Room database suitable for testing.
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
