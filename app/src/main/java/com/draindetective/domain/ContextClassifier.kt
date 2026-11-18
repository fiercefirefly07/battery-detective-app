package com.draindetective.domain

/**
 * Enumeration of recognized situational physical contexts.
 */
enum class DeviceContext(val label: String, val displayName: String, val emoji: String) {
    ON_DESK("ON_DESK", "On Desk", "🪑"),
    IN_POCKET_OR_BAG("IN_POCKET_OR_BAG", "In Pocket / Bag", "👖"),
    MOVING("MOVING", "Moving / In Transit", "🚶"),
    CHARGING("CHARGING", "Charging", "⚡"),
    IN_USE("IN_USE", "Active in Use", "📱"),
    UNKNOWN("UNKNOWN", "Unknown Context", "❓");

    companion object {
        fun fromLabel(label: String?): DeviceContext =
            entries.firstOrNull { it.label == label } ?: UNKNOWN
    }
}

/**
 * Pure Kotlin classifier that maps physical hardware sensors (proximity, light, accelerometer variance)
 * combined with power and screen states into situational contexts.
 */
object ContextClassifier {

    /** Inside a dark pocket or enclosed bag, ambient light is typically below 5 lux. */
    const val POCKET_MAX_LIGHT_LUX: Float = 5.0f

    /** A device resting motionless on a stationary surface has negligible acceleration variance (< 0.05). */
    const val DESK_MAX_MOTION_VARIANCE: Float = 0.05f

    /** Walking, jogging, or vehicle transit induces distinct motion variance (>= 0.35). */
    const val MOVING_MIN_MOTION_VARIANCE: Float = 0.35f

    /**
     * Classifies the device context from current environmental and power parameters.
     *
     * @param proximityCovered True if proximity sensor indicates an object right next to the screen
     * @param lightLux Ambient light reading in lux (nullable if sensor missing or disabled)
     * @param motionLevel Accelerometer magnitude variance over a ~5 s sampling window (nullable)
     * @param isCharging True if battery state is charging or connected to external power
     * @param screenOn True if the device display is interactive / turned on
     * @return The classified [DeviceContext]
     */
    fun classify(
        proximityCovered: Boolean?,
        lightLux: Float?,
        motionLevel: Float?,
        isCharging: Boolean,
        screenOn: Boolean
    ): DeviceContext {
        // Priority 1: Connected to charger
        if (isCharging) {
            return DeviceContext.CHARGING
        }

        // Priority 2: Screen active & interactive without obstruction
        if (screenOn && proximityCovered != true) {
            return DeviceContext.IN_USE
        }

        // Priority 3: Inside pocket or enclosed bag
        // Proximity sensor covered combined with dark ambient light (< 5 lux) or unlit proximity
        if (proximityCovered == true) {
            if (lightLux == null || lightLux <= POCKET_MAX_LIGHT_LUX) {
                return DeviceContext.IN_POCKET_OR_BAG
            }
        }

        // Priority 4: In active physical transit / walking (screen off or passive)
        if (motionLevel != null && motionLevel >= MOVING_MIN_MOTION_VARIANCE) {
            return DeviceContext.MOVING
        }

        // Priority 5: Resting flat on desk / table
        // Negligible motion, screen off, proximity unobstructed
        if (motionLevel != null && motionLevel <= DESK_MAX_MOTION_VARIANCE && !screenOn) {
            if (proximityCovered != true) {
                return DeviceContext.ON_DESK
            }
        }

        // Priority 6: Partial clue fallbacks
        if (proximityCovered == true && screenOn) {
            // Accidental pocket touch or phone call to ear
            return DeviceContext.IN_POCKET_OR_BAG
        }

        if (proximityCovered == false && lightLux != null && lightLux > 20f && !screenOn && motionLevel != null && motionLevel <= 0.10f) {
            // In room light, stationary, screen off
            return DeviceContext.ON_DESK
        }

        // If sensor values are missing (e.g. background restriction) or ambiguous
        return DeviceContext.UNKNOWN
    }
}
