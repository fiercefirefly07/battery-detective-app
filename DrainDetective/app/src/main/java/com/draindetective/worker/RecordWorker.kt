package com.draindetective.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.draindetective.DrainApp
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that captures a battery snapshot periodically in the background.
 * Battery-friendly and constraint-free so recording continues even in low battery states.
 */
class RecordWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DrainApp ?: return Result.failure()
        return try {
            app.repo.saveReading(isBackground = true)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "periodic_battery_drain_recorder"

        fun schedule(context: Context) {
            // 15-minute minimum interval per Android WorkManager specification
            // Deliberately constraint-free so critical low-battery drain events are not dropped
            val request = PeriodicWorkRequestBuilder<RecordWorker>(15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
