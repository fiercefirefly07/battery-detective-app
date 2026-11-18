package com.draindetective.alert

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.draindetective.MainActivity
import com.draindetective.domain.DeviceContext

object PocketCookingNotifier {

    private const val CHANNEL_ID = "pocket_cooking_alerts"
    private const val NOTIFICATION_ID = 4040
    private const val PREFS_NAME = "pocket_cooking_prefs"
    private const val KEY_LAST_ALERT_TS = "last_alert_timestamp"

    /** Minimum temperature in °C to trigger pocket cooking warning. */
    const val POCKET_COOKING_TEMP_THRESHOLD: Float = 37.5f

    /** Minimum drop in % across a reading interval to consider acute drain. */
    const val POCKET_COOKING_MIN_DROP: Int = 2

    /** Cooldown duration between pocket cooking alerts (30 minutes). */
    private const val COOLDOWN_MS: Long = 30 * 60 * 1000L

    fun checkAndNotify(
        context: Context,
        deviceContext: DeviceContext,
        temperature: Float,
        recentDrop: Int
    ) {
        if (deviceContext != DeviceContext.IN_POCKET_OR_BAG) return
        if (temperature < POCKET_COOKING_TEMP_THRESHOLD) return
        if (recentDrop < POCKET_COOKING_MIN_DROP) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastAlert = prefs.getLong(KEY_LAST_ALERT_TS, 0L)
        val now = System.currentTimeMillis()

        if (now - lastAlert < COOLDOWN_MS) {
            // Within cooldown window
            return
        }

        // Check POST_NOTIFICATIONS permission on Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            createNotificationChannel(nm)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentTitle("🔥 Pocket Cooking Alert!")
                .setContentText("Phone is ${"%.1f".format(temperature)}°C and draining fast (-$recentDrop%) inside your pocket/bag. Remove to cool down!")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "Your phone is experiencing rapid battery drain (-$recentDrop%) while trapped in an enclosed pocket or bag with rising temperature (${"%.1f".format(temperature)}°C). Check for rogue apps or screen wake-locks."
                    )
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
            prefs.edit().putLong(KEY_LAST_ALERT_TS, now).apply()
        } catch (_: Exception) {
            // Never crash if notifications fail
        }
    }

    private fun createNotificationChannel(nm: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pocket Cooking Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical warnings when phone is overheating and draining inside a pocket or bag."
            }
            nm.createNotificationChannel(channel)
        }
    }
}
