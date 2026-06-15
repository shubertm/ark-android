package com.arkade.storage.db

import androidx.room.RoomDatabase

/**
 * Initializes and returns a test `Database` configured for native targets.
 *
 * The returned `Database` should be suitable for use in tests (for example,
 * an isolated or in-memory instance) and is specific to the native implementation.
 *
 * @return A `Database` instance prepared for testing on native platforms.
 */
actual fun initializeTestDatabaseBuilder(): RoomDatabase.Builder<Database> {
    TODO("Not yet implemented")
}
