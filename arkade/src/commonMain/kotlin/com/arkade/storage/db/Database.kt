package com.arkade.storage.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.arkade.storage.db.dao.WalletDao
import com.arkade.storage.db.entities.WalletEntity

@Database(
    entities = [WalletEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(DatabaseConstructor::class)
abstract class Database : RoomDatabase() {
    /**
     * Provides access to the DAO responsible for wallet persistence operations.
     *
     * @return The [WalletDao] used to read and modify wallet entities in this database.
     */
    abstract fun walletDao(): WalletDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object DatabaseConstructor : RoomDatabaseConstructor<com.arkade.storage.db.Database> {
    /**
     * Constructs and configures the platform-specific database used by the storage layer.
     *
     * @return An initialized [com.arkade.storage.db.Database] instance.
     */
    override fun initialize(): com.arkade.storage.db.Database
}
