package com.arkade.core.wallet

import com.arkade.storage.db.Database
import com.arkade.storage.db.initializeTestDb
import kotlin.test.Test

actual abstract class WalletTest actual constructor() : com.arkade.Test() {
    actual val testDb: Database = initializeTestDb()

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
}
