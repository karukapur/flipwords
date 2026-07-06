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
        val downloadProgress = queryDownloadProgress()
        if (downloadProgress != null) {
            prefs.modelStatus = downloadProgress.status
            return prefs.modelStatus
        }

        val file = modelFile()
        prefs.modelStatus = if (file.exists() && file.length() >= MIN_MODEL_BYTES) {
            AiLabPreferences.MODEL_READY
        } else {
            AiLabPreferences.MODEL_NOT_DOWNLOADED
        }
        return prefs.modelStatus
    }

    fun downloadProgress(): AiModelDownloadProgress {
        queryDownloadProgress()?.let { return it }

        val file = modelFile()
        val status = if (file.exists() && file.length() >= MIN_MODEL_BYTES) {
            AiLabPreferences.MODEL_READY
        } else {
            prefs.modelStatus
        }

        return AiModelDownloadProgress(
            status = status,
            downloadedBytes = file.length().coerceAtLeast(0L),
            totalBytes = if (status == AiLabPreferences.MODEL_READY) file.length() else MIN_MODEL_BYTES,
        )
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

    private fun queryDownloadProgress(): AiModelDownloadProgress? {
        val downloadId = prefs.modelDownloadId
        if (downloadId == AiLabPreferences.NO_DOWNLOAD_ID) return null

        downloadManager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
            if (!cursor.moveToFirst()) return null

            val statusCode = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val file = modelFile()
            val downloaded = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
            ).takeIf { it >= 0L } ?: file.length().coerceAtLeast(0L)
            val reportedTotal = cursor.getLong(
                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
            )
            val status = when (statusCode) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (file.exists() && file.length() >= MIN_MODEL_BYTES) {
                        AiLabPreferences.MODEL_READY
                    } else {
                        AiLabPreferences.MODEL_FAILED
                    }
                }

                else -> statusLabel(statusCode)
            }
            val total = when {
                reportedTotal > 0L -> reportedTotal
                statusCode == DownloadManager.STATUS_SUCCESSFUL && file.exists() -> file.length()
                else -> MIN_MODEL_BYTES
            }

            return AiModelDownloadProgress(
                status = status,
                downloadedBytes = downloaded,
                totalBytes = total,
            )
        }

        return null
    }

    private fun statusLabel(status: Int): String =
        when (status) {
            DownloadManager.STATUS_RUNNING,
            DownloadManager.STATUS_PENDING,
            DownloadManager.STATUS_PAUSED -> AiLabPreferences.MODEL_DOWNLOADING

            DownloadManager.STATUS_SUCCESSFUL -> AiLabPreferences.MODEL_READY
            DownloadManager.STATUS_FAILED -> AiLabPreferences.MODEL_FAILED
            else -> AiLabPreferences.MODEL_NOT_DOWNLOADED
        }
}

data class AiModelDownloadProgress(
    val status: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
) {
    val percent: Int
        get() = if (totalBytes <= 0L) 0 else ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)

    val isDownloading: Boolean
        get() = status == AiLabPreferences.MODEL_DOWNLOADING
}
