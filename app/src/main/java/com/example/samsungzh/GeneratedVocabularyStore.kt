package com.example.samsungzh

import android.content.Context
import java.io.File

class GeneratedVocabularyStore(context: Context) {
    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, FILE_NAME)

    fun loadEntries(): List<GeneratedVocabularyEntry> {
        if (!file.exists()) return emptyList()
        return try {
            GeneratedVocabularyValidator.fromJson(file.readText())
        } catch (_: RuntimeException) {
            emptyList()
        }
    }

    fun saveEntries(entries: List<GeneratedVocabularyEntry>) {
        file.writeText(GeneratedVocabularyValidator.toJson(entries))
        val prefs = AiLabPreferences(appContext)
        prefs.generatedCount = entries.size
        prefs.lastGenerationMillis = entries.maxOfOrNull { it.createdAtMillis } ?: System.currentTimeMillis()
        prefs.generatedStatus = AiLabPreferences.GENERATED_READY
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
        val prefs = AiLabPreferences(appContext)
        prefs.generatedCount = 0
        prefs.generatedStatus = AiLabPreferences.GENERATED_EMPTY
    }

    companion object {
        private const val FILE_NAME = "generated_vocabulary.json"
    }
}
