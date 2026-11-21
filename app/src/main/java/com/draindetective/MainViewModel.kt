package com.draindetective

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.draindetective.data.BatteryReading
import com.draindetective.data.Incident
import com.draindetective.domain.AnalysisSummary
import com.draindetective.domain.DrainAnalyzer
import com.draindetective.domain.GhostRaceAnalyzer
import com.draindetective.domain.GhostRaceSummary
import com.draindetective.service.DetectiveOnDutyService
import com.draindetective.usage.SuspectApp
import com.draindetective.usage.UsageTracker
import com.draindetective.widget.DrainWidgetProvider
import com.draindetective.worker.RecordWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {

    val repo = (app as DrainApp).repo

    val readings: StateFlow<List<BatteryReading>> = repo.readings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incidents: StateFlow<List<Incident>> = repo.incidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val summary: StateFlow<AnalysisSummary> = readings
        .map { list -> DrainAnalyzer.analyze(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DrainAnalyzer.analyze(emptyList()))

    val ghostRaceSummary: StateFlow<GhostRaceSummary> = readings
        .map { list -> GhostRaceAnalyzer.analyze(list) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GhostRaceAnalyzer.analyze(emptyList()))

    private val _isForegroundRecording = MutableStateFlow(true)
    val isForegroundRecording: StateFlow<Boolean> = _isForegroundRecording.asStateFlow()

    private val _isBackgroundWorkerEnabled = MutableStateFlow(repo.isBackgroundWorkerEnabled)
    val isBackgroundWorkerEnabled: StateFlow<Boolean> = _isBackgroundWorkerEnabled.asStateFlow()

    private val _isDetectiveDutyActive = MutableStateFlow(repo.isDetectiveDutyEnabled)
    val isDetectiveDutyActive: StateFlow<Boolean> = _isDetectiveDutyActive.asStateFlow()

    private val _lastBackgroundReadingTime = MutableStateFlow(repo.lastBackgroundReadingTime)
    val lastBackgroundReadingTime: StateFlow<Long> = _lastBackgroundReadingTime.asStateFlow()

    private val _selectedHour = MutableStateFlow<Int?>(null)
    val selectedHour: StateFlow<Int?> = _selectedHour.asStateFlow()

    private val _suspectApps = MutableStateFlow<List<SuspectApp>>(emptyList())
    val suspectApps: StateFlow<List<SuspectApp>> = _suspectApps.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(UsageTracker.hasUsagePermission(app))
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    val isRecordingBlackBoxLive: StateFlow<Boolean> = repo.blackBox.isRecordingIncident
    val blackBoxLiveProgress: StateFlow<Float?> = repo.blackBox.currentIncidentProgress

    init {
        // Automatic foreground recording every 15 minutes tied to ViewModel lifecycle
        viewModelScope.launch {
            _isForegroundRecording.collectLatest { active ->
                while (active) {
                    saveManualReading()
                    delay(15 * 60_000L)
                }
            }
        }
    }

    fun saveManualReading() {
        viewModelScope.launch {
            repo.saveReading(isBackground = false, sampleSensors = true)
            DrainWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun toggleForegroundRecording() {
        _isForegroundRecording.value = !_isForegroundRecording.value
    }

    fun toggleBackgroundWorker(enable: Boolean) {
        repo.isBackgroundWorkerEnabled = enable
        _isBackgroundWorkerEnabled.value = enable
        if (enable) {
            RecordWorker.schedule(getApplication())
        } else {
            RecordWorker.cancel(getApplication())
        }
    }

    fun toggleDetectiveDuty(enable: Boolean) {
        repo.isDetectiveDutyEnabled = enable
        _isDetectiveDutyActive.value = enable
        if (enable) {
            DetectiveOnDutyService.start(getApplication())
        } else {
            DetectiveOnDutyService.stop(getApplication())
        }
    }

    fun selectHour(hour: Int) {
        _selectedHour.value = hour
        loadSuspectAppsForHour(hour)
    }

    fun closeCaseFile() {
        _selectedHour.value = null
        _suspectApps.value = emptyList()
    }

    fun checkUsagePermission() {
        _hasUsagePermission.value = UsageTracker.hasUsagePermission(getApplication())
        _selectedHour.value?.let { loadSuspectAppsForHour(it) }
    }

    private fun loadSuspectAppsForHour(hour: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val hasPerm = UsageTracker.hasUsagePermission(getApplication())
            _hasUsagePermission.value = hasPerm
            if (!hasPerm) {
                _suspectApps.value = emptyList()
                return@launch
            }

            val currentReadings = readings.value
            val matchingReadings = currentReadings.filter { reading ->
                val cal = Calendar.getInstance().apply { timeInMillis = reading.timestamp }
                cal.get(Calendar.HOUR_OF_DAY) == hour
            }

            val (startMs, endMs) = if (matchingReadings.isNotEmpty()) {
                val minTs = matchingReadings.minOf { it.timestamp }
                val calStart = Calendar.getInstance().apply {
                    timeInMillis = minTs
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = calStart.timeInMillis
                start to (start + TimeUnit.HOURS.toMillis(1))
            } else {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val start = cal.timeInMillis
                start to (start + TimeUnit.HOURS.toMillis(1))
            }

            val suspects = UsageTracker.querySuspectApps(getApplication(), startMs, endMs)
            _suspectApps.value = suspects
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            repo.seedDemoData()
            _lastBackgroundReadingTime.value = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(12)
            repo.lastBackgroundReadingTime = _lastBackgroundReadingTime.value
            DrainWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun clearData() {
        viewModelScope.launch {
            repo.clearAll()
            _selectedHour.value = null
            _suspectApps.value = emptyList()
            DrainWidgetProvider.triggerUpdate(getApplication())
        }
    }
}
