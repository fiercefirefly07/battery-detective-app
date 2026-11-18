package com.draindetective.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a Black Box high-resolution incident capture.
 * Triggered automatically when an acute battery drop rate spike is detected.
 */
@Entity(tableName = "incidents")
data class Incident(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val initialLevel: Int,
    val finalLevel: Int,
    val dropPercent: Int,
    val peakDrainRatePerHour: Float,
    val dominantContext: String,
    val avgTemperature: Float,
    val verdict: String,
    val readingCount: Int,
    val readingsJson: String
)
