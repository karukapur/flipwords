package com.example.samsungzh

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import java.util.Calendar

object AiGenerationScheduler {
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun scheduleNext(context: Context): Boolean {
        val appContext = context.applicationContext
        if (!canScheduleExact(appContext)) return false

        val prefs = AiLabPreferences(appContext)
        val triggerAtMillis = nextTriggerMillis(
            hour = prefs.dailyGenerationHour,
            minute = prefs.dailyGenerationMinute,
        )
        val alarmManager = appContext.getSystemService(AlarmManager::class.java)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(appContext),
        )
        prefs.lastScheduledGenerationMillis = triggerAtMillis
        return true
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        appContext.getSystemService(AlarmManager::class.java).cancel(pendingIntent(appContext))
        val prefs = AiLabPreferences(appContext)
        prefs.dailyGenerationEnabled = false
        prefs.lastScheduledGenerationMillis = 0L
    }

    fun permissionIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }

    private fun pendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, AiGenerationAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }

    private const val REQUEST_CODE = 5107
}
