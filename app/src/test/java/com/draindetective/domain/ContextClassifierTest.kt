package com.draindetective.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ContextClassifierTest {

    @Test
    fun testChargingTakesHighestPrecedence() {
        // Even if moving or in pocket, charging state is definitive
        val result = ContextClassifier.classify(
            proximityCovered = true,
            lightLux = 0f,
            motionLevel = 1.2f,
            isCharging = true,
            screenOn = false
        )
        assertEquals(DeviceContext.CHARGING, result)
    }

    @Test
    fun testScreenOnWithoutObstructionIsInUse() {
        val result = ContextClassifier.classify(
            proximityCovered = false,
            lightLux = 150f,
            motionLevel = 0.08f,
            isCharging = false,
            screenOn = true
        )
        assertEquals(DeviceContext.IN_USE, result)
    }

    @Test
    fun testInPocketOrBagWhenProximityCoveredAndDark() {
        val result = ContextClassifier.classify(
            proximityCovered = true,
            lightLux = 1.5f,
            motionLevel = 0.15f,
            isCharging = false,
            screenOn = false
        )
        assertEquals(DeviceContext.IN_POCKET_OR_BAG, result)
    }

    @Test
    fun testMovingWhenHighMotionVariance() {
        val result = ContextClassifier.classify(
            proximityCovered = false,
            lightLux = 80f,
            motionLevel = 0.75f,
            isCharging = false,
            screenOn = false
        )
        assertEquals(DeviceContext.MOVING, result)
    }

    @Test
    fun testOnDeskWhenStationaryAndScreenOff() {
        val result = ContextClassifier.classify(
            proximityCovered = false,
            lightLux = 45f,
            motionLevel = 0.015f,
            isCharging = false,
            screenOn = false
        )
        assertEquals(DeviceContext.ON_DESK, result)
    }

    @Test
    fun testUnknownWhenAllSensorsNull() {
        // Typical background worker without foreground sensor access
        val result = ContextClassifier.classify(
            proximityCovered = null,
            lightLux = null,
            motionLevel = null,
            isCharging = false,
            screenOn = false
        )
        assertEquals(DeviceContext.UNKNOWN, result)
    }
}
