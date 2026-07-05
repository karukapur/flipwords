package com.example.samsungzh

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WordUpdateScheduler {
    private const val UNIQUE_WORK_NAME = "chinese_word_rotation"
    private const val MIN_PERIODIC_WORK_MINUTES = 15L

    fun schedule(context: Context) {
        val intervalMinutes = (OverlayPreferences(context).rotationIntervalSeconds.toLong() + 59L) / 60L
        val repeatMinutes = maxOf(MIN_PERIODIC_WORK_MINUTES, intervalMinutes)
        val request = PeriodicWorkRequestBuilder<WordUpdateWorker>(
            repeatMinutes,
            TimeUnit.MINUTES,
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
