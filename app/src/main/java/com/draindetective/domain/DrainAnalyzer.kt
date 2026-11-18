package com.draindetective.domain

import com.draindetective.data.BatteryReading
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class HourStat(
    val hour: Int,
    val totalDrop: Int,
    val readingCount: Int,
    val validIntervalCount: Int,
    val isSparse: Boolean,
    val explanation: String,
    val avgTemp: Float,
    val screenOnRatio: Float,
    val networkSplit: Map<String, Float>,
    val powerSaveRatio: Float,
    val chargingSampleCount: Int,
    val dominantContext: DeviceContext = DeviceContext.UNKNOWN
) {
    val screenOnMinutes: Int
        get() = (screenOnRatio * 60f).toInt().coerceIn(0, 60)
}

data class WorstHourHero(
    val hour: Int,
    val dropPercent: Int,
    val labelRange: String,
    val heroTitle: String,
    val isSparse: Boolean,
    val explanation: String
)

data class ContextDrainRate(
    val context: DeviceContext,
    val ratePerHour: Float,
    val totalDrop: Int,
    val sampleCount: Int
)

data class AnalysisSummary(
    val hourlyStats: Map<Int, HourStat>,
    val worstHour: HourStat?,
    val worstHero: WorstHourHero?,
    val averageHourlyDrop: Float,
    val estimatedHoursToEmpty: Float?,
    val totalReadings: Int,
    val contextDrainRates: List<ContextDrainRate>
)

object DrainAnalyzer {

    // Maximum interval duration to consider for continuous hourly drop (2 hours)
    private const val MAX_INTERVAL_GAP_MS = 2 * 60 * 60 * 1000L

    /**
     * Analyzes battery readings to calculate hourly drops, identify the worst drain hour,
     * exclude charging/increasing intervals, calculate context drain rates, and handle sparse data.
     */
    fun analyze(
        readings: List<BatteryReading>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): AnalysisSummary {
        if (readings.isEmpty()) {
            return emptySummary()
        }

        val sorted = readings.sortedBy { it.timestamp }

        // Hourly accumulators for 24 clock hours
        val hourlyDrops = IntArray(24)
        val validIntervals = IntArray(24)
        val readingCounts = IntArray(24)
        val tempSums = FloatArray(24)
        val screenOnCounts = IntArray(24)
        val powerSaveCounts = IntArray(24)
        val chargingCounts = IntArray(24)
        val networkCounts = Array(24) { mutableMapOf<String, Int>() }
        val hourlyContextCounts = Array(24) { mutableMapOf<DeviceContext, Int>() }

        // Context drain accumulators across the dataset
        val contextDropSums = mutableMapOf<DeviceContext, Int>()
        val contextDurationMs = mutableMapOf<DeviceContext, Long>()
        val contextOccurrences = mutableMapOf<DeviceContext, Int>()

        // First pass: count raw readings and condition context per hour
        for (r in sorted) {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(r.timestamp), zoneId)
            val h = zdt.hour

            readingCounts[h]++
            tempSums[h] += r.temperature
            if (r.screenOn) screenOnCounts[h]++
            if (r.powerSaveOn) powerSaveCounts[h]++
            if (r.isCharging || r.pluggedType != "NONE") chargingCounts[h]++

            val net = r.networkType.ifBlank { "NONE" }
            networkCounts[h][net] = (networkCounts[h][net] ?: 0) + 1

            val ctx = DeviceContext.fromLabel(r.contextLabel)
            hourlyContextCounts[h][ctx] = (hourlyContextCounts[h][ctx] ?: 0) + 1
            contextOccurrences[ctx] = (contextOccurrences[ctx] ?: 0) + 1
        }

        // Second pass: compute drops between consecutive readings
        var totalDischargeDrop = 0
        var totalDischargeHours = 0

        for (i in 0 until sorted.size - 1) {
            val a = sorted[i]
            val b = sorted[i + 1]

            val deltaMs = b.timestamp - a.timestamp
            if (deltaMs <= 0 || deltaMs > MAX_INTERVAL_GAP_MS) {
                continue
            }

            val wasCharging = a.isCharging || b.isCharging ||
                    a.pluggedType != "NONE" || b.pluggedType != "NONE"
            if (wasCharging || b.level > a.level) {
                continue
            }

            val drop = a.level - b.level
            if (drop > 0) {
                val midTs = a.timestamp + (deltaMs / 2)
                val h = ZonedDateTime.ofInstant(Instant.ofEpochMilli(midTs), zoneId).hour

                hourlyDrops[h] += drop
                validIntervals[h]++
                totalDischargeDrop += drop

                // Attribute drop to the interval's physical context
                val intervalContext = DeviceContext.fromLabel(b.contextLabel ?: a.contextLabel)
                contextDropSums[intervalContext] = (contextDropSums[intervalContext] ?: 0) + drop
                contextDurationMs[intervalContext] = (contextDurationMs[intervalContext] ?: 0L) + deltaMs
            }
        }

