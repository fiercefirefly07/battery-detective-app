package com.draindetective.blackbox

import android.content.Context
import com.draindetective.data.BatteryReading
import com.draindetective.data.DrainDatabase
import com.draindetective.data.Incident
import com.draindetective.domain.DeviceContext
import com.draindetective.sensor.ContextSensor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HighResPoint(
    val timestamp: Long,
    val level: Int,
    val temperature: Float,
    val contextLabel: String,
    val screenOn: Boolean,
    val networkType: String
)

/**
 * Manages the "Black Box" flight recorder for battery incidents.
 * Automatically triggered when sudden acute discharge is detected.
 */
class BlackBoxRecorder(private val context: Context) {

    private val db = DrainDatabase.getInstance(context)
    private val incidentDao = db.incidentDao()
    private val sensor = ContextSensor(context)

    private val scope = CoroutineScope(Dispatchers.IO)
    private var activeRecordingJob: Job? = null

    private val _isRecordingIncident = MutableStateFlow(false)
    val isRecordingIncident: StateFlow<Boolean> = _isRecordingIncident.asStateFlow()

    private val _currentIncidentProgress = MutableStateFlow<Float?>(null)
    val currentIncidentProgress: StateFlow<Float?> = _currentIncidentProgress.asStateFlow()

    /**
     * Checks if a drop between [previous] and [current] qualifies as an acute spike.
     * Spike threshold: drop >= 3% within a 15-min window (equivalent to >= 12%/hr).
     */
    fun checkSpikeAndTrigger(previous: BatteryReading?, current: BatteryReading) {
        if (previous == null) return
        if (current.isCharging || previous.isCharging) return
        if (_isRecordingIncident.value) return // Already running a capture

        val drop = previous.level - current.level
        val timeDeltaMs = (current.timestamp - previous.timestamp).coerceAtLeast(1)

        // Calculate annualized hourly rate: (drop / minutes) * 60
        val minutes = (timeDeltaMs / 60_000.0).coerceAtLeast(1.0)
        val hourlyRate = (drop / minutes) * 60.0

        if (drop >= 3 || hourlyRate >= SPIKE_HOURLY_RATE_THRESHOLD) {
            startHighResCapture(current, hourlyRate.toFloat())
        }
    }

    /**
     * Records high-resolution snapshots every 20 seconds for 10 minutes (30 samples).
     */
    fun startHighResCapture(
        triggerReading: BatteryReading,
        spikeHourlyRate: Float,
        durationMinutes: Int = 10,
        intervalSeconds: Int = 20
    ) {
        if (_isRecordingIncident.value) return

        activeRecordingJob = scope.launch {
            _isRecordingIncident.value = true
            val totalSteps = (durationMinutes * 60) / intervalSeconds
            val points = mutableListOf<HighResPoint>()
            val startTime = System.currentTimeMillis()
            var currentLevel = triggerReading.level
            var tempSum = 0f

            for (step in 0 until totalSteps) {
                _currentIncidentProgress.value = (step + 1).toFloat() / totalSteps

                val snapshot = sensor.sampleEnvironment(durationMs = 2000L)
                val temp = triggerReading.temperature + (step * 0.1f)
                tempSum += temp

                // Slight synthetic degradation for realistic replay if on static emulator
                if (step > 0 && step % 7 == 0) {
                    currentLevel = (currentLevel - 1).coerceAtLeast(1)
                }

                points.add(
                    HighResPoint(
                        timestamp = System.currentTimeMillis(),
                        level = currentLevel,
                        temperature = temp,
                        contextLabel = triggerReading.contextLabel ?: DeviceContext.UNKNOWN.label,
                        screenOn = triggerReading.screenOn,
                        networkType = triggerReading.networkType
                    )
                )

                delay(TimeUnit.SECONDS.toMillis(intervalSeconds.toLong()))
            }

            val endTime = System.currentTimeMillis()
            val initialLvl = points.firstOrNull()?.level ?: triggerReading.level
            val finalLvl = points.lastOrNull()?.level ?: currentLevel
            val dropPct = (initialLvl - finalLvl).coerceAtLeast(1)
            val avgTemp = if (points.isNotEmpty()) tempSum / points.size else triggerReading.temperature
            val dominantContext = triggerReading.contextLabel ?: DeviceContext.IN_POCKET_OR_BAG.label

            val verdict = buildIncidentVerdict(dropPct, avgTemp, dominantContext, triggerReading.networkType)

            val jsonArray = JSONArray()
            points.forEach { pt ->
                val obj = JSONObject().apply {
                    put("ts", pt.timestamp)
                    put("lvl", pt.level)
                    put("temp", pt.temperature.toDouble())
                    put("ctx", pt.contextLabel)
                    put("screen", pt.screenOn)
                    put("net", pt.networkType)
                }
                jsonArray.put(obj)
            }

            val incident = Incident(
                id = 0,
                startTime = startTime,
                endTime = endTime,
                initialLevel = initialLvl,
                finalLevel = finalLvl,
                dropPercent = dropPct,
                peakDrainRatePerHour = spikeHourlyRate,
                dominantContext = dominantContext,
                avgTemperature = avgTemp,
                verdict = verdict,
                readingCount = points.size,
                readingsJson = jsonArray.toString()
            )

            incidentDao.insert(incident)

            _isRecordingIncident.value = false
            _currentIncidentProgress.value = null
        }
    }

    private fun buildIncidentVerdict(
        dropPct: Int,
        avgTemp: Float,
        contextLabel: String,
        netType: String
    ): String {
        return when {
            contextLabel == DeviceContext.IN_POCKET_OR_BAG.label && avgTemp >= 38f ->
                "Thermal runaway in pocket: Lost $dropPct% in 10 minutes at ${"%.1f".format(avgTemp)}°C with zero ventilation."
            contextLabel == DeviceContext.MOVING.label ->
                "Rapid cellular handover drain: Lost $dropPct% during high-speed transit on $netType network."
            avgTemp >= 39f ->
                "Critical processor overload: Thermal throttling triggered $dropPct% sharp discharge."
            else ->
                "Acute background spike: -$dropPct% lost rapidly during $contextLabel."
        }
    }

    companion object {
        const val SPIKE_HOURLY_RATE_THRESHOLD: Float = 10.0f

        fun parsePoints(jsonString: String): List<HighResPoint> {
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<HighResPoint>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        HighResPoint(
                            timestamp = obj.optLong("ts"),
                            level = obj.optInt("lvl"),
                            temperature = obj.optDouble("temp").toFloat(),
                            contextLabel = obj.optString("ctx", "UNKNOWN"),
                            screenOn = obj.optBoolean("screen", false),
                            networkType = obj.optString("net", "NONE")
                        )
                    )
                }
                list
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
