package com.draindetective.domain

import com.draindetective.data.BatteryReading
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

data class GhostPoint(
    val minuteOfDay: Int, // 0..1439
    val level: Int
)

data class GhostRaceSummary(
    val hasEnoughData: Boolean,
    val ghostDayDate: String?,
    val ghostAverageHourlyDrop: Float,
    val currentLevelDiff: Int, // positive = ahead (e.g. +6%), negative = behind (-8%)
    val isAhead: Boolean,
    val bannerTitle: String,
    val bannerReason: String,
    val todayPoints: List<GhostPoint>,
    val ghostPoints: List<GhostPoint>
)

/**
 * Pure Kotlin analyzer for the Ghost Race feature:
 * Benchmarks today's real-time battery curve against the user's historical "best day".
 */
object GhostRaceAnalyzer {

    private const val MIN_READINGS_PER_DAY = 8

    fun analyze(
        readings: List<BatteryReading>,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): GhostRaceSummary {
        if (readings.size < MIN_READINGS_PER_DAY * 2) {
            return emptySummary()
        }

        val sorted = readings.sortedBy { it.timestamp }

        // Group readings by calendar day
        val byDay = sorted.groupBy { reading ->
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(reading.timestamp), zoneId).toLocalDate()
        }

        if (byDay.size < 2) {
            return emptySummary()
        }

        // Today is the latest day in the dataset
        val todayDate = byDay.keys.maxOrNull() ?: return emptySummary()
        val todayReadings = byDay[todayDate] ?: emptyList()

        // Historical candidate days (excluding today) with enough readings
        val pastDays = byDay.filter { (date, list) ->
            date != todayDate && list.size >= MIN_READINGS_PER_DAY
        }

        if (pastDays.isEmpty()) {
            return emptySummary()
        }

        // Find the "Ghost Day": day with the lowest active discharge rate
        var bestDate: LocalDate? = null
        var lowestDrainRate = Float.MAX_VALUE
        var bestDayPoints = emptyList<GhostPoint>()

        for ((date, dayReadings) in pastDays) {
            val daySorted = dayReadings.sortedBy { it.timestamp }
            var totalDrop = 0
            var intervals = 0

            for (i in 0 until daySorted.size - 1) {
                val a = daySorted[i]
                val b = daySorted[i + 1]
                val drop = a.level - b.level
                val wasCharging = a.isCharging || b.isCharging || a.pluggedType != "NONE"
                if (!wasCharging && drop > 0) {
                    totalDrop += drop
                    intervals++
                }
            }

            val avgDrop = if (intervals > 0) totalDrop.toFloat() / intervals else 0f
            if (intervals >= 3 && avgDrop < lowestDrainRate) {
                lowestDrainRate = avgDrop
                bestDate = date
                bestDayPoints = daySorted.map { r ->
                    val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(r.timestamp), zoneId)
                    GhostPoint(
                        minuteOfDay = (zdt.hour * 60) + zdt.minute,
                        level = r.level
                    )
                }
            }
        }

        if (bestDate == null || bestDayPoints.isEmpty()) {
            return emptySummary()
        }

        val todayPoints = todayReadings.map { r ->
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(r.timestamp), zoneId)
            GhostPoint(
                minuteOfDay = (zdt.hour * 60) + zdt.minute,
                level = r.level
            )
        }

        val latestToday = todayPoints.lastOrNull() ?: return emptySummary()
        val currentMinute = latestToday.minuteOfDay
        val currentLevel = latestToday.level

        // Find the ghost level at this same minute of day (closest point on ghost day)
        val ghostPointAtTime = bestDayPoints.minByOrNull { Math.abs(it.minuteOfDay - currentMinute) }
        val ghostLevel = ghostPointAtTime?.level ?: currentLevel
        val diff = currentLevel - ghostLevel
        val isAhead = diff >= 0

        val (bannerTitle, bannerReason) = if (isAhead) {
            val title = if (diff == 0) "Pacing exactly with your ghost" else "You're $diff% ahead of your ghost! 🏎️"
            val reason = "Superb power efficiency today! Outperforming your personal best day (${bestDate.month.name.take(3)} ${bestDate.dayOfMonth})."
            title to reason
        } else {
            val title = "You're ${-diff}% behind your ghost 🐢"

            // Analyze why today is falling behind from context data
            val screenOnCount = todayReadings.count { it.screenOn }
            val screenOnRatio = screenOnCount.toFloat() / todayReadings.size.coerceAtLeast(1)
            val pocketCount = todayReadings.count { it.contextLabel == DeviceContext.IN_POCKET_OR_BAG.label }
            val movingCount = todayReadings.count { it.contextLabel == DeviceContext.MOVING.label }
            val avgTemp = todayReadings.map { it.temperature }.average().toFloat()

            val reason = when {
                screenOnRatio >= 0.45f -> "Primary reason: High active screen-on time today (${(screenOnRatio * 100).toInt()}% of snapshots)."
                pocketCount >= 3 && avgTemp >= 37.0f -> "Primary reason: Thermal build-up (${"%.1f".format(avgTemp)}°C) inside pocket/bag."
                movingCount >= 4 -> "Primary reason: Continuous mobile cellular handovers while moving/in transit."
                avgTemp >= 38.0f -> "Primary reason: High battery thermal load (${"%.1f".format(avgTemp)}°C)."
                else -> "Primary reason: Higher background radio polling and active drain compared to your benchmark day."
            }

            title to reason
        }

        return GhostRaceSummary(
            hasEnoughData = true,
            ghostDayDate = "${bestDate.dayOfMonth} ${bestDate.month.name.take(3)}",
            ghostAverageHourlyDrop = if (lowestDrainRate == Float.MAX_VALUE) 0f else lowestDrainRate,
            currentLevelDiff = diff,
            isAhead = isAhead,
            bannerTitle = bannerTitle,
            bannerReason = bannerReason,
            todayPoints = todayPoints,
            ghostPoints = bestDayPoints
        )
    }

    private fun emptySummary() = GhostRaceSummary(
        hasEnoughData = false,
        ghostDayDate = null,
        ghostAverageHourlyDrop = 0f,
        currentLevelDiff = 0,
        isAhead = true,
        bannerTitle = "Ghost Benchmark Calibrating",
        bannerReason = "Record readings across at least two days (or tap 'Seed Demo Data' in Settings) to race today against your best battery day.",
        todayPoints = emptyList(),
        ghostPoints = emptyList()
    )
}
