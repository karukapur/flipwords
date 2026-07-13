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
        applyMetadata(metadataFor(entries), preserveStatus = false)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
        applyMetadata(GeneratedVocabularyMetadata.Empty, preserveStatus = false)
    }

    fun syncMetadata(preserveStatus: Boolean = false): GeneratedVocabularyMetadata {
        val metadata = metadataFor(loadEntries())
        applyMetadata(metadata, preserveStatus)
        return metadata
    }

    private fun applyMetadata(metadata: GeneratedVocabularyMetadata, preserveStatus: Boolean) {
        val prefs = AiLabPreferences(appContext)
        prefs.generatedCount = metadata.count
        prefs.lastGenerationMillis = metadata.lastGenerationMillis
        if (!preserveStatus) {
            prefs.generatedStatus = if (metadata.hasEntries) {
                AiLabPreferences.GENERATED_READY
            } else {
                AiLabPreferences.GENERATED_EMPTY
            }
        }
    }

    companion object {
        private const val FILE_NAME = "generated_vocabulary.json"

        fun metadataFor(entries: List<GeneratedVocabularyEntry>): GeneratedVocabularyMetadata =
            if (entries.isEmpty()) {
                GeneratedVocabularyMetadata.Empty
            } else {
                GeneratedVocabularyMetadata(
                    count = entries.size,
                    lastGenerationMillis = entries.maxOf { it.createdAtMillis }.coerceAtLeast(0L),
                )
            }
    }
}

data class GeneratedVocabularyMetadata(
    val count: Int,
    val lastGenerationMillis: Long,
) {
    val hasEntries: Boolean
        get() = count > 0

    companion object {
        val Empty = GeneratedVocabularyMetadata(count = 0, lastGenerationMillis = 0L)
    }
}
