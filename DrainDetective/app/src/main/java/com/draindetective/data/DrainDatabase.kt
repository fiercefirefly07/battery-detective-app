package com.draindetective.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [BatteryReading::class, Incident::class],
    version = 2,
    exportSchema = false
)
abstract class DrainDatabase : RoomDatabase() {
    abstract fun readingDao(): BatteryReadingDao
    abstract fun incidentDao(): IncidentDao

    companion object {
        @Volatile
        private var instance: DrainDatabase? = null

        fun getInstance(context: Context): DrainDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DrainDatabase::class.java,
                    "drain_detective.db"
                ).fallbackToDestructiveMigration(true).build().also { instance = it }
            }
        }
    }
}
