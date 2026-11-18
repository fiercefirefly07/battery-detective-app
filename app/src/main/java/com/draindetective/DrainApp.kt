package com.draindetective

import android.app.Application
import com.draindetective.data.BatteryRepository
import com.draindetective.worker.RecordWorker

class DrainApp : Application() {

    val repo by lazy { BatteryRepository(this) }

    override fun onCreate() {
        super.onCreate()
        if (repo.isBackgroundWorkerEnabled) {
            RecordWorker.schedule(this)
        }
    }
}
