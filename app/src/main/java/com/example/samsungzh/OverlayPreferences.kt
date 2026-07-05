package com.example.samsungzh

import android.content.Context

class OverlayPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var hanziSizeSp: Int
        get() = prefs.getInt(KEY_HANZI_SIZE, DEFAULT_HANZI_SIZE)
        set(value) = prefs.edit().putInt(KEY_HANZI_SIZE, value.coerceIn(MIN_HANZI_SIZE, MAX_HANZI_SIZE)).apply()

    var pinyinSizeSp: Int
        get() = prefs.getInt(KEY_PINYIN_SIZE, DEFAULT_PINYIN_SIZE)
        set(value) = prefs.edit().putInt(KEY_PINYIN_SIZE, value.coerceIn(MIN_PINYIN_SIZE, MAX_PINYIN_SIZE)).apply()

    var englishSizeSp: Int
        get() = prefs.getInt(KEY_ENGLISH_SIZE, DEFAULT_ENGLISH_SIZE)
        set(value) = prefs.edit().putInt(KEY_ENGLISH_SIZE, value.coerceIn(MIN_ENGLISH_SIZE, MAX_ENGLISH_SIZE)).apply()

    var hanziColor: Int
        get() = prefs.getInt(KEY_HANZI_COLOR, DEFAULT_HANZI_COLOR)
        set(value) = prefs.edit().putInt(KEY_HANZI_COLOR, value).apply()

    var pinyinColor: Int
        get() = prefs.getInt(KEY_PINYIN_COLOR, DEFAULT_PINYIN_COLOR)
        set(value) = prefs.edit().putInt(KEY_PINYIN_COLOR, value).apply()

    var englishColor: Int
        get() = prefs.getInt(KEY_ENGLISH_COLOR, DEFAULT_ENGLISH_COLOR)
        set(value) = prefs.edit().putInt(KEY_ENGLISH_COLOR, value).apply()

    var overlayStatus: String
        get() = prefs.getString(KEY_OVERLAY_STATUS, STATUS_IDLE) ?: STATUS_IDLE
        set(value) = prefs.edit().putString(KEY_OVERLAY_STATUS, value).apply()

    var overlayEnabled: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ENABLED, value).apply()

    var rotationIntervalSeconds: Int
        get() = prefs.getInt(KEY_ROTATION_INTERVAL_SECONDS, DEFAULT_ROTATION_INTERVAL_SECONDS)
        set(value) = prefs.edit()
            .putInt(KEY_ROTATION_INTERVAL_SECONDS, value.coerceIn(MIN_ROTATION_INTERVAL_SECONDS, MAX_ROTATION_INTERVAL_SECONDS))
            .apply()

    val rotationIntervalMillis: Long
        get() = rotationIntervalSeconds * 1000L

    companion object {
        private const val PREFS_NAME = "overlay_preferences"
        private const val KEY_HANZI_SIZE = "hanzi_size_sp"
        private const val KEY_PINYIN_SIZE = "pinyin_size_sp"
        private const val KEY_ENGLISH_SIZE = "english_size_sp"
        private const val KEY_HANZI_COLOR = "hanzi_color"
        private const val KEY_PINYIN_COLOR = "pinyin_color"
        private const val KEY_ENGLISH_COLOR = "english_color"
        private const val KEY_OVERLAY_STATUS = "overlay_status"
        private const val KEY_OVERLAY_ENABLED = "overlay_enabled"
        private const val KEY_ROTATION_INTERVAL_SECONDS = "rotation_interval_seconds"

        const val DEFAULT_HANZI_SIZE = 30
        const val DEFAULT_PINYIN_SIZE = 13
        const val DEFAULT_ENGLISH_SIZE = 14

        const val DEFAULT_HANZI_COLOR = -593174
        const val DEFAULT_PINYIN_COLOR = -5257512
        const val DEFAULT_ENGLISH_COLOR = -2575254

        const val MIN_HANZI_SIZE = 18
        const val MAX_HANZI_SIZE = 56
        const val MIN_PINYIN_SIZE = 10
        const val MAX_PINYIN_SIZE = 28
        const val MIN_ENGLISH_SIZE = 10
        const val MAX_ENGLISH_SIZE = 32

        const val DEFAULT_ROTATION_INTERVAL_SECONDS = 90 * 60
        const val MIN_ROTATION_INTERVAL_SECONDS = 5
        const val MAX_ROTATION_INTERVAL_SECONDS = 90 * 60
        const val ROTATION_INTERVAL_STEP_SECONDS = 5

        const val STATUS_IDLE = "Overlay status: idle"
        const val STATUS_WAITING = "Overlay status: waiting for cover display. Main display fallback disabled."
    }
}
