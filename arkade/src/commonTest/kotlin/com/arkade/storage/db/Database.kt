package com.arkade.storage.db

/**
 * Obtain a Database instance configured for use in tests.
 *
 * Platform-specific implementations should provide a test-ready Database.
 *
 * @return A `Database` configured for testing (isolated/test data setup).
 */
expect fun initializeTestDb(): Database
