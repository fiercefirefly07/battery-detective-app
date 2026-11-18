package com.draindetective.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a single battery snapshot and its contextual environment.
 *
 * @param id Auto-incremented primary key
 * @param timestamp Epoch milliseconds when the reading was captured
 * @param level Battery percentage (0 - 100)
 * @param isCharging True if battery status is charging or full
 * @param pluggedType Power connection: "AC", "USB", "WIRELESS", or "NONE"
 * @param temperature Battery temperature in degrees Celsius (e.g. 32.5°C)
 * @param screenOn Interactive power state (screen was on when recorded)
 * @param networkType Active network transport: "WIFI", "MOBILE", or "NONE"
 * @param powerSaveOn True if system battery saver mode was active
 * @param proximityCovered True if proximity sensor detected close object (e.g. pocket/bag)
 * @param lightLux Ambient light level in lux (nullable if sensor missing or background)
 * @param motionLevel Accelerometer magnitude variance over ~5 s sample (nullable)
 * @param contextLabel Inferred situation: "ON_DESK", "IN_POCKET_OR_BAG", "MOVING", "CHARGING", "IN_USE", "UNKNOWN"
 */
@Entity(tableName = "battery_readings")
data class BatteryReading(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val level: Int,
    val isCharging: Boolean,
    val pluggedType: String = "NONE",
    val temperature: Float = 0f,
    val screenOn: Boolean = false,
    val networkType: String = "NONE",
    val powerSaveOn: Boolean = false,
    val proximityCovered: Boolean? = null,
    val lightLux: Float? = null,
    val motionLevel: Float? = null,
    val contextLabel: String? = null
)
