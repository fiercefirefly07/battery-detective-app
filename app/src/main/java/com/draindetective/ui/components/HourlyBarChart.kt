package com.draindetective.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.domain.AnalysisSummary
import com.draindetective.ui.theme.CardBg
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary

@Composable
fun HourlyBarChart(
    summary: AnalysisSummary,
    selectedHour: Int?,
    onSelectHour: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxDrop = (summary.hourlyStats.values.maxOfOrNull { it.totalDrop } ?: 1).coerceAtLeast(1)
    val worstH = summary.worstHour?.hour

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardElevated, RoundedCornerShape(24.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Hourly Drain Analysis",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = "Tap any bar to inspect Case File",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            if (summary.worstHour != null) {
                Box(
                    modifier = Modifier
                        .background(FlameRed.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Peak: -${summary.worstHour.totalDrop}%",
                        color = FlameRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // Animated chart canvas
        val animProgress by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(800),
            label = "chartAnim"
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .pointerInput(summary) {
                        detectTapGestures { offset ->
                            val barWidth = size.width / 24f
                            val tappedHour = (offset.x / barWidth).toInt().coerceIn(0, 23)
                            onSelectHour(tappedHour)
                        }
                    }
            ) {
                val totalWidth = size.width
                val totalHeight = size.height
                val barSlotWidth = totalWidth / 24f
                val barSpacing = 3.dp.toPx()
                val barWidth = (barSlotWidth - barSpacing).coerceAtLeast(2f)

                // Draw subtle baseline and horizontal grid guides
                val guideLines = 3
                for (g in 1..guideLines) {
                    val y = totalHeight * (g.toFloat() / (guideLines + 1))
                    drawLine(
                        color = CardBorder.copy(alpha = 0.4f),
                        start = Offset(0f, y),
                        end = Offset(totalWidth, y),
                        strokeWidth = 1f
                    )
                }

                for (h in 0..23) {
                    val stat = summary.hourlyStats[h]
                    val drop = stat?.totalDrop ?: 0
                    val isWorst = (h == worstH && drop > 0)
                    val isSelected = (h == selectedHour)

                    val normalizedHeight = if (drop > 0) {
                        (drop.toFloat() / maxDrop.toFloat()) * totalHeight * animProgress
                    } else {
                        // Tiny stub for sparse/zero hours
                        4.dp.toPx()
                    }

                    val x = (h * barSlotWidth) + (barSpacing / 2f)
                    val y = totalHeight - normalizedHeight

                    val barBrush = when {
                        isWorst -> Brush.verticalGradient(
                            listOf(FlameRed, FlameOrange)
                        )
                        isSelected -> Brush.verticalGradient(
                            listOf(TealNeon, TealNeon.copy(alpha = 0.5f))
                        )
                        drop > 0 -> Brush.verticalGradient(
                            listOf(TealNeon.copy(alpha = 0.85f), CardBg)
                        )
                        else -> Brush.verticalGradient(
                            listOf(CardBorder, CardBorder.copy(alpha = 0.3f))
                        )
                    }

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, normalizedHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    // Draw a highlight marker for worst hour
                    if (isWorst) {
                        drawCircle(
                            color = FlameRed,
                            radius = 3.dp.toPx(),
                            center = Offset(x + (barWidth / 2f), (y - 6.dp.toPx()).coerceAtLeast(4.dp.toPx()))
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Time axis labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("00:00", "04:00", "08:00", "12:00", "16:00", "20:00", "23:00").forEach { label ->
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
