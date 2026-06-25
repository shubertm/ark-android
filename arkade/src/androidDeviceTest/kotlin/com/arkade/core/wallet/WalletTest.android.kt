package com.arkade.core.wallet

import androidx.room.RoomDatabase
import com.arkade.storage.db.Database
import com.arkade.storage.db.initializeTestDatabaseBuilder
import kotlin.test.Test

actual abstract class WalletTest actual constructor() : com.arkade.Test() {
    actual val dbBuilder: RoomDatabase.Builder<Database> = initializeTestDatabaseBuilder()

    /**
     * Verifies that creating a wallet succeeds and the created wallet is persisted and retrievable.
     */
    @Test
    actual abstract fun should_create_wallet_successfully()

    /**
     * Verifies that loading additional wallets succeeds and yields the expected results.
     */
    @Test
    actual abstract fun should_load_more_wallets_successfully()

    @Test
    actual abstract fun should_store_and_retrieve_valid_vtxo_data_successfully()

    @Test
    actual abstract fun should_store_and_retrieve_valid_ark_contracts_successfully()

    @Test
    actual abstract fun should_store_and_retrieve_valid_ark_intents_successfully()
}
