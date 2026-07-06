package com.example.samsungzh

import android.content.Context

class AiLabPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var sourceMode: AiVocabularySourceMode
        get() = AiVocabularySourceMode.fromStoredValue(prefs.getString(KEY_SOURCE_MODE, null))
        set(value) = prefs.edit().putString(KEY_SOURCE_MODE, value.storedValue).apply()

    var hskLevel: AiHskLevel
        get() = AiHskLevel.fromStoredValue(prefs.getString(KEY_HSK_LEVEL, null))
        set(value) = prefs.edit().putString(KEY_HSK_LEVEL, value.storedValue).apply()

    var dailyGenerationHour: Int
        get() = prefs.getInt(KEY_DAILY_GENERATION_HOUR, DEFAULT_DAILY_GENERATION_HOUR)
        set(value) = prefs.edit().putInt(KEY_DAILY_GENERATION_HOUR, value.coerceIn(0, 23)).apply()

    var dailyGenerationMinute: Int
        get() = prefs.getInt(KEY_DAILY_GENERATION_MINUTE, DEFAULT_DAILY_GENERATION_MINUTE)
        set(value) = prefs.edit().putInt(KEY_DAILY_GENERATION_MINUTE, value.coerceIn(0, 59)).apply()

    var dailyGenerationEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_GENERATION_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_DAILY_GENERATION_ENABLED, value).apply()

    var modelDownloadId: Long
        get() = prefs.getLong(KEY_MODEL_DOWNLOAD_ID, NO_DOWNLOAD_ID)
        set(value) = prefs.edit().putLong(KEY_MODEL_DOWNLOAD_ID, value).apply()

    var modelStatus: String
        get() = prefs.getString(KEY_MODEL_STATUS, MODEL_NOT_DOWNLOADED) ?: MODEL_NOT_DOWNLOADED
        set(value) = prefs.edit().putString(KEY_MODEL_STATUS, value).apply()

    var generatedStatus: String
        get() = prefs.getString(KEY_GENERATED_STATUS, GENERATED_EMPTY) ?: GENERATED_EMPTY
        set(value) = prefs.edit().putString(KEY_GENERATED_STATUS, value).apply()

    var generatedCount: Int
        get() = prefs.getInt(KEY_GENERATED_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_GENERATED_COUNT, value.coerceAtLeast(0)).apply()

    var lastGenerationMillis: Long
        get() = prefs.getLong(KEY_LAST_GENERATION_MILLIS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_GENERATION_MILLIS, value.coerceAtLeast(0L)).apply()

    var lastScheduledGenerationMillis: Long
        get() = prefs.getLong(KEY_LAST_SCHEDULED_GENERATION_MILLIS, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SCHEDULED_GENERATION_MILLIS, value.coerceAtLeast(0L)).apply()

    var pendingDebugLog: String
        get() = prefs.getString(KEY_PENDING_DEBUG_LOG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PENDING_DEBUG_LOG, value).apply()

    var lastFailureLogId: Long
        get() = prefs.getLong(KEY_LAST_FAILURE_LOG_ID, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_FAILURE_LOG_ID, value.coerceAtLeast(0L)).apply()

    var promptedFailureLogId: Long
        get() = prefs.getLong(KEY_PROMPTED_FAILURE_LOG_ID, 0L)
        set(value) = prefs.edit().putLong(KEY_PROMPTED_FAILURE_LOG_ID, value.coerceAtLeast(0L)).apply()

    companion object {
        private const val PREFS_NAME = "ai_lab_preferences"
        private const val KEY_SOURCE_MODE = "source_mode"
        private const val KEY_HSK_LEVEL = "hsk_level"
        private const val KEY_DAILY_GENERATION_HOUR = "daily_generation_hour"
        private const val KEY_DAILY_GENERATION_MINUTE = "daily_generation_minute"
        private const val KEY_DAILY_GENERATION_ENABLED = "daily_generation_enabled"
        private const val KEY_MODEL_DOWNLOAD_ID = "model_download_id"
        private const val KEY_MODEL_STATUS = "model_status"
        private const val KEY_GENERATED_STATUS = "generated_status"
        private const val KEY_GENERATED_COUNT = "generated_count"
        private const val KEY_LAST_GENERATION_MILLIS = "last_generation_millis"
        private const val KEY_LAST_SCHEDULED_GENERATION_MILLIS = "last_scheduled_generation_millis"
        private const val KEY_PENDING_DEBUG_LOG = "pending_debug_log"
        private const val KEY_LAST_FAILURE_LOG_ID = "last_failure_log_id"
        private const val KEY_PROMPTED_FAILURE_LOG_ID = "prompted_failure_log_id"

        private const val DEFAULT_DAILY_GENERATION_HOUR = 2
        private const val DEFAULT_DAILY_GENERATION_MINUTE = 0
        const val NO_DOWNLOAD_ID = -1L

        const val MODEL_NOT_DOWNLOADED = "Model: not downloaded"
        const val MODEL_DOWNLOADING = "Model: downloading"
        const val MODEL_READY = "Model: ready"
        const val MODEL_FAILED = "Model: download failed"

        const val GENERATED_EMPTY = "Generated pack: empty"
        const val GENERATED_RUNNING = "Generated pack: generation running"
        const val GENERATED_READY = "Generated pack: ready"
        const val GENERATED_FAILED = "Generated pack: generation failed"
    }
}
