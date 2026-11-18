package com.draindetective.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.draindetective.DrainApp
import com.draindetective.MainActivity
import com.draindetective.domain.DeviceContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Opt-in Foreground Service for continuous situational sensor sampling ("Detective on Duty").
 * Android restricts sensors for standard background processes; this foreground service provides
 * persistent sampling with a clear user notification and explanation.
 */
class DetectiveOnDutyService : Service() {

    private val serviceJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        _isServiceActive.value = true
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildDutyNotification("Initializing forensic sensors..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            val app = applicationContext as? DrainApp ?: return@launch
            while (isActive) {
                val reading = app.repo.saveReading(isBackground = false, sampleSensors = true)
                val contextObj = DeviceContext.fromLabel(reading.contextLabel)
                val statusText = "Last sampled: ${reading.level}% (${contextObj.displayName}) • ${"%.1f".format(reading.temperature)}°C"
                updateNotification(statusText)

                // 15-minute sampling interval
                delay(TimeUnit.MINUTES.toMillis(15))
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        _isServiceActive.value = false
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.notify(NOTIFICATION_ID, buildDutyNotification(text))
    }

    private fun buildDutyNotification(statusText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, DetectiveOnDutyService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("🕵️ Detective on Duty")
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Detective on Duty is actively sampling physical context (proximity, light, motion) every 15 min.\n$statusText"))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Duty", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Detective on Duty Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing persistent notification while continuous forensic sampling is active."
            }
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_STOP = "com.draindetective.action.STOP_DUTY"
        private const val CHANNEL_ID = "detective_duty_channel"
        private const val NOTIFICATION_ID = 5050

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, DetectiveOnDutyService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isServiceActive.value = true
        }

        fun stop(context: Context) {
            val intent = Intent(context, DetectiveOnDutyService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
            _isServiceActive.value = false
        }
    }
}
