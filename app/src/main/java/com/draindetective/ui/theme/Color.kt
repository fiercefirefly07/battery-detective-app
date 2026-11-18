package com.draindetective.ui.theme

import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF0A1020)
val BgDarkVariant = Color(0xFF10182E)
val CardBg = Color(0xFF16213E)
val CardElevated = Color(0xFF1B2745)
val CardBorder = Color(0xFF27375E)

val TealNeon = Color(0xFF2DE2C0)
val AmberGold = Color(0xFFFFC857)
val FlameRed = Color(0xFFFF5D73)
val FlameOrange = Color(0xFFFF7A45)
val PurpleAccent = Color(0xFF8A4FFF)
val BlueAccent = Color(0xFF3B82F6)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF8FA0C4)
val TextMuted = Color(0xFF5E7199)

fun batteryLevelColor(level: Int): Color = when {
    level > 50 -> TealNeon
    level > 20 -> AmberGold
    else -> FlameRed
}

fun drainRateColor(dropPct: Int): Color = when {
    dropPct >= 10 -> FlameRed
    dropPct >= 5 -> FlameOrange
    dropPct >= 2 -> AmberGold
    else -> TealNeon
}
