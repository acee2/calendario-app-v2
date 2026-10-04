package com.calendario.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [EventEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile private var I: AppDatabase? = null
        fun get(c: Context): AppDatabase =
            I ?: synchronized(this) {
                I ?: Room.databaseBuilder(c, AppDatabase::class.java, "calendario.db").build().also { I = it }
            }
    }
}
