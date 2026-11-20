package com.draindetective.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.draindetective.ui.theme.AmberGold
import com.draindetective.ui.theme.BgDark
import com.draindetective.ui.theme.CardBorder
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.FlameRed
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary
import com.draindetective.ui.theme.TextSecondary
import com.draindetective.usage.UsageTracker
import java.util.concurrent.TimeUnit

@Composable
fun SettingsScreen(
    isBackgroundEnabled: Boolean,
    onToggleBackground: (Boolean) -> Unit,
    isDetectiveDutyActive: Boolean,
    onToggleDetectiveDuty: (Boolean) -> Unit,
    lastBackgroundTs: Long,
    hasUsagePermission: Boolean,
    onCheckUsagePermission: () -> Unit,
    onSeedDemoData: () -> Unit,
    onClearData: () -> Unit,
    totalReadingsCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val lastBgText = if (lastBackgroundTs > 0) {
        val diffMs = (System.currentTimeMillis() - lastBackgroundTs).coerceAtLeast(0)
        val mins = TimeUnit.MILLISECONDS.toMinutes(diffMs)
        when {
            mins < 1 -> "just now"
            mins < 60 -> "$mins min ago"
            else -> "${mins / 60}h ${mins % 60}m ago"
        }
    } else {
        "Never / awaiting first run"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = "Settings & Forensics",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Configure your monitoring, alerts, and system permissions.",
                color = TextSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Section 1: Detective on Duty (Foreground Service)
        SettingCard(
            icon = "🕵️",
            title = "Detective on Duty",
            subtitle = "Continuous situational sensor sampling (Requires persistent notification).",
            isActive = isDetectiveDutyActive
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Always-on monitoring",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = isDetectiveDutyActive,
                    onCheckedChange = { enable ->
                        if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        onToggleDetectiveDuty(enable)
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = TealNeon,
                        checkedThumbColor = BgDark,
                        uncheckedTrackColor = CardBorder
                    )
                )
            }
            Text(
                text = "Runs a low-power service to continuously monitor sensors and detect if your phone is in a pocket, moving, or on a desk.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Section 2: Standard WorkManager Background Recording
        SettingCard(
            icon = "⏱️",
            title = "Background Sync",
            subtitle = "Periodic battery snapshots (Every ~15 mins).",
            isActive = isBackgroundEnabled
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Enable Sync",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Last synced: $lastBgText",
                        color = if (isBackgroundEnabled) TealNeon else TextMuted,
                        fontSize = 12.sp
                    )
                }
                Switch(
                    checked = isBackgroundEnabled,
                    onCheckedChange = onToggleBackground,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = TealNeon,
                        checkedThumbColor = BgDark,
                        uncheckedTrackColor = CardBorder
                    )
                )
            }
        }

        // Section 3: Pocket Cooking & Notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardElevated, RoundedCornerShape(20.dp))
                    .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pocket Cooking Alerts",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (hasNotificationPermission) "Enabled (Notifications granted)" else "Permission needed for critical overheat alerts",
                            color = if (hasNotificationPermission) TealNeon else AmberGold,
                            fontSize = 12.sp
                        )
                    }
                    if (!hasNotificationPermission) {
                        Button(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberGold, contentColor = BgDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Allow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section 4: Usage Stats Access (Suspect Lineup)
        SettingCard(
            icon = "📊",
            title = "App Usage Access",
            subtitle = "Required to identify which apps are draining your battery.",
            isActive = hasUsagePermission
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasUsagePermission) "Access Granted" else "Action Required",
                    color = if (hasUsagePermission) TealNeon else AmberGold,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Button(
                    onClick = {
                        try {
                            context.startActivity(UsageTracker.createUsageAccessIntent())
                        } catch (_: Exception) {}
                        onCheckUsagePermission()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasUsagePermission) CardBorder else AmberGold,
                        contentColor = if (hasUsagePermission) TextPrimary else BgDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (hasUsagePermission) "Review Settings" else "Grant Access",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section 5: "How Context Fingerprint Works" (Educational / Transparency Section)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardElevated, RoundedCornerShape(20.dp))
                .border(1.dp, TealNeon.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔬", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "How Context Fingerprint Works",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealNeon
                    )
                }
                Text(
                    text = "Most battery managers only list which app was open. Drain Detective reveals the physical situation of your device when energy was lost:\n\n" +
                            "• 👖 IN POCKET / BAG: Proximity sensor covered (< 5cm) AND ambient light < 5 lux. If temperature rises >= 37.5°C during rapid discharge, a 'Pocket Cooking' alert is triggered.\n\n" +
                            "• 🪑 ON DESK: Motion variance <= 0.05 m/s² (stationary) with display off and proximity clear.\n\n" +
                            "• 🚶 MOVING / IN TRANSIT: Accelerometer variance >= 0.35 m/s² indicates walking or vehicle movement with continuous base-station handovers.\n\n" +
                            "• 📱 IN USE: Screen active and interactive.\n\n" +
                            "• ⚡ CHARGING: Connected to AC, USB, or wireless power dock.\n\n" +
                            "• ❓ UNKNOWN: Default state when sensors are not accessible (e.g. background restriction without Detective on Duty).",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // Section 6: Judge Demo Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardElevated, RoundedCornerShape(20.dp))
                .border(1.dp, TealNeon.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Judge Demonstration Tools",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealNeon
                    )
                }

                Text(
                    text = "Seeds 3 days (288 snapshots) covering all 6 contexts (Desk, Pocket, Moving, In Use, Charging), an acute Pocket-Cooking incident, a clean Ghost Race day benchmark, and 1 Black Box flight incident with 30 scrubbable steps for instant evaluation.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSeedDemoData,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealNeon,
                            contentColor = BgDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Seed Full Demo Data", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = onClearData,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = FlameRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Clear All", fontSize = 13.sp)
                    }
                }

                Text(
                    text = "Currently stored readings: $totalReadingsCount records",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
fun SettingCard(
    icon: String,
    title: String,
    subtitle: String,
    isActive: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardElevated, RoundedCornerShape(20.dp))
            .border(1.dp, if (isActive) TealNeon.copy(alpha = 0.5f) else CardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(if (isActive) TealNeon.copy(alpha = 0.15f) else CardBorder.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                ) {
                    Text(icon, fontSize = 20.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            content()
        }
    }
}
