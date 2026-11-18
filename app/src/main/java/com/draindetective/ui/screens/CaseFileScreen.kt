package com.draindetective.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.draindetective.domain.DrainAnalyzer
import com.draindetective.domain.HourStat
import com.draindetective.ui.components.SuspectLineupSection
import com.draindetective.ui.theme.BgDark
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary
import com.draindetective.usage.SuspectApp
import com.draindetective.usage.UsageTracker

@Composable
fun CaseFileDialog(
    hour: Int,
    stat: HourStat,
    avgHourlyDrop: Float,
    suspects: List<SuspectApp>,
    hasUsagePermission: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val verdict = remember(stat, avgHourlyDrop) {
        CaseFileVerdict.generate(stat, avgHourlyDrop)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.94f)
                .background(BgDark, RoundedCornerShape(28.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(28.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📁", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "CASE FILE #%02d".format(hour),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TealNeon,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "${DrainAnalyzer.formatHourRange(hour)} (%02d:00 - %02d:00)".format(hour, (hour + 1) % 24),
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(CardElevated, RoundedCornerShape(50))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                // Drain Rate & vs Average Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF26121E), Color(0xFF331818))),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, verdict.color.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Observed Drain",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (stat.totalDrop > 0) "-${stat.totalDrop}%" else "0%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = verdict.color
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Box(
                            modifier = Modifier
                                .background(verdict.color.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = verdict.vsAverageBadge,
                                color = verdict.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${stat.dominantContext.emoji} ${stat.dominantContext.displayName}",
                            color = TealNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Plain Language Verdict Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardElevated)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = verdict.title,
                            color = verdict.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = verdict.description,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgDark.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = verdict.contextVerdict,
                                color = TealNeon,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Environmental Conditions Matrix (4 tiles)
                Text(
                    text = "📊 Forensic Conditions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile(
                        icon = "📱",
                        label = "Screen Active",
                        value = "${stat.screenOnMinutes} min",
                        sub = "${(stat.screenOnRatio * 100).toInt()}% ratio",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        icon = "🌡️",
                        label = "Battery Temp",
                        value = "${"%.1f".format(stat.avgTemp)}°C",
                        sub = if (stat.avgTemp >= 38f) "Hot" else "Normal",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val mobilePct = ((stat.networkSplit["MOBILE"] ?: 0f) * 100).toInt()
                    val wifiPct = ((stat.networkSplit["WIFI"] ?: 0f) * 100).toInt()
                    MetricTile(
                        icon = "📶",
                        label = "Network Split",
                        value = if (mobilePct > 0) "$mobilePct% Mobile" else "$wifiPct% WiFi",
                        sub = if (mobilePct > 0 && wifiPct > 0) "$wifiPct% WiFi" else "Active radio",
                        modifier = Modifier.weight(1f)
                    )
                    MetricTile(
                        icon = stat.dominantContext.emoji,
                        label = "Context",
                        value = stat.dominantContext.displayName,
                        sub = if (stat.chargingSampleCount > 0) "${stat.chargingSampleCount} chg samples" else "Pure discharge",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Suspect Lineup Section
                SuspectLineupSection(
                    suspects = suspects,
                    hasUsagePermission = hasUsagePermission,
                    onGrantPermissionClicked = {
                        try {
                            context.startActivity(UsageTracker.createUsageAccessIntent())
                        } catch (_: Exception) {}
                    }
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MetricTile(
    icon: String,
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(CardElevated, RoundedCornerShape(16.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 16.sp)
                Spacer(Modifier.width(6.dp))
                Text(label, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            }
            Text(value, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = TealNeon, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
