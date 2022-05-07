package com.example.cryptile.app_data.room_files

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SafeData::class],
    version = 1,
    exportSchema = false
)
abstract class SafeDatabase : RoomDatabase() {
    abstract fun safeDao(): SafeDao

    companion object {
        @Volatile
        private var instance: SafeDatabase? = null
        fun getDatabase(context: Context): SafeDatabase {
            /**
             * here, if the instance is not initialized, it gets initialized and then returned
             */
            return instance ?: synchronized(this) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    SafeDatabase::class.java,
                    "safe_database"
                ).fallbackToDestructiveMigration().build()
                return instance!!
            }
        }
    }
}