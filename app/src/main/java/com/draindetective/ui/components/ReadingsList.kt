package com.draindetective.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.data.BatteryReading
import com.draindetective.domain.DeviceContext
import com.draindetective.ui.theme.AmberGold
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary
import com.draindetective.ui.theme.batteryLevelColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm  •  dd MMM")
    .withZone(ZoneId.systemDefault())

@Composable
fun ReadingItemRow(
    reading: BatteryReading,
    previousReading: BatteryReading?,
    modifier: Modifier = Modifier
) {
    val delta = if (previousReading != null) {
        previousReading.level - reading.level
    } else {
        null
    }

    val contextObj = DeviceContext.fromLabel(reading.contextLabel)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardElevated, RoundedCornerShape(18.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Battery status dot
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    if (reading.isCharging) TealNeon else batteryLevelColor(reading.level),
                    CircleShape
                )
        )

        Spacer(Modifier.width(14.dp))

        // Level & Timestamp
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${reading.level}%",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                if (reading.isCharging) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "⚡ ${reading.pluggedType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealNeon
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = timeFormatter.format(Instant.ofEpochMilli(reading.timestamp)),
                color = TextSecondary,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(4.dp))

            // Context metadata badges: Temp, Screen, Network, Context Fingerprint
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallTag(
                    text = "${contextObj.emoji} ${contextObj.displayName}",
                    color = when (contextObj) {
                        DeviceContext.IN_POCKET_OR_BAG -> FlameRed
                        DeviceContext.MOVING -> FlameOrange
                        DeviceContext.ON_DESK -> TealNeon
                        else -> TextSecondary
                    }
                )
                SmallTag(
                    text = "${"%.1f".format(reading.temperature)}°C",
                    color = if (reading.temperature >= 38f) FlameRed else TextSecondary
                )
                if (reading.networkType != "NONE") {
                    SmallTag(
                        text = reading.networkType,
                        color = if (reading.networkType == "MOBILE") FlameOrange else TealNeon
                    )
                }
            }
        }

        // Drop / Increase Chip
        when {
            delta == null || delta == 0 -> {
                Chip(text = "0%", color = TextMuted)
            }
            delta > 0 -> {
                Chip(text = "-$delta%", color = FlameRed)
            }
            else -> {
                Chip(text = "+${-delta}% ⚡", color = TealNeon)
            }
        }
    }
}

@Composable
private fun SmallTag(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
fun EmptyReadingsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(CardElevated, RoundedCornerShape(20.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(CardBorder.copy(alpha = 0.3f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔋", fontSize = 42.sp)
            }
            Text(
                text = "No Battery Snapshots",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Tap '+ Save reading' below to record your first manual snapshot, or visit the Settings tab to start background monitoring.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}
