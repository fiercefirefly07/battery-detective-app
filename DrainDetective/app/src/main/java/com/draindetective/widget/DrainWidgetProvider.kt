package com.draindetective.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.draindetective.DrainApp
import com.draindetective.MainActivity
import com.draindetective.R
import com.draindetective.domain.DrainAnalyzer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DrainWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val app = context.applicationContext as? DrainApp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val readings = app.repo.readings.firstOrNull() ?: emptyList()
            val latest = readings.firstOrNull() ?: app.repo.captureCurrentReading()
            val summary = DrainAnalyzer.analyze(readings)

            val levelText = "${latest.level}%"
            val drainText = if (latest.isCharging) {
                "⚡ Charging (${latest.pluggedType})"
            } else if (summary.averageHourlyDrop > 0f) {
                "Avg: -${"%.1f".format(summary.averageHourlyDrop)}%/h"
            } else {
                "Discharging normally"
            }

            val estText = if (latest.isCharging) {
                "Plugged in"
            } else if (summary.estimatedHoursToEmpty != null) {
                "Est: ${"%.1f".format(summary.estimatedHoursToEmpty)}h left"
            } else {
                "Calibrating rate..."
            }

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_drain)
                views.setTextViewText(R.id.widget_level, levelText)
                views.setTextViewText(R.id.widget_drain_rate, drainText)
                views.setTextViewText(R.id.widget_time_to_empty, estText)
                views.setTextViewText(R.id.widget_status, if (latest.isCharging) "Charging" else "Discharging")

                val intent = Intent(context, MainActivity::class.java)
                val pi = PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, pi)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            val intent = Intent(context, DrainWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, DrainWidgetProvider::class.java)
                )
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
