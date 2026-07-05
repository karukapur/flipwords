package com.example.samsungzh

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class WordUpdateWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        WordRepository(applicationContext).currentWord()
        return Result.success()
    }
}
