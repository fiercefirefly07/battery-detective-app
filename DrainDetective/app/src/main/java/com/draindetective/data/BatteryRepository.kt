package com.draindetective.data

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import com.draindetective.alert.PocketCookingNotifier
import com.draindetective.blackbox.BlackBoxRecorder
import com.draindetective.domain.ContextClassifier
import com.draindetective.domain.DeviceContext
import com.draindetective.sensor.ContextSensor
import com.draindetective.service.DetectiveOnDutyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BatteryRepository(private val context: Context) {

    private val db = DrainDatabase.getInstance(context)
    private val dao = db.readingDao()
    private val incidentDao = db.incidentDao()

    private val sensor = ContextSensor(context)
    val blackBox = BlackBoxRecorder(context)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("drain_detective_prefs", Context.MODE_PRIVATE)

    val readings: Flow<List<BatteryReading>> = dao.getAll()
    val readingsAsc: Flow<List<BatteryReading>> = dao.getAllAsc()
    val incidents: Flow<List<Incident>> = incidentDao.getAll()

    var isBackgroundWorkerEnabled: Boolean
        get() = prefs.getBoolean(KEY_BG_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_BG_ENABLED, value).apply()

    var isDetectiveDutyEnabled: Boolean
        get() = prefs.getBoolean(KEY_DUTY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DUTY_ENABLED, value).apply()

    var lastBackgroundReadingTime: Long
        get() = prefs.getLong(KEY_LAST_BG_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BG_TIME, value).apply()

    /**
     * Reads battery hardware metrics and samples physical environment when sensors are accessible.
     */
    suspend fun captureReading(
        isBackground: Boolean,
        sampleSensors: Boolean
    ): BatteryReading = withContext(Dispatchers.IO) {
        val intent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (rawLevel >= 0 && scale > 0) {
            (rawLevel * 100 / scale).coerceIn(0, 100)
        } else {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
        }

        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val pluggedType = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "WIRELESS"
            else -> if (isCharging) "OTHER" else "NONE"
        }

        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val temperature = if (rawTemp > 0) rawTemp / 10.0f else 28.0f

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val screenOn = pm?.isInteractive ?: false
        val powerSaveOn = pm?.isPowerSaveMode ?: false

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkType = try {
            val activeNet = cm?.activeNetwork
            val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null
            when {
                caps == null -> "NONE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "MOBILE"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "WIFI"
                else -> "NONE"
            }
        } catch (_: Exception) {
            "NONE"
        }

        // Sensors can only be sampled when foreground or duty service is active
        val shouldSample = sampleSensors || !isBackground || DetectiveOnDutyService.isServiceActive.value
        val (proximity, lux, motion) = if (shouldSample) {
            sensor.sampleEnvironment(durationMs = 3000L)
        } else {
            com.draindetective.sensor.ContextSensorSnapshot(null, null, null)
        }

        val contextCategory = if (shouldSample) {
            ContextClassifier.classify(
                proximityCovered = proximity,
                lightLux = lux,
                motionLevel = motion,
                isCharging = isCharging,
                screenOn = screenOn
            )
        } else {
            DeviceContext.UNKNOWN
        }

        BatteryReading(
            timestamp = System.currentTimeMillis(),
            level = level,
            isCharging = isCharging,
            pluggedType = pluggedType,
            temperature = temperature,
            screenOn = screenOn,
            networkType = networkType,
            powerSaveOn = powerSaveOn,
            proximityCovered = proximity,
            lightLux = lux,
            motionLevel = motion,
            contextLabel = contextCategory.label
        )
    }

    suspend fun saveReading(
        isBackground: Boolean = false,
        sampleSensors: Boolean = false
    ): BatteryReading = withContext(Dispatchers.IO) {
        val previous = dao.getLatest()
        val reading = captureReading(isBackground, sampleSensors)
        dao.insert(reading)

        if (isBackground) {
            lastBackgroundReadingTime = reading.timestamp
        }

        // Check for Pocket Cooking warning
        if (previous != null) {
            val drop = (previous.level - reading.level).coerceAtLeast(0)
            PocketCookingNotifier.checkAndNotify(
                context = context,
                deviceContext = DeviceContext.fromLabel(reading.contextLabel),
                temperature = reading.temperature,
                recentDrop = drop
            )

            // Check for Black Box Acute Spike Incident
            blackBox.checkSpikeAndTrigger(previous, reading)
        }

        reading
    }

    fun captureCurrentReading(): BatteryReading {
        val intent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val rawLevel = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val level = if (rawLevel >= 0 && scale > 0) (rawLevel * 100 / scale).coerceIn(0, 100) else 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryReading(
            timestamp = System.currentTimeMillis(),
            level = level,
            isCharging = isCharging,
            pluggedType = if (isCharging) "AC" else "NONE",
            temperature = 28f,
            screenOn = true,
            networkType = "WIFI",
            powerSaveOn = false,
            contextLabel = DeviceContext.IN_USE.label
        )
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clear()
        incidentDao.clear()
    }

    /**
     * Seeds 3 days of rich, realistic demo data covering:
     * 1. Readings across all 6 contexts (ON_DESK, IN_POCKET_OR_BAG, MOVING, CHARGING, IN_USE, UNKNOWN)
     * 2. Pocket cooking incident (hot temperature 39.8°C + in pocket + rapid drain)
     * 3. Clear best day for Ghost Race (Day 2 is super-efficient ghost benchmark, Day 3 is live today)
     * 4. Black box incident pre-recorded with 30 high-resolution scrubbable steps!
     */
    suspend fun seedDemoData() = withContext(Dispatchers.IO) {
        dao.clear()
        incidentDao.clear()

        val now = System.currentTimeMillis()
        val intervalMs = TimeUnit.MINUTES.toMillis(15)
        val totalSteps = 4 * 24 * 3 // 3 full days = 288 points
        val startTime = now - (totalSteps * intervalMs)

        val demoList = ArrayList<BatteryReading>(totalSteps)
        var batteryLevel = 98

        for (i in 0 until totalSteps) {
            val ts = startTime + (i * intervalMs)
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = ts }
            val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
            val minute = cal.get(java.util.Calendar.MINUTE)
            val dayIndex = i / (4 * 24) // 0 = Day 1, 1 = Day 2 (Ghost Day), 2 = Day 3 (Today)

            val isOvernight = hour in 0..5
            val isWorstHour = (dayIndex == 2 && hour == 18) // Today 6-7 PM: Pocket Cooking nightmare!
            val isLunchHour = hour in 12..13
            val isCommute = (hour in 8..9) || (hour in 17..18 && dayIndex != 2)
            val isDeskWork = hour in 10..11 || hour in 14..16

            val isCharging: Boolean
            val pluggedType: String
            val screenOn: Boolean
            val networkType: String
            val temp: Float
            val powerSave: Boolean
            val proximity: Boolean?
            val lux: Float?
            val motion: Float?
            val contextLabel: String

            when {
                isOvernight -> {
                    // Charging context
                    isCharging = true
                    pluggedType = "AC"
                    screenOn = false
                    networkType = "WIFI"
                    temp = 25.5f
                    powerSave = false
                    proximity = false
                    lux = 1.0f
                    motion = 0.01f
                    contextLabel = DeviceContext.CHARGING.label
                    batteryLevel = (batteryLevel + 5).coerceAtMost(100)
                }
                isWorstHour -> {
                    // Pocket Cooking anomaly! Trapped in pocket on mobile 5G, thermal build-up
                    isCharging = false
                    pluggedType = "NONE"
                    screenOn = (minute == 0) // Accidental wake-lock
                    networkType = "MOBILE"
                    temp = 39.5f + (minute / 25f)
                    powerSave = false
                    proximity = true
                    lux = 0.2f
                    motion = 0.22f
                    contextLabel = DeviceContext.IN_POCKET_OR_BAG.label
                    batteryLevel = (batteryLevel - 3).coerceAtLeast(3)
                }
                isCommute -> {
                    // Moving context in transit
                    isCharging = false
                    pluggedType = "NONE"
                    screenOn = (minute in 15..30)
                    networkType = "MOBILE"
                    temp = 32.8f
                    powerSave = false
                    proximity = false
                    lux = 85.0f
                    motion = 0.65f
                    contextLabel = DeviceContext.MOVING.label
                    if (dayIndex == 1) {
                        // Day 2 is Ghost Day (super efficient!)
                        if (i % 2 == 0) batteryLevel = (batteryLevel - 1).coerceAtLeast(4)
                    } else {
                        batteryLevel = (batteryLevel - 1).coerceAtLeast(4)
                    }
                }
                isDeskWork -> {
                    // On Desk context
                    isCharging = false
                    pluggedType = "NONE"
                    screenOn = false
                    networkType = "WIFI"
                    temp = 28.5f
                    powerSave = false
                    proximity = false
                    lux = 45.0f
                    motion = 0.015f
                    contextLabel = DeviceContext.ON_DESK.label
                    if (dayIndex == 1) {
                        if (i % 4 == 0) batteryLevel = (batteryLevel - 1).coerceAtLeast(4)
                    } else {
                        if (i % 2 == 0) batteryLevel = (batteryLevel - 1).coerceAtLeast(4)
                    }
                }
                isLunchHour -> {
                    // In Use context
                    isCharging = false
                    pluggedType = "NONE"
                    screenOn = true
                    networkType = "WIFI"
                    temp = 33.2f
                    powerSave = false
                    proximity = false
                    lux = 120.0f
                    motion = 0.12f
                    contextLabel = DeviceContext.IN_USE.label
                    batteryLevel = (batteryLevel - 1).coerceAtLeast(4)
                }
                else -> {
                    // Regular standby or background
                    isCharging = false
                    pluggedType = "NONE"
                    screenOn = false
                    networkType = "WIFI"
                    temp = 29.0f
                    powerSave = batteryLevel < 20
                    proximity = null
                    lux = null
                    motion = null
                    contextLabel = DeviceContext.UNKNOWN.label
                    if (dayIndex == 1) {
                        if (i % 5 == 0) batteryLevel = (batteryLevel - 1).coerceAtLeast(4)
                    } else {
                        if (i % 2 == 0) batteryLevel = (batteryLevel - 1).coerceAtLeast(4)
                    }
                }
            }

            if (batteryLevel < 12 && !isOvernight && !isWorstHour) {
                batteryLevel = 50
            }

            demoList.add(
                BatteryReading(
                    id = 0,
                    timestamp = ts,
                    level = batteryLevel,
                    isCharging = isCharging,
                    pluggedType = pluggedType,
                    temperature = temp,
                    screenOn = screenOn,
                    networkType = networkType,
                    powerSaveOn = powerSave,
                    proximityCovered = proximity,
                    lightLux = lux,
                    motionLevel = motion,
                    contextLabel = contextLabel
                )
            )
        }

        dao.insertAll(demoList)

        // Seed 1 Realistic Black Box Incident for Judges to test scrubber and playback!
        val incidentStartTime = now - TimeUnit.HOURS.toMillis(4)
        val highResPoints = JSONArray()
        var simLvl = 74
        for (step in 0 until 30) {
            val stepTs = incidentStartTime + (step * 20_000L)
            if (step > 0 && step % 5 == 0) simLvl--
            val pt = JSONObject().apply {
                put("ts", stepTs)
                put("lvl", simLvl)
                put("temp", 38.2 + (step * 0.06))
                put("ctx", DeviceContext.IN_POCKET_OR_BAG.label)
                put("screen", step in 8..14)
                put("net", "MOBILE")
            }
            highResPoints.put(pt)
        }

        val demoIncident = Incident(
            id = 0,
            startTime = incidentStartTime,
            endTime = incidentStartTime + TimeUnit.MINUTES.toMillis(10),
            initialLevel = 74,
            finalLevel = 68,
            dropPercent = 6,
            peakDrainRatePerHour = 18.0f,
            dominantContext = DeviceContext.IN_POCKET_OR_BAG.label,
            avgTemperature = 39.1f,
            verdict = "Thermal runaway inside pocket: Lost 6% in 10 minutes at 39.1°C with zero ventilation and continuous 5G radio polling.",
            readingCount = 30,
            readingsJson = highResPoints.toString()
        )
        incidentDao.insert(demoIncident)
    }

    companion object {
        private const val KEY_BG_ENABLED = "key_bg_worker_enabled"
        private const val KEY_DUTY_ENABLED = "key_detective_duty_enabled"
        private const val KEY_LAST_BG_TIME = "key_last_bg_time"
    }
}
