package com.draindetective.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IncidentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(incident: Incident): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(incidents: List<Incident>)

    @Query("SELECT * FROM incidents ORDER BY startTime DESC")
    fun getAll(): Flow<List<Incident>>

    @Query("SELECT * FROM incidents WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Incident?

    @Query("DELETE FROM incidents")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM incidents")
    suspend fun count(): Int
}
