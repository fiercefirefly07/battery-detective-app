package com.draindetective.usage

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Process
import android.provider.Settings
import java.util.concurrent.TimeUnit

data class SuspectApp(
    val packageName: String,
    val appName: String,
    val foregroundMinutes: Int,
    val icon: Drawable?
)

object UsageTracker {

    /**
     * Checks if the app has been granted PACKAGE_USAGE_STATS permission via AppOps.
     */
    fun hasUsagePermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
                ?: return false
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns an intent to open the system Usage Access Settings.
     */
    fun createUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Queries foreground apps active during the given hour window [startMs, endMs].
     * Returns the top 3 foreground apps with readable name, duration in minutes, and icon.
     * Never crashes; returns empty list if permission is missing or query fails.
     */
    fun querySuspectApps(
        context: Context,
        startMs: Long,
        endMs: Long
    ): List<SuspectApp> {
        if (!hasUsagePermission(context)) {
            return emptyList()
        }

        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList()

            val statsList: List<UsageStats> = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST,
                startMs,
                endMs
            ) ?: emptyList()

            val pm = context.packageManager
            val ownPackage = context.packageName

            // Aggregate foreground time and filter out system/irrelevant apps
            statsList
                .filter { it.totalTimeInForeground > 30_000L && it.packageName != ownPackage }
                .sortedByDescending { it.totalTimeInForeground }
                .take(3)
                .map { stat ->
                    val (label, icon) = resolveAppInfo(pm, stat.packageName)
                    val minutes = (stat.totalTimeInForeground / 60_000L).toInt().coerceAtLeast(1)
                    SuspectApp(
                        packageName = stat.packageName,
                        appName = label,
                        foregroundMinutes = minutes,
                        icon = icon
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun resolveAppInfo(pm: PackageManager, packageName: String): Pair<String, Drawable?> {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)
            label to icon
        } catch (_: Exception) {
            packageName.substringAfterLast('.') to null
        }
    }
}
