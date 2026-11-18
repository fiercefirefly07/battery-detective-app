package com.draindetective.ui.screens

import androidx.compose.ui.graphics.Color
import com.draindetective.domain.DeviceContext
import com.draindetective.domain.HourStat
import com.draindetective.ui.theme.AmberGold
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon

data class VerdictInfo(
    val title: String,
    val description: String,
    val vsAverageBadge: String,
    val contextVerdict: String,
    val color: Color
)

object CaseFileVerdict {

    fun generate(stat: HourStat, avgHourlyDrop: Float): VerdictInfo {
        if (stat.isSparse) {
            return VerdictInfo(
                title = "Inconclusive Evidence",
                description = "Only isolated readings were recorded in this hour window. " +
                        "At least 2 consecutive discharge readings are required to analyze conditions with high confidence.",
                vsAverageBadge = "Sparse data",
                contextVerdict = "Context fingerprint unavailable due to sparse sampling.",
                color = AmberGold
            )
        }

        if (stat.totalDrop == 0) {
            val isCharging = stat.chargingSampleCount > 0
            return VerdictInfo(
                title = if (isCharging) "Charging Safehouse" else "Zero Drain Observed",
                description = if (isCharging) {
                    "The device was actively plugged in and charging during this hour. Battery loss was prevented."
                } else {
                    "Battery remained completely steady during this hour with 0% drop across recorded readings."
                },
                vsAverageBadge = "0% drain",
                contextVerdict = if (isCharging) "Location: Power charging dock" else "Location: Idle undisturbed",
                color = TealNeon
            )
        }

        val ratio = if (avgHourlyDrop > 0.1f) stat.totalDrop.toFloat() / avgHourlyDrop else 1.0f
        val vsAverageBadge = when {
            ratio >= 2.5f -> "${"%.1f".format(ratio)}x your average"
            ratio >= 1.2f -> "${"%.1f".format(ratio)}x average"
            ratio <= 0.8f -> "${"%.1f".format(ratio)}x (below average)"
            else -> "Typical average"
        }

        val isHighScreen = stat.screenOnMinutes >= 35 || stat.screenOnRatio >= 0.5f
        val isHighTemp = stat.avgTemp >= 36.5f
        val mobilePct = (stat.networkSplit["MOBILE"] ?: 0f)
        val isMobileData = mobilePct >= 0.5f

        val contextNote = when (stat.dominantContext) {
            DeviceContext.IN_POCKET_OR_BAG -> "👖 Phone was enclosed in a pocket or bag (dark ambient lux & proximity covered). Trapped heat accelerated discharge."
            DeviceContext.ON_DESK -> "🪑 Phone was resting flat and motionless on a desk (low motion variance). Standby power leak suspected."
            DeviceContext.MOVING -> "🚶 Continuous motion detected (high accelerometer variance). Cellular tower handovers likely increased radio draw."
            DeviceContext.IN_USE -> "📱 Active user engagement: Display was on and unobstructed."
            DeviceContext.CHARGING -> "⚡ Device was connected to power."
            DeviceContext.UNKNOWN -> "❓ Background reading: Sensor fingerprinting restricted in standard background without duty mode."
        }

        val (title, desc, color) = when {
            stat.dominantContext == DeviceContext.IN_POCKET_OR_BAG && isHighTemp -> Triple(
                "🔥 Pocket Cooking: Thermal Trapping in Enclosed Bag/Pocket",
                "Severe battery drop while phone was enclosed with zero ventilation. Internal temperature reached ${"%.1f".format(stat.avgTemp)}°C. Check for screen wake-locks or background runaways.",
                FlameRed
            )
            isHighScreen && isHighTemp && isMobileData -> Triple(
                "🔥 Prime Suspect: Heavy Screen & Mobile 5G Load",
                "High screen-on time combined with elevated battery temperature (${"%.1f".format(stat.avgTemp)}°C) and mobile cellular radio transmission drove heavy battery loss.",
                FlameRed
            )
            isHighScreen && isHighTemp -> Triple(
                "📱 Screen Intensity & Thermal Build-Up",
                "Continuous display engagement combined with thermal load (${"%.1f".format(stat.avgTemp)}°C) accelerated battery discharge well above baseline.",
                FlameOrange
            )
            stat.dominantContext == DeviceContext.MOVING && isMobileData -> Triple(
                "🚶 Rapid Cellular Handover in Transit",
                "Device was moving actively while connected to mobile cellular data. Constant base-station handshakes drove battery loss.",
                FlameOrange
            )
            !isHighScreen && isMobileData && stat.totalDrop >= 5 -> Triple(
                "👻 Phantom Background Drain",
                "Screen was off for the majority of the hour, but mobile data connectivity and active background synchronizations drew significant continuous power.",
                FlameOrange
            )
            !isHighScreen && stat.totalDrop >= 5 -> Triple(
                "⚙️ Silent Background Activity",
                "Unusual standby discharge detected: battery dropped by ${stat.totalDrop}% while display was inactive. Likely caused by wake-locks or background polling.",
                AmberGold
            )
            isHighScreen -> Triple(
                "💡 Active Display Engagement",
                "Active screen-on usage was the primary power consumer during this hour. Thermal and radio conditions remained within safe normal limits.",
                TealNeon
            )
            else -> Triple(
                "⚖️ Routine Discharge",
                "Normal standard battery consumption. Drain rate remained aligned with healthy baseline usage.",
                TealNeon
            )
        }

        return VerdictInfo(title, desc, vsAverageBadge, contextNote, color)
    }
}
