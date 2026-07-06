package com.example.samsungzh

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AiDebugLogDumper {
    fun recordFailure(
        context: Context,
        reason: String,
        rawOutput: String? = null,
        validEntryCount: Int? = null,
        error: Throwable? = null,
    ) {
        val appContext = context.applicationContext
        val prefs = AiLabPreferences(appContext)
        val now = System.currentTimeMillis()
        prefs.pendingDebugLog = buildLog(
            context = appContext,
            reason = reason,
            rawOutput = rawOutput,
            validEntryCount = validEntryCount,
            error = error,
            nowMillis = now,
        )
        prefs.lastFailureLogId = now
    }

    fun savePendingLog(context: Context): File? {
        val appContext = context.applicationContext
        val log = AiLabPreferences(appContext).pendingDebugLog.ifBlank { return null }
        val directory = logDirectory(appContext).apply { mkdirs() }
        val file = File(directory, "flipwords-ai-log-${fileTimestamp()}.txt")
        file.writeText(log)
        return file
    }

    fun logDirectory(context: Context): File {
        val documentsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        return File(documentsDir ?: context.filesDir, "FlipWordsLogs")
    }

    private fun buildLog(
        context: Context,
        reason: String,
        rawOutput: String?,
        validEntryCount: Int?,
        error: Throwable?,
        nowMillis: Long,
    ): String {
        val prefs = AiLabPreferences(context)
        val modelManager = AiModelManager(context)
        val modelFile = modelManager.modelFile()
        val rawPreview = rawOutput
            ?.take(MAX_RAW_OUTPUT_CHARS)
            ?.ifBlank { "(blank output)" }
            ?: "(not available)"

        return buildString {
            appendLine("FlipWords AI Generation Debug Log")
            appendLine("Created: ${displayTimestamp(nowMillis)}")
            appendLine()
            appendLine("Failure")
            appendLine("Reason: $reason")
            appendLine("Valid entries accepted: ${validEntryCount ?: "not available"}")
            appendLine()
            appendLine("App State")
            appendLine("Generated status: ${prefs.generatedStatus}")
            appendLine("Generated count: ${prefs.generatedCount}")
            appendLine("Source mode: ${prefs.sourceMode.label}")
            appendLine("HSK level: ${prefs.hskLevel.label}")
            appendLine("Daily generation enabled: ${prefs.dailyGenerationEnabled}")
            appendLine("Daily generation time: ${String.format(Locale.US, "%02d:%02d", prefs.dailyGenerationHour, prefs.dailyGenerationMinute)}")
            appendLine("Last scheduled generation millis: ${prefs.lastScheduledGenerationMillis}")
            appendLine()
            appendLine("Model")
            appendLine("Repository: ${AiModelManager.MODEL_REPOSITORY}")
            appendLine("File name: ${AiModelManager.MODEL_FILE_NAME}")
            appendLine("Model status: ${prefs.modelStatus}")
            appendLine("Model path: ${modelFile.absolutePath}")
            appendLine("Model exists: ${modelFile.exists()}")
            appendLine("Model bytes: ${if (modelFile.exists()) modelFile.length() else 0L}")
            appendLine("Download id: ${prefs.modelDownloadId}")
            appendLine()
            appendLine("Device")
            appendLine("SDK: ${Build.VERSION.SDK_INT}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
            appendLine()
            appendLine("Raw Model Output Preview")
            appendLine(rawPreview)
            if (rawOutput != null && rawOutput.length > MAX_RAW_OUTPUT_CHARS) {
                appendLine()
                appendLine("Raw output truncated to $MAX_RAW_OUTPUT_CHARS characters.")
            }
            if (error != null) {
                appendLine()
                appendLine("Exception")
                appendLine(error.stackTraceText())
            }
        }
    }

    private fun Throwable.stackTraceText(): String {
        val writer = StringWriter()
        printStackTrace(PrintWriter(writer))
        return writer.toString()
    }

    private fun fileTimestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())

    private fun displayTimestamp(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date(millis))

    private const val MAX_RAW_OUTPUT_CHARS = 12_000
}