        // Build HourStat for each hour (0..23)
        val hourlyMap = LinkedHashMap<Int, HourStat>(24)
        for (h in 0..23) {
            val count = readingCounts[h]
            val intervals = validIntervals[h]
            val drop = hourlyDrops[h]

            val isSparse = count <= 1 || (count > 1 && intervals == 0 && chargingCounts[h] == 0)
            val explanation = when {
                count == 0 -> "No readings recorded during this hour."
                count == 1 -> "Only 1 reading recorded. At least 2 consecutive discharge readings are needed to measure drop."
                chargingCounts[h] >= count -> "Device was charging throughout this hour; discharge drops excluded."
                intervals == 0 && drop == 0 -> "Battery level remained steady (0% drop) across recorded readings."
                else -> "Recorded $count readings with $intervals active discharge intervals."
            }

            val avgTemp = if (count > 0) tempSums[h] / count else 0f
            val screenRatio = if (count > 0) screenOnCounts[h].toFloat() / count else 0f
            val powerSaveRatio = if (count > 0) powerSaveCounts[h].toFloat() / count else 0f

            val netMap = mutableMapOf<String, Float>()
            if (count > 0) {
                networkCounts[h].forEach { (net, n) ->
                    netMap[net] = n.toFloat() / count
                }
            }

            val dominant = hourlyContextCounts[h].maxByOrNull { it.value }?.key ?: DeviceContext.UNKNOWN

            hourlyMap[h] = HourStat(
                hour = h,
                totalDrop = drop,
                readingCount = count,
                validIntervalCount = intervals,
                isSparse = isSparse,
                explanation = explanation,
                avgTemp = avgTemp,
                screenOnRatio = screenRatio,
                networkSplit = netMap,
                powerSaveRatio = powerSaveRatio,
                chargingSampleCount = chargingCounts[h],
                dominantContext = dominant
            )

            if (intervals > 0 && drop > 0) {
                totalDischargeHours++
            }
        }

        val nonZeroHours = hourlyMap.values.filter { it.totalDrop > 0 && !it.isSparse }
        val worst = nonZeroHours.maxWithOrNull(
            compareBy<HourStat> { it.totalDrop }
                .thenBy { it.validIntervalCount }
                .thenBy { it.hour }
        )

        val avgHourlyDrop = if (totalDischargeHours > 0) {
            totalDischargeDrop.toFloat() / totalDischargeHours
        } else {
            0f
        }

        val latest = sorted.last()
        val estimatedHours: Float? = if (!latest.isCharging && avgHourlyDrop > 0.1f) {
            (latest.level.toFloat() / avgHourlyDrop).coerceIn(0.1f, 168f)
        } else {
            null
        }

        val worstHero: WorstHourHero? = worst?.let {
            val label = formatHourRange(it.hour)
            WorstHourHero(
                hour = it.hour,
                dropPercent = it.totalDrop,
                labelRange = label,
                heroTitle = "Worst drain hour: $label, -${it.totalDrop}%",
                isSparse = false,
                explanation = "Lost ${it.totalDrop}% battery during this period (${it.dominantContext.displayName})."
            )
        }

        // Build context drain rates list
        val contextRateList = DeviceContext.entries
            .filter { it != DeviceContext.CHARGING }
            .map { ctx ->
                val drop = contextDropSums[ctx] ?: 0
                val durationMs = contextDurationMs[ctx] ?: 0L
                val hours = durationMs / (1000.0 * 3600.0)
                val rate = if (hours > 0.1) (drop / hours).toFloat() else if (drop > 0) drop.toFloat() else 0f
                ContextDrainRate(
                    context = ctx,
                    ratePerHour = rate,
                    totalDrop = drop,
                    sampleCount = contextOccurrences[ctx] ?: 0
                )
            }
            .sortedByDescending { it.ratePerHour }

        return AnalysisSummary(
            hourlyStats = hourlyMap,
            worstHour = worst,
            worstHero = worstHero,
            averageHourlyDrop = avgHourlyDrop,
            estimatedHoursToEmpty = estimatedHours,
            totalReadings = sorted.size,
            contextDrainRates = contextRateList
        )
    }

    fun formatHourRange(h: Int): String {
        fun formatSingle(hr: Int): String {
            val clamped = ((hr % 24) + 24) % 24
            return when {
                clamped == 0 -> "12 AM"
                clamped < 12 -> "$clamped AM"
                clamped == 12 -> "12 PM"
                else -> "${clamped - 12} PM"
            }
        }

        fun shortNumber(hr: Int): String {
            val clamped = ((hr % 24) + 24) % 24
            return when {
                clamped == 0 -> "12"
                clamped <= 12 -> "$clamped"
                else -> "${clamped - 12}"
            }
        }

        val startClamped = ((h % 24) + 24) % 24
        val endClamped = (startClamped + 1) % 24

        val startAmPm = if (startClamped < 12) "AM" else "PM"
        val endAmPm = if (endClamped < 12) "AM" else "PM"

        return if (startAmPm == endAmPm) {
            "${shortNumber(startClamped)}-${shortNumber(endClamped)} $endAmPm"
        } else {
            "${formatSingle(startClamped)}-${formatSingle(endClamped)}"
        }
    }

    private fun emptySummary(): AnalysisSummary {
        val emptyMap = (0..23).associateWith { h ->
            HourStat(
                hour = h,
                totalDrop = 0,
                readingCount = 0,
                validIntervalCount = 0,
                isSparse = true,
                explanation = "No readings recorded yet.",
                avgTemp = 0f,
                screenOnRatio = 0f,
                networkSplit = emptyMap(),
                powerSaveRatio = 0f,
                chargingSampleCount = 0,
                dominantContext = DeviceContext.UNKNOWN
            )
        }
        return AnalysisSummary(
            hourlyStats = emptyMap,
            worstHour = null,
            worstHero = null,
            averageHourlyDrop = 0f,
            estimatedHoursToEmpty = null,
            totalReadings = 0,
            contextDrainRates = emptyList()
        )
    }
}
