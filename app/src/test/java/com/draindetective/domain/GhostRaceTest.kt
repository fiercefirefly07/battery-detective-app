package com.draindetective.domain

import com.draindetective.data.BatteryReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class GhostRaceTest {

    private val zone = ZoneId.of("UTC")

    private fun makeTs(day: Int, hour: Int, minute: Int): Long {
        return ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun testEmptyOrSingleDayReturnsCalibrating() {
        val readings = listOf(
            BatteryReading(1, makeTs(18, 10, 0), 90, isCharging = false),
            BatteryReading(2, makeTs(18, 10, 15), 88, isCharging = false)
        )
        val summary = GhostRaceAnalyzer.analyze(readings, zone)
        assertFalse(summary.hasEnoughData)
        assertTrue(summary.bannerTitle.contains("Calibrating"))
    }

    @Test
    fun testFindsBestDayAndComputesAheadDifference() {
        val list = mutableListOf<BatteryReading>()

        // Day 16 (Heavy drain day - drops 3% every hour)
        var level16 = 90
        for (h in 8..18) {
            list.add(BatteryReading(0, makeTs(16, h, 0), level16, false, "NONE", 32f, true, "WIFI", false))
            level16 -= 3
        }

        // Day 17 (Ghost day - super efficient! Only drops 1% every hour)
        var level17 = 95
        for (h in 8..18) {
            list.add(BatteryReading(0, makeTs(17, h, 0), level17, false, "NONE", 28f, false, "WIFI", false))
            level17 -= 1
        }

        // Today (Day 18): very efficient, at hour 14 is at 92% (Ghost day was 95 - 6 = 89%)
        var level18 = 98
        for (h in 8..14) {
            list.add(BatteryReading(0, makeTs(18, h, 0), level18, false, "NONE", 29f, false, "WIFI", false))
            level18 -= 1
        }

        val summary = GhostRaceAnalyzer.analyze(list, zone)

        assertTrue(summary.hasEnoughData)
        assertEquals("17 NOV", summary.ghostDayDate)
        assertTrue(summary.isAhead)
        assertTrue(summary.currentLevelDiff > 0)
        assertTrue(summary.bannerTitle.contains("ahead"))
    }
}
