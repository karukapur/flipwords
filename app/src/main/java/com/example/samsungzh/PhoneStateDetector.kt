package com.example.samsungzh

import android.content.Context
import android.content.Intent
import android.os.PowerManager

class PhoneStateDetector(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun recordSystemIntent(action: String?, nowMillis: Long = System.currentTimeMillis()) {
        when (action) {
            Intent.ACTION_SCREEN_ON,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_DREAMING_STOPPED -> recordInteractive(nowMillis)
            Intent.ACTION_SCREEN_OFF -> recordNonInteractive(nowMillis)
        }
    }

    fun recordFullAppOpen(nowMillis: Long = System.currentTimeMillis()) {
        recordInteractive(nowMillis)
        prefs.edit().putLong(KEY_LAST_FULL_APP_OPEN_AT, nowMillis).apply()
    }

    fun currentState(nowMillis: Long = System.currentTimeMillis()): PhoneLearningState {
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        val interactive = powerManager?.isInteractive ?: true
        val lastInteractiveAt = prefs.getLong(KEY_LAST_INTERACTIVE_AT, nowMillis)
        val lastNonInteractiveAt = prefs.getLong(KEY_LAST_NON_INTERACTIVE_AT, 0L)
        val lastFullAppOpenAt = prefs.getLong(KEY_LAST_FULL_APP_OPEN_AT, 0L)

        return when {
            lastFullAppOpenAt > 0L && nowMillis - lastFullAppOpenAt < FULL_APP_WINDOW_MILLIS ->
                PhoneLearningState.FULL_APP_LEARNING
            !interactive && lastNonInteractiveAt > 0L &&
                nowMillis - lastNonInteractiveAt >= SchedulerConfig.INACTIVITY_PAUSE_THRESHOLD_MILLIS ->
                PhoneLearningState.ASLEEP_OR_INACTIVE
            !interactive -> PhoneLearningState.LOCKED_IDLE
            nowMillis - lastInteractiveAt < USER_PRESENT_WINDOW_MILLIS ->
                PhoneLearningState.ACTIVE_PHONE_USE
            else -> PhoneLearningState.GLANCE_OPPORTUNITY
        }
    }

    private fun recordInteractive(nowMillis: Long) {
        prefs.edit().putLong(KEY_LAST_INTERACTIVE_AT, nowMillis).apply()
    }

    private fun recordNonInteractive(nowMillis: Long) {
        prefs.edit().putLong(KEY_LAST_NON_INTERACTIVE_AT, nowMillis).apply()
    }

    private companion object {
        const val PREFS_NAME = "phone_learning_state"
        const val KEY_LAST_INTERACTIVE_AT = "last_interactive_at"
        const val KEY_LAST_NON_INTERACTIVE_AT = "last_non_interactive_at"
        const val KEY_LAST_FULL_APP_OPEN_AT = "last_full_app_open_at"
        const val FULL_APP_WINDOW_MILLIS = 5L * SchedulerConfig.MILLIS_PER_MINUTE
        const val USER_PRESENT_WINDOW_MILLIS = 15L * SchedulerConfig.MILLIS_PER_MINUTE
    }
}
