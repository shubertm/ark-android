package com.arkade.storage.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun initializeRoomDatabaseBuilder(
    appName: String,
    context: Context,
): RoomDatabase.Builder<Database> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("$appName.db")
    return Room.databaseBuilder(
        context = appContext,
        name = dbFile.absolutePath,
    ) {
        DatabaseConstructor.initialize()
    }
}
