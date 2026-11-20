package com.draindetective.ui.theme

import androidx.compose.ui.graphics.Color

val BgDark = Color(0xFF030305)
val BgDarkVariant = Color(0xFF07070B)
val CardBg = Color(0xFF0B0D13)
val CardElevated = Color(0xFF0F1219)
val CardBorder = Color(0xFF1E2433)

val TealNeon = Color(0xFF00FFCC) // Cyberpunk Cyan
val AmberGold = Color(0xFFFFB800) // Electric Yellow
val FlameRed = Color(0xFFFF2A55) // Neon Pink/Red
val FlameOrange = Color(0xFFFF6600)
val PurpleAccent = Color(0xFF9D00FF) // Deep Violet
val BlueAccent = Color(0xFF0066FF)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0AABF)
val TextMuted = Color(0xFF4A5568)

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
