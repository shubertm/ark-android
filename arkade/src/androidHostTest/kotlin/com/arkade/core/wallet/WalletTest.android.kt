package com.arkade.core.wallet

import androidx.room.RoomDatabase
import com.arkade.storage.db.Database
import com.arkade.storage.db.initializeTestDatabaseBuilder
import kotlin.test.Test

actual abstract class WalletTest actual constructor() : com.arkade.Test() {
    actual val dbBuilder: RoomDatabase.Builder<Database> = initializeTestDatabaseBuilder()

    /**
     * Verifies that a wallet can be created successfully and stored in the test database.
     *
     * Implementations should assert that the created wallet meets expected properties and is
     * persisted, so it can be retrieved by subsequent operations.
     */
    @Test
    actual abstract fun should_create_wallet_successfully()

    /**
     * Verifies that additional wallets can be loaded successfully.
     *
     * Implementations must assert that requesting more wallet entries returns the expected results
     * and updates any pagination or in-memory state accordingly.
     */
    @Test
    actual abstract fun should_load_more_wallets_successfully()

    @Test
    actual abstract fun should_store_and_retrieve_valid_vtxo_data_successfully()
}
