package com.example.samsungzh

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

class AiModelManager(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = AiLabPreferences(appContext)
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)

    fun modelFile(): File {
        val dir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: appContext.filesDir
        return File(dir, MODEL_FILE_NAME)
    }

    fun refreshStatus(): String {
        val file = modelFile()
        if (file.exists() && file.length() >= MIN_MODEL_BYTES) {
            prefs.modelStatus = AiLabPreferences.MODEL_READY
            return prefs.modelStatus
        }

        val downloadId = prefs.modelDownloadId
        if (downloadId != AiLabPreferences.NO_DOWNLOAD_ID) {
            downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    prefs.modelStatus = when (status) {
                        DownloadManager.STATUS_RUNNING,
                        DownloadManager.STATUS_PENDING,
                        DownloadManager.STATUS_PAUSED -> AiLabPreferences.MODEL_DOWNLOADING

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            if (file.exists() && file.length() >= MIN_MODEL_BYTES) {
                                AiLabPreferences.MODEL_READY
                            } else {
                                AiLabPreferences.MODEL_FAILED
                            }
                        }

                        DownloadManager.STATUS_FAILED -> AiLabPreferences.MODEL_FAILED
                        else -> AiLabPreferences.MODEL_NOT_DOWNLOADED
                    }
                    return prefs.modelStatus
                }
            }
        }

        prefs.modelStatus = AiLabPreferences.MODEL_NOT_DOWNLOADED
        return prefs.modelStatus
    }

    fun startDownload(): Long {
        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("FlipWords AI model")
            .setDescription("Downloading $MODEL_FILE_NAME")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                MODEL_FILE_NAME,
            )

        val id = downloadManager.enqueue(request)
        prefs.modelDownloadId = id
        prefs.modelStatus = AiLabPreferences.MODEL_DOWNLOADING
        return id
    }

    fun isReady(): Boolean = refreshStatus() == AiLabPreferences.MODEL_READY

    companion object {
        const val MODEL_FILE_NAME = "gemma-4-E2B-it.litertlm"
        const val MODEL_REPOSITORY = "litert-community/gemma-4-E2B-it-litert-lm"
        const val MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm?download=true"

        private const val MIN_MODEL_BYTES = 2L * 1024L * 1024L * 1024L
    }
}
