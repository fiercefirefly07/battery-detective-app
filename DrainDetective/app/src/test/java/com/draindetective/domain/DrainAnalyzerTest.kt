package com.draindetective.domain

import com.draindetective.data.BatteryReading
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DrainAnalyzerTest {

    private val testZone: ZoneId = ZoneId.of("UTC")

    private fun makeTs(hour: Int, minute: Int): Long {
        return ZonedDateTime.of(2026, 11, 18, hour, minute, 0, 0, testZone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun testNormalDischarge() {
        // Hour 14: readings at 14:00 (100%), 14:15 (96%), 14:30 (92%), 14:45 (88%), 15:00 (86%)
        // Total drops in hour 14 should sum to 12% (14:00 -> 14:45) + (14:45 -> 15:00 is midpoint 14:52 = 2%) = 14%
        val readings = listOf(
            BatteryReading(1, makeTs(14, 0), 100, isCharging = false, pluggedType = "NONE"),
            BatteryReading(2, makeTs(14, 15), 96, isCharging = false, pluggedType = "NONE"),
            BatteryReading(3, makeTs(14, 30), 92, isCharging = false, pluggedType = "NONE"),
            BatteryReading(4, makeTs(14, 45), 88, isCharging = false, pluggedType = "NONE"),
            BatteryReading(5, makeTs(15, 0), 86, isCharging = false, pluggedType = "NONE")
        )

        val summary = DrainAnalyzer.analyze(readings, testZone)

        assertNotNull(summary.worstHour)
        assertEquals(14, summary.worstHour?.hour)
        assertEquals(14, summary.worstHour?.totalDrop)
        assertFalse(summary.worstHour!!.isSparse)
        assertEquals("Worst drain hour: 2-3 PM, -14%", summary.worstHero?.heroTitle)
    }

    @Test
    fun testChargingIntervalExcluded() {
        // Hour 15: discharging from 86 to 82, then plugged into charger (82 -> 88 -> 95)
        val readings = listOf(
            BatteryReading(1, makeTs(15, 0), 86, isCharging = false, pluggedType = "NONE"),
            BatteryReading(2, makeTs(15, 15), 82, isCharging = false, pluggedType = "NONE"), // Drop: 4%
            // Charging starts:
            BatteryReading(3, makeTs(15, 30), 88, isCharging = true, pluggedType = "AC"),
            BatteryReading(4, makeTs(15, 45), 95, isCharging = true, pluggedType = "AC")
        )

        val summary = DrainAnalyzer.analyze(readings, testZone)

        val hour15 = summary.hourlyStats[15]
        assertNotNull(hour15)
        // Drop should ONLY be 4%, the charging intervals (82->88 and 88->95) must be excluded!
        assertEquals(4, hour15?.totalDrop)
        assertEquals(1, hour15?.validIntervalCount)
        assertEquals(2, hour15?.chargingSampleCount)
    }

    @Test
    fun testLevelIncreaseWithoutChargingFlagExcluded() {
        // Battery percentage calibration jump up without charging flag
        val readings = listOf(
            BatteryReading(1, makeTs(10, 0), 50, isCharging = false, pluggedType = "NONE"),
            BatteryReading(2, makeTs(10, 15), 52, isCharging = false, pluggedType = "NONE"), // +2% increase
            BatteryReading(3, makeTs(10, 30), 51, isCharging = false, pluggedType = "NONE")  // -1% drop
        )

        val summary = DrainAnalyzer.analyze(readings, testZone)
        val hour10 = summary.hourlyStats[10]
        assertNotNull(hour10)
        // The +2% jump is excluded; only the 1% drop is counted
        assertEquals(1, hour10?.totalDrop)
    }

    @Test
    fun testGapsInDataIgnored() {
        // Phone turned off at 10:00 (90%) and turned back on at 16:00 (50%)
        // Delta is 6 hours (> 2h max gap). This 40% drop must NOT be counted as a single hour drop.
        val readings = listOf(
            BatteryReading(1, makeTs(10, 0), 90, isCharging = false, pluggedType = "NONE"),
            BatteryReading(2, makeTs(16, 0), 50, isCharging = false, pluggedType = "NONE"),
            BatteryReading(3, makeTs(16, 15), 48, isCharging = false, pluggedType = "NONE")
        )

        val summary = DrainAnalyzer.analyze(readings, testZone)

        // Hour 10 should have 0 drop (no valid consecutive pair)
        assertEquals(0, summary.hourlyStats[10]?.totalDrop)
        // Hour 16 should only have 2% drop (from 16:00 to 16:15), NOT 42%
        assertEquals(2, summary.hourlyStats[16]?.totalDrop)
        assertEquals(16, summary.worstHour?.hour)
        assertEquals(2, summary.worstHour?.totalDrop)
    }

    @Test
    fun testTiesHandledDeterministically() {
        // Hour 9 drops by 6%, Hour 18 also drops by 6%
        val readings = listOf(
            BatteryReading(1, makeTs(9, 0), 90, isCharging = false, pluggedType = "NONE"),
            BatteryReading(2, makeTs(9, 30), 84, isCharging = false, pluggedType = "NONE"), // Drop 6%
            BatteryReading(3, makeTs(18, 0), 70, isCharging = false, pluggedType = "NONE"),
            BatteryReading(4, makeTs(18, 15), 67, isCharging = false, pluggedType = "NONE"),
            BatteryReading(5, makeTs(18, 30), 64, isCharging = false, pluggedType = "NONE")  // Drop 3+3 = 6% (2 intervals)
        )

        val summary = DrainAnalyzer.analyze(readings, testZone)

        assertNotNull(summary.worstHour)
        // Hour 18 has higher valid interval count (2 vs 1), so tiebreaker favors hour 18
        assertEquals(18, summary.worstHour?.hour)
        assertEquals(6, summary.worstHour?.totalDrop)
    }

    @Test
    fun testSingleReadingHandledAsSparse() {
        val readings = listOf(
            BatteryReading(1, makeTs(12, 10), 85, isCharging = false, pluggedType = "NONE")
        )

        val summary = DrainAnalyzer.analyze(readings, testZone)

        // Single reading cannot form a drop
        assertNull(summary.worstHour)
        assertNull(summary.worstHero)

        val hour12 = summary.hourlyStats[12]
        assertNotNull(hour12)
        assertEquals(1, hour12?.readingCount)
        assertEquals(0, hour12?.totalDrop)
        assertTrue(hour12!!.isSparse)
        assertTrue(hour12.explanation.contains("Only 1 reading"))
    }

    @Test
    fun testEmptyReadings() {
        val summary = DrainAnalyzer.analyze(emptyList(), testZone)
        assertNull(summary.worstHour)
        assertNull(summary.worstHero)
        assertEquals(24, summary.hourlyStats.size)
        assertTrue(summary.hourlyStats.values.all { it.isSparse && it.totalDrop == 0 })
    }

    @Test
    fun testFormatHourRange() {
        assertEquals("6-7 PM", DrainAnalyzer.formatHourRange(18))
        assertEquals("12-1 PM", DrainAnalyzer.formatHourRange(12))
        assertEquals("12-1 AM", DrainAnalyzer.formatHourRange(0))
        assertEquals("11 AM-12 PM", DrainAnalyzer.formatHourRange(11))
        assertEquals("11 PM-12 AM", DrainAnalyzer.formatHourRange(23))
    }
}
