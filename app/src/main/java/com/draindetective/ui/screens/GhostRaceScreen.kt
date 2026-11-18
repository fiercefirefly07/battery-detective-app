package com.draindetective.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.domain.GhostPoint
import com.draindetective.domain.GhostRaceSummary
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary

@Composable
fun GhostRaceScreen(
    summary: GhostRaceSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "👻 Ghost Race",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Race today's live battery curve against your all-time most efficient day.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Ahead / Behind Banner
        val bannerGradient = if (!summary.hasEnoughData) {
            Brush.horizontalGradient(listOf(CardElevated, CardElevated))
        } else if (summary.isAhead) {
            Brush.horizontalGradient(listOf(Color(0xFF0F302A), Color(0xFF14453A)))
        } else {
            Brush.horizontalGradient(listOf(Color(0xFF381520), Color(0xFF452216)))
        }

        val borderColor = if (!summary.hasEnoughData) {
            CardBorder
        } else if (summary.isAhead) {
            TealNeon.copy(alpha = 0.5f)
        } else {
            FlameRed.copy(alpha = 0.5f)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bannerGradient, RoundedCornerShape(20.dp))
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = summary.bannerTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (summary.isAhead) TealNeon else FlameRed
                    )
                    if (summary.hasEnoughData) {
                        Text(
                            text = if (summary.isAhead) "OUTPERFORMING" else "FALLING BEHIND",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (summary.isAhead) TealNeon else FlameOrange,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = summary.bannerReason,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (summary.hasEnoughData && summary.ghostDayDate != null) {
                    Text(
                        text = "Benchmark Ghost Day: ${summary.ghostDayDate} (Avg -${"%.1f".format(summary.ghostAverageHourlyDrop)}%/hr)",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Live vs Ghost Canvas Curve Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardElevated, RoundedCornerShape(24.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "24-Hour Diurnal Overlay",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendMarker(color = Color.White, label = "Today")
                        LegendMarker(color = TealNeon.copy(alpha = 0.5f), label = "Ghost Day")
                    }
                }

                GhostCanvasGraph(
                    todayPoints = summary.todayPoints,
                    ghostPoints = summary.ghostPoints,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                )

                // Time axis labels
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("00:00", "06:00", "12:00", "18:00", "24:00").forEach { label ->
                        Text(
                            text = label,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostCanvasGraph(
    todayPoints: List<GhostPoint>,
    ghostPoints: List<GhostPoint>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Draw horizontal grid lines for 25%, 50%, 75%
        for (pct in listOf(0.25f, 0.50f, 0.75f)) {
            val y = h * (1f - pct)
            drawLine(
                color = CardBorder.copy(alpha = 0.35f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        // Helper to convert (minuteOfDay, level) to Canvas (x, y)
        fun toOffset(pt: GhostPoint): Offset {
            val x = (pt.minuteOfDay.toFloat() / 1440f) * w
            val y = h * (1f - (pt.level.toFloat() / 100f))
            return Offset(x, y)
        }

        // Draw Ghost Curve (Faint, dashed teal)
        if (ghostPoints.size >= 2) {
            val ghostPath = Path()
            val first = toOffset(ghostPoints.first())
            ghostPath.moveTo(first.x, first.y)
            for (i in 1 until ghostPoints.size) {
                val pt = toOffset(ghostPoints[i])
                ghostPath.lineTo(pt.x, pt.y)
            }

            drawPath(
                path = ghostPath,
                color = TealNeon.copy(alpha = 0.45f),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                    cap = StrokeCap.Round
                )
            )
        }

        // Draw Today's Live Curve (Solid white/amber with glowing head)
        if (todayPoints.size >= 2) {
            val todayPath = Path()
            val first = toOffset(todayPoints.first())
            todayPath.moveTo(first.x, first.y)
            for (i in 1 until todayPoints.size) {
                val pt = toOffset(todayPoints[i])
                todayPath.lineTo(pt.x, pt.y)
            }

            drawPath(
                path = todayPath,
                color = Color.White,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Live glowing dot at latest point
            val latest = toOffset(todayPoints.last())
            drawCircle(
                color = TealNeon.copy(alpha = 0.35f),
                radius = 7.dp.toPx(),
                center = latest
            )
            drawCircle(
                color = Color.White,
                radius = 3.5.dp.toPx(),
                center = latest
            )
        }
    }
}

@Composable
private fun LegendMarker(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(5.dp))
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
    }
}
