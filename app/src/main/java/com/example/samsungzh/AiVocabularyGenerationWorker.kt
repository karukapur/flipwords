package com.example.samsungzh

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

class AiVocabularyGenerationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val prefs = AiLabPreferences(appContext)
    private val modelManager = AiModelManager(appContext)
    private val store = GeneratedVocabularyStore(appContext)

    override suspend fun doWork(): Result {
        createNotificationChannel()
        setForeground(generationForegroundInfo("Generating vocabulary..."))
        prefs.generatedStatus = "${AiLabPreferences.GENERATED_RUNNING}: checking model"

        if (!modelManager.isReady()) {
            prefs.generatedStatus = "${AiLabPreferences.GENERATED_FAILED}: model not ready"
            AiDebugLogDumper.recordFailure(
                context = applicationContext,
                reason = "Model was not ready when generation started.",
            )
            return Result.failure()
        }

        return try {
            val now = System.currentTimeMillis()
            prefs.generatedStatus = "${AiLabPreferences.GENERATED_RUNNING}: ${prefs.hskLevel.label}, loading model"
            val rawOutput = LiteRtVocabularyGenerator().generate(modelManager.modelFile(), prefs.hskLevel)
            prefs.generatedStatus = "${AiLabPreferences.GENERATED_RUNNING}: validating entries"
            val entries = GeneratedVocabularyValidator.parseValidEntries(rawOutput, now)
            if (entries.size < GeneratedVocabularyValidator.REQUIRED_BATCH_SIZE) {
                prefs.generatedStatus =
                    "${AiLabPreferences.GENERATED_FAILED}: ${entries.size}/50 valid entries"
                AiDebugLogDumper.recordFailure(
                    context = applicationContext,
                    reason = "Generated output did not contain 50 valid entries.",
                    rawOutput = rawOutput,
                    validEntryCount = entries.size,
                )
                return Result.failure()
            }

            store.saveEntries(entries)
            prefs.generatedStatus = "${AiLabPreferences.GENERATED_READY}: ${entries.size} entries"
            refreshOverlayIfNeeded()
            notifyComplete("Generated ${entries.size} entries")
            Result.success()
        } catch (error: Throwable) {
            prefs.generatedStatus =
                "${AiLabPreferences.GENERATED_FAILED}: ${error.message ?: error::class.java.simpleName}"
            AiDebugLogDumper.recordFailure(
                context = applicationContext,
                reason = "LiteRT-LM generation threw an exception.",
                error = error,
            )
            Result.failure()
        }
    }

    private fun notifyComplete(text: String) {
        applicationContext.getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun generationForegroundInfo(text: String): ForegroundInfo {
        val notification = buildNotification(text)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun refreshOverlayIfNeeded() {
        if (!OverlayPreferences(applicationContext).overlayEnabled) return

        applicationContext.startService(
            Intent(applicationContext, CoverOverlayService::class.java)
                .setAction(CoverOverlayActions.ACTION_REFRESH),
        )
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = android.app.PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("FlipWords AI Lab")
            .setContentText(text)
            .setOngoing(false)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "AI vocabulary generation",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows local AI vocabulary generation progress."
        }
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "ai_vocabulary_generation"
        private const val CHANNEL_ID = "ai_vocabulary_generation"
        private const val NOTIFICATION_ID = 5108

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<AiVocabularyGenerationWorker>().build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request,
            )
        }
    }
}
