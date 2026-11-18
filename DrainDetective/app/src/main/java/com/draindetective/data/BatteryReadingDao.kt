package com.draindetective.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryReadingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reading: BatteryReading): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(readings: List<BatteryReading>)

    @Query("SELECT * FROM battery_readings ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BatteryReading>>

    @Query("SELECT * FROM battery_readings ORDER BY timestamp ASC")
    fun getAllAsc(): Flow<List<BatteryReading>>

    @Query("SELECT * FROM battery_readings ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): BatteryReading?

    @Query("SELECT COUNT(*) FROM battery_readings")
    suspend fun count(): Int

    @Query("DELETE FROM battery_readings")
    suspend fun clear()
}
