package com.draindetective.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.data.BatteryReading
import com.draindetective.domain.AnalysisSummary
import com.draindetective.ui.theme.AmberGold
import com.draindetective.ui.theme.BgDark
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary
import com.draindetective.ui.theme.batteryLevelColor

@Composable
fun BatteryHeroGauge(
    latestReading: BatteryReading?,
    isRecording: Boolean,
    onToggleRecording: () -> Unit,
    estimatedHours: Float?,
    modifier: Modifier = Modifier
) {
    val level = latestReading?.level ?: 100
    val isCharging = latestReading?.isCharging == true
    val color = if (isCharging) TealNeon else batteryLevelColor(level)
    val sweepAngle by animateFloatAsState(
        targetValue = (level / 100f) * 360f,
        animationSpec = tween(900),
        label = "heroGaugeSweep"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardElevated, RoundedCornerShape(28.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular gauge
        Box(
            modifier = Modifier.size(118.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val inset = strokeWidth / 2f
                val arcSize = Size(size.width - (inset * 2), size.height - (inset * 2))

                // Sci-fi outer ring (dashed)
                drawArc(
                    color = TealNeon.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset - 6.dp.toPx(), inset - 6.dp.toPx()),
                    size = Size(arcSize.width + 12.dp.toPx(), arcSize.height + 12.dp.toPx()),
                    style = Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )

                // Background track
                drawArc(
                    color = Color.White.copy(alpha = 0.08f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )

                // Filled level arc
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (latestReading != null) "$level%" else "--",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                if (isCharging) {
                    Text(
                        text = "⚡ Plugged",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TealNeon
                    )
                }
            }
        }

        Spacer(Modifier.width(20.dp))

        // Right details column
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "DRAIN DETECTIVE",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                letterSpacing = 1.5.sp
            )

            Text(
                text = if (isRecording) "AUTO-LOGGING ACTIVE" else "FOREGROUND PAUSED",
                color = TealNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (estimatedHours != null && !isCharging) {
                Text(
                    text = "EST: ${"%.1f".format(estimatedHours)}H TO EMPTY",
                    color = AmberGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = isRecording,
                    onCheckedChange = { onToggleRecording() },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = TealNeon,
                        checkedThumbColor = BgDark,
                        uncheckedTrackColor = CardBorder
                    )
                )
                Text(
                    text = if (isRecording) "Active" else "Paused",
                    color = if (isRecording) TealNeon else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun WorstDrainHeroCard(
    summary: AnalysisSummary,
    onOpenCaseFile: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val worstHero = summary.worstHero
    val worstStat = summary.worstHour

    val gradientBrush = if (worstHero != null) {
        Brush.horizontalGradient(
            listOf(Color(0xFF381524), Color(0xFF452214))
        )
    } else {
        Brush.horizontalGradient(
            listOf(CardElevated, CardElevated)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush, RoundedCornerShape(24.dp))
            .border(1.dp, if (worstHero != null) FlameRed.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(24.dp))
            .clickable(enabled = worstStat != null) {
                worstStat?.let { onOpenCaseFile(it.hour) }
            }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (worstHero != null) "🔥" else "🔎",
            fontSize = 38.sp
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "WORST DRAIN HOUR",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            if (worstHero != null && worstStat != null) {
                Text(
                    text = worstHero.labelRange,
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Spacer(Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "-${worstHero.dropPercent}% drop",
                        color = FlameRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• Tap for Case File ➔",
                        color = FlameOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = "Awaiting Data",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (summary.totalReadings <= 1) {
                        "Record at least 2 readings to detect active discharge drop."
                    } else {
                        "Device has been charging or stable. No discharge drop measured yet."
                    },
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
    }
}
