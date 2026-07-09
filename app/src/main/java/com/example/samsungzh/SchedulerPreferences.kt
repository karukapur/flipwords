package com.example.samsungzh

import android.content.Context

class SchedulerPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var minimumSpacingMinutes: Long
        get() = prefs.getLong(KEY_MINIMUM_SPACING_MINUTES, SchedulerConfig.DEFAULT_MINIMUM_SPACING_MINUTES)
        set(value) = prefs.edit()
            .putLong(KEY_MINIMUM_SPACING_MINUTES, normalizeMinutes(value))
            .apply()

    val minimumSpacingMillis: Long
        get() = minimumSpacingMinutes * SchedulerConfig.MILLIS_PER_MINUTE

    companion object {
        private const val PREFS_NAME = "scheduler_preferences"
        private const val KEY_MINIMUM_SPACING_MINUTES = "minimum_spacing_minutes"

        const val MIN_MINIMUM_SPACING_MINUTES = 5L
        const val MAX_MINIMUM_SPACING_MINUTES = 24L * 60L

        fun normalizeMinutes(minutes: Long): Long =
            minutes.coerceIn(MIN_MINIMUM_SPACING_MINUTES, MAX_MINIMUM_SPACING_MINUTES)
    }
}
