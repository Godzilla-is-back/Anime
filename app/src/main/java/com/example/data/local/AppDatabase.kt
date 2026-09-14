package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WatchlistEntity::class,
        WatchHistoryEntity::class,
        DownloadedEpisodeEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun animeDao(): AnimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                try {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "anihub_stream.db"
                    ).fallbackToDestructiveMigration(dropAllTables = true).build()
                    INSTANCE = instance
                    instance
                } catch (e: Throwable) {
                    val inMemory = Room.inMemoryDatabaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java
                    ).build()
                    INSTANCE = inMemory
                    inMemory
                }
            }
        }
    }
}
