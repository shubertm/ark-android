package com.arkade.storage.db

import androidx.room.RoomDatabase

/**
 * Obtain a Database instance configured for use in tests.
 *
 * Platform-specific implementations should provide a test-ready Database.
 *
 * @return A `Database` configured for testing (isolated/test data setup).
 */
expect fun initializeTestDatabaseBuilder(): RoomDatabase.Builder<Database>
