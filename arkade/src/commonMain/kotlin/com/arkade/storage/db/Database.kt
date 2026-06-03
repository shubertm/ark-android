package com.arkade.storage.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import com.arkade.storage.db.dao.VtxoDao
import com.arkade.storage.db.dao.WalletDao
import com.arkade.storage.db.entities.ContractEntity
import com.arkade.storage.db.entities.VtxoEntity
import com.arkade.storage.db.entities.WalletEntity
import com.arkade.utils.StringMapTypeConverter

@Database(
    entities = [WalletEntity::class, VtxoEntity::class, ContractEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(DatabaseConstructor::class)
@TypeConverters(StringMapTypeConverter::class)
abstract class Database : RoomDatabase() {
    /**
     * Provides access to the DAO responsible for wallet persistence operations.
     *
     * @return The [WalletDao] used to read and modify wallet entities in this database.
     */
    abstract fun walletDao(): WalletDao

    abstract fun vtxoDao(): VtxoDao
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
