package com.draindetective.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

data class ContextSensorSnapshot(
    val proximityCovered: Boolean?,
    val lightLux: Float?,
    val motionLevel: Float?
)

/**
 * Manages low-power episodic sampling of hardware sensors:
 * - Proximity (TYPE_PROXIMITY)
 * - Ambient Light (TYPE_LIGHT)
 * - Accelerometer motion variance (TYPE_ACCELEROMETER)
 *
 * Samples for ~4-5 seconds and immediately unregisters all listeners to preserve battery.
 * Never crashes if hardware sensors are absent on emulators or budget devices.
 */
class ContextSensor(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val lightSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val accelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    /**
     * Samples the physical environment for [durationMs] (default 4000ms),
     * computes accelerometer magnitude variance, and unregisters immediately.
     */
    suspend fun sampleEnvironment(durationMs: Long = 4000L): ContextSensorSnapshot = withContext(Dispatchers.Default) {
        if (sensorManager == null) {
            return@withContext ContextSensorSnapshot(null, null, null)
        }

        var lastProximityCovered: Boolean? = null
        val lightReadings = mutableListOf<Float>()
        val accelMagnitudes = mutableListOf<Float>()

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                when (event.sensor.type) {
                    Sensor.TYPE_PROXIMITY -> {
                        val distance = event.values.getOrNull(0) ?: return
                        val maxRange = proximitySensor?.maximumRange ?: 5f
                        // Proximity is covered if distance is less than maximum range or near 0
                        lastProximityCovered = distance < maxRange.coerceAtLeast(1f)
                    }
                    Sensor.TYPE_LIGHT -> {
                        val lux = event.values.getOrNull(0) ?: return
                        synchronized(lightReadings) {
                            lightReadings.add(lux)
                        }
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val x = event.values.getOrNull(0) ?: 0f
                        val y = event.values.getOrNull(1) ?: 0f
                        val z = event.values.getOrNull(2) ?: 0f
                        val magnitude = sqrt((x * x) + (y * y) + (z * z))
                        synchronized(accelMagnitudes) {
                            accelMagnitudes.add(magnitude)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        try {
            // Register listeners with SENSOR_DELAY_NORMAL (low power)
            proximitySensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
            lightSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }
            accelSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL) }

            // Sample for the requested duration
            delay(durationMs)
        } catch (_: Exception) {
            // Fallback gracefully on sensor hardware issues
        } finally {
            // CRITICAL: Unregister listeners immediately to eliminate background battery drain
            try {
                sensorManager.unregisterListener(listener)
            } catch (_: Exception) {}
        }

        val avgLux = synchronized(lightReadings) {
            if (lightReadings.isNotEmpty()) lightReadings.average().toFloat() else null
        }

        val variance = synchronized(accelMagnitudes) {
            if (accelMagnitudes.size >= 3) {
                val mean = accelMagnitudes.average()
                val sumSq = accelMagnitudes.fold(0.0) { acc, v -> acc + (v - mean) * (v - mean) }
                (sumSq / accelMagnitudes.size).toFloat()
            } else {
                null
            }
        }

        ContextSensorSnapshot(
            proximityCovered = lastProximityCovered,
            lightLux = avgLux,
            motionLevel = variance
        )
    }
}
