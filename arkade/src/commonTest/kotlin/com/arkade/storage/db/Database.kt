package com.arkade.storage.db

import androidx.room.RoomDatabase

/**
 * Obtain a Database builder instance configured for use in tests.
 *
 * Platform-specific implementations should provide a test-ready Database builder.
 *
 * @return A `RoomDatabase.Builder<Database>` configured for testing (isolated/test data setup).
 */
expect fun initializeTestDatabaseBuilder(): RoomDatabase.Builder<Database>
