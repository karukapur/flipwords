package com.example.samsungzh

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AiGenerationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AiLabPreferences(context).dailyGenerationEnabled) return

        AiVocabularyGenerationWorker.enqueue(context)
        AiGenerationScheduler.scheduleNext(context)
    }
}
