package com.arkade.core.wallet

import androidx.room.RoomDatabase
import com.arkade.storage.db.Database
import com.arkade.storage.db.initializeTestDatabaseBuilder
import kotlin.test.Test

actual abstract class WalletTest actual constructor() : com.arkade.Test() {
    actual val dbBuilder: RoomDatabase.Builder<Database> = initializeTestDatabaseBuilder()

    /**
     * Verifies that a wallet can be created successfully and persisted in the test database.
     *
     * Expected outcome: a new wallet is created, stored in `testDb`, and can be retrieved with the
     * expected properties.
     */
    @Test
    actual abstract fun should_create_wallet_successfully()

    /**
     * Ensures that additional wallets can be retrieved from the test database.
     *
     * Implementations should verify that requesting more wallets returns the next set of wallet
     * records and that previously loaded wallets remain available and correctly ordered.
     */
    @Test
    actual abstract fun should_load_more_wallets_successfully()

    @Test
    actual abstract fun should_store_and_retrieve_valid_vtxo_data_successfully()

    @Test
    actual abstract fun should_store_and_retrieve_valid_ark_contracts_successfully()
}
