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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.draindetective.blackbox.BlackBoxRecorder
import com.draindetective.blackbox.HighResPoint
import com.draindetective.data.Incident
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
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val incidentTimeFmt = DateTimeFormatter.ofPattern("HH:mm  •  dd MMM")
    .withZone(ZoneId.systemDefault())

@Composable
fun IncidentsScreen(
    incidents: List<Incident>,
    isRecordingLive: Boolean,
    liveProgress: Float?,
    modifier: Modifier = Modifier
) {
    var selectedIncidentId by remember { mutableStateOf<Long?>(incidents.firstOrNull()?.id) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "📦 Black Box Incidents",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "High-resolution 10-minute flight recorder triggered during acute drain spikes.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Live recording alert banner
        if (isRecordingLive) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF3B1E12), Color(0xFF4A2518))),
                            RoundedCornerShape(18.dp)
                        )
                        .border(1.dp, FlameOrange, RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🚨", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Black Box Capture In Progress!",
                                color = FlameOrange,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Sampling environment & battery level every 20s for 10 minutes...",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        liveProgress?.let { prog ->
                            Slider(
                                value = prog,
                                onValueChange = {},
                                enabled = false,
                                colors = SliderDefaults.colors(
                                    disabledThumbColor = FlameOrange,
                                    disabledActiveTrackColor = FlameRed
                                )
                            )
                        }
                    }
                }
            }
        }

        if (incidents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardElevated, RoundedCornerShape(20.dp))
                        .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🛡️", fontSize = 42.sp)
                        Text(
                            text = "No Flight Incidents Logged",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Black Box automatically triggers high-resolution capture when sudden battery drop spikes (>= 3% in 15m) occur. You can seed a realistic demo incident in Settings.",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Selected Incident Replay Box
            val selected = incidents.firstOrNull { it.id == selectedIncidentId } ?: incidents.first()
            item {
                IncidentReplayerCard(incident = selected)
            }

            item {
                Text(
                    text = "Recorded Incidents (${incidents.size})",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(incidents) { incident ->
                val isSelected = incident.id == selectedIncidentId
                IncidentListItem(
                    incident = incident,
                    isSelected = isSelected,
                    onSelect = { selectedIncidentId = incident.id }
                )
            }
        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
fun IncidentReplayerCard(incident: Incident) {
    val points = remember(incident.readingsJson) {
        BlackBoxRecorder.parsePoints(incident.readingsJson)
    }

    var sliderPos by remember(incident.id) { mutableFloatStateOf(0f) }
    val maxIndex = (points.size - 1).coerceAtLeast(0)
    val currentIndex = (sliderPos * maxIndex).toInt().coerceIn(0, maxIndex)
    val currentPt: HighResPoint? = points.getOrNull(currentIndex)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardElevated)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Incident Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📼", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "INCIDENT FLIGHT RECORDER",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TealNeon,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = incidentTimeFmt.format(Instant.ofEpochMilli(incident.startTime)),
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .background(FlameRed.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "-${incident.dropPercent}% in 10m",
                        color = FlameRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Verdict Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF28131B), RoundedCornerShape(14.dp))
                    .border(1.dp, FlameRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "🔎 Black Box Verdict",
                        color = FlameRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = incident.verdict,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 17.sp
                    )
                }
            }

            // Replay Scrubber Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Scrubber: Sample ${currentIndex + 1} of ${points.size}",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${(currentIndex * 20) / 60}m ${(currentIndex * 20) % 60}s",
                        color = AmberGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = sliderPos,
                    onValueChange = { sliderPos = it },
                    colors = SliderDefaults.colors(
                        thumbColor = TealNeon,
                        activeTrackColor = TealNeon,
                        inactiveTrackColor = CardBorder
                    )
                )
            }

            // Telemetry values at scrubber position
            if (currentPt != null) {
                val ctx = DeviceContext.fromLabel(currentPt.contextLabel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniTelemetryBox("Battery", "${currentPt.level}%", TealNeon, Modifier.weight(1f))
                    MiniTelemetryBox("Temp", "${"%.1f".format(currentPt.temperature)}°C", if (currentPt.temperature >= 38f) FlameRed else AmberGold, Modifier.weight(1f))
                    MiniTelemetryBox("Context", "${ctx.emoji} ${ctx.displayName}", TextPrimary, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniTelemetryBox(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(CardBorder.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(label, color = TextMuted, fontSize = 10.sp)
            Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun IncidentListItem(
    incident: Incident,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val ctx = DeviceContext.fromLabel(incident.dominantContext)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) CardElevated else CardBorder.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .border(
                1.dp,
                if (isSelected) TealNeon else CardBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🚨", fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${ctx.emoji} ${ctx.displayName} • -${incident.dropPercent}% drop",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = incidentTimeFmt.format(Instant.ofEpochMilli(incident.startTime)),
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
        Text(
            text = "Peak: -${"%.0f".format(incident.peakDrainRatePerHour)}%/h",
            color = FlameRed,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
