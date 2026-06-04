package com.arkade.storage.db

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun initializeRoomDatabaseBuilder(appName: String): RoomDatabase.Builder<Database> {
    val appDir =
        File(System.getProperty("user.home"), ".$appName").also {
            if (it.exists() && !it.isDirectory) {
                throw IllegalArgumentException("App path exists but is not a directory: ${it.absolutePath}")
            }
            if (!it.exists() && !it.mkdirs()) {
                throw IllegalArgumentException("Failed to create app directory: ${it.absolutePath}")
            }
        }
    val dbFile = File(appDir, "$appName.db")
    return Room.databaseBuilder(
        name = dbFile.absolutePath,
    ) { DatabaseConstructor.initialize() }
}
