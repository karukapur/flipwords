package com.example.samsungzh

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WordUpdateScheduler {
    private const val UNIQUE_WORK_NAME = "flipwords_adaptive_scheduler"
    private const val MIN_PERIODIC_WORK_MINUTES = 15L

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<WordUpdateWorker>(
            MIN_PERIODIC_WORK_MINUTES,
            TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
