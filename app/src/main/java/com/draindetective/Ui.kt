package com.draindetective

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.draindetective.ui.components.BatteryHeroGauge
import com.draindetective.ui.components.ContextDrainChart
import com.draindetective.ui.components.EmptyReadingsState
import com.draindetective.ui.components.HourlyBarChart
import com.draindetective.ui.components.ReadingItemRow
import com.draindetective.ui.components.WorstDrainHeroCard
import com.draindetective.ui.screens.CaseFileDialog
import com.draindetective.ui.screens.GhostRaceScreen
import com.draindetective.ui.screens.HeatmapScreen
import com.draindetective.ui.screens.IncidentsScreen
import com.draindetective.ui.screens.SettingsScreen
import com.draindetective.ui.theme.BgDark
import com.draindetective.ui.theme.BgDarkVariant
import com.draindetective.ui.theme.CardElevated
import com.draindetective.ui.theme.TealNeon
import com.draindetective.ui.theme.TextMuted
import com.draindetective.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Root(vm: MainViewModel = viewModel()) {
    val readings by vm.readings.collectAsStateWithLifecycle()
    val incidents by vm.incidents.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val ghostSummary by vm.ghostRaceSummary.collectAsStateWithLifecycle()
    val isRecording by vm.isForegroundRecording.collectAsStateWithLifecycle()
    val isBackgroundEnabled by vm.isBackgroundWorkerEnabled.collectAsStateWithLifecycle()
    val isDetectiveDutyActive by vm.isDetectiveDutyActive.collectAsStateWithLifecycle()
    val lastBackgroundTs by vm.lastBackgroundReadingTime.collectAsStateWithLifecycle()
    val selectedHour by vm.selectedHour.collectAsStateWithLifecycle()
    val suspectApps by vm.suspectApps.collectAsStateWithLifecycle()
    val hasUsagePermission by vm.hasUsagePermission.collectAsStateWithLifecycle()
    val isRecordingBlackBoxLive by vm.isRecordingBlackBoxLive.collectAsStateWithLifecycle()
    val blackBoxProgress by vm.blackBoxLiveProgress.collectAsStateWithLifecycle()

    var currentTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        vm.checkUsagePermission()
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TealNeon,
            surface = CardElevated,
            background = BgDark
        )
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(BgDark, BgDarkVariant))),
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Drain Detective",
                            fontWeight = FontWeight.Bold,
                            color = TealNeon
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = TealNeon
                    )
                )
            },
            floatingActionButton = {
                if (currentTab == 0) {
                    ExtendedFloatingActionButton(
                        onClick = { vm.saveManualReading() },
                        containerColor = TealNeon,
                        contentColor = BgDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "+ Save reading",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = CardElevated,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    val tabs = listOf(
                        Icons.Rounded.BatteryChargingFull to "Readings",
                        Icons.Rounded.Analytics to "Hourly",
                        Icons.Rounded.DirectionsRun to "Ghost",
                        Icons.Rounded.BugReport to "Incidents",
                        Icons.Rounded.Settings to "Settings"
                    )
                    tabs.forEachIndexed { index, (icon, title) ->
                        NavigationBarItem(
                            selected = currentTab == index,
                            onClick = { currentTab = index },
                            icon = { Icon(icon, contentDescription = title) },
                            label = {
                                Text(
                                    text = title,
                                    fontSize = 11.sp,
                                    fontWeight = if (currentTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealNeon,
                                selectedTextColor = TealNeon,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = TealNeon.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "tab_transition"
                ) { targetTab ->
                    when (targetTab) {
                    0 -> {
                        // Readings Tab: Gauge, Worst Hour Hero, Context Fingerprint Chart, Snapshots List
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                BatteryHeroGauge(
                                    latestReading = readings.firstOrNull(),
                                    isRecording = isRecording,
                                    onToggleRecording = { vm.toggleForegroundRecording() },
                                    estimatedHours = summary.estimatedHoursToEmpty
                                )
                            }

                            item {
                                WorstDrainHeroCard(
                                    summary = summary,
                                    onOpenCaseFile = { hour -> vm.selectHour(hour) }
                                )
                            }

                            item {
                                ContextDrainChart(rates = summary.contextDrainRates)
                            }

                            if (readings.isEmpty()) {
                                item {
                                    EmptyReadingsState()
                                }
                            } else {
                                item {
                                    Text(
                                        text = "Recorded Snapshots (${readings.size})",
                                        color = TextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }

                                itemsIndexed(
                                    items = readings,
                                    key = { _, reading -> reading.id }
                                ) { index, reading ->
                                    val previous = readings.getOrNull(index + 1)
                                    ReadingItemRow(
                                        reading = reading,
                                        previousReading = previous
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Hourly Tab: Canvas Bar Chart + 24-Cell Heatmap
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                HourlyBarChart(
                                    summary = summary,
                                    selectedHour = selectedHour,
                                    onSelectHour = { hour -> vm.selectHour(hour) }
                                )
                            }

                            item {
                                HeatmapScreen(
                                    summary = summary,
                                    onSelectHour = { hour -> vm.selectHour(hour) }
                                )
                            }

                            item {
                                WorstDrainHeroCard(
                                    summary = summary,
                                    onOpenCaseFile = { hour -> vm.selectHour(hour) }
                                )
                            }
                        }
                    }

                    2 -> {
                        // Ghost Race Tab: Real-Time vs Best Day Overlay
                        GhostRaceScreen(summary = ghostSummary)
                    }

                    3 -> {
                        // Black Box Incidents Tab: Flight Recorder & Replay Scrubber
                        IncidentsScreen(
                            incidents = incidents,
                            isRecordingLive = isRecordingBlackBoxLive,
                            liveProgress = blackBoxProgress
                        )
                    }

                    4 -> {
                        // Settings & Transparency Tab
                        SettingsScreen(
                            isBackgroundEnabled = isBackgroundEnabled,
                            onToggleBackground = { vm.toggleBackgroundWorker(it) },
                            isDetectiveDutyActive = isDetectiveDutyActive,
                            onToggleDetectiveDuty = { vm.toggleDetectiveDuty(it) },
                            lastBackgroundTs = lastBackgroundTs,
                            hasUsagePermission = hasUsagePermission,
                            onCheckUsagePermission = { vm.checkUsagePermission() },
                            onSeedDemoData = { vm.seedDemoData() },
                            onClearData = { vm.clearData() },
                            totalReadingsCount = readings.size
                        )
                    }
                }
                }

                // Case File Dialog / Inspector
                selectedHour?.let { hour ->
                    val stat = summary.hourlyStats[hour]
                    if (stat != null) {
                        CaseFileDialog(
                            hour = hour,
                            stat = stat,
                            avgHourlyDrop = summary.averageHourlyDrop,
                            suspects = suspectApps,
                            hasUsagePermission = hasUsagePermission,
                            onDismiss = { vm.closeCaseFile() }
                        )
                    }
                }
            }
        }
    }
}
