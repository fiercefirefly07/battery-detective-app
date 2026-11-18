package com.draindetective.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.domain.AnalysisSummary
import com.draindetective.domain.DrainAnalyzer
import com.draindetective.ui.theme.AmberGold
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameOrange
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary

@Composable
fun HeatmapScreen(
    summary: AnalysisSummary,
    onSelectHour: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxDrop = (summary.hourlyStats.values.maxOfOrNull { it.totalDrop } ?: 1).coerceAtLeast(1)
    val worstHour = summary.worstHour?.hour

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column {
            Text(
                text = "Hour-of-Day Heatmap",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "24-hour cycle drain intensity. Tap any hour to examine forensic evidence.",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Color Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardElevated, RoundedCornerShape(14.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = CardBorder, label = "0% / Idle")
            LegendItem(color = TealNeon, label = "Low (1-3%)")
            LegendItem(color = AmberGold, label = "Med (4-7%)")
            LegendItem(color = FlameRed, label = "High (8%+)")
        }

        // 24 Cells Grid (4 columns x 6 rows)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(24) { hour ->
                val stat = summary.hourlyStats[hour]
                val drop = stat?.totalDrop ?: 0
                val isWorst = (hour == worstHour && drop > 0)

                val cellBg = when {
                    isWorst -> FlameRed.copy(alpha = 0.35f)
                    drop >= 8 -> FlameOrange.copy(alpha = 0.28f)
                    drop >= 4 -> AmberGold.copy(alpha = 0.22f)
                    drop > 0 -> TealNeon.copy(alpha = 0.15f)
                    else -> CardElevated
                }

                val borderColor = when {
                    isWorst -> FlameRed
                    drop >= 8 -> FlameOrange
                    drop > 0 -> TealNeon.copy(alpha = 0.4f)
                    else -> CardBorder
                }

                Box(
                    modifier = Modifier
                        .height(74.dp)
                        .background(cellBg, RoundedCornerShape(14.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { onSelectHour(hour) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "%02d:00".format(hour),
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isWorst) {
                                Spacer(Modifier.width(2.dp))
                                Text("🔥", fontSize = 10.sp)
                            }
                        }

                        Text(
                            text = if (drop > 0) "-$drop%" else if (stat?.isSparse == true) "--" else "0%",
                            color = if (isWorst) FlameRed else if (drop > 0) TextPrimary else TextMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (stat?.chargingSampleCount ?: 0 > 0) "⚡ Chg" else if (stat?.isSparse == true) "Sparse" else "${stat?.readingCount} pts",
                            color = if (stat?.chargingSampleCount ?: 0 > 0) TealNeon else TextMuted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(10.dp)
                .height(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(text = label, color = TextMuted, fontSize = 10.sp)
    }
}
