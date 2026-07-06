package com.example.samsungzh

import android.content.Context

class WordRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val overlayPreferences = OverlayPreferences(context)
    private val aiLabPreferences = AiLabPreferences(context)
    private val generatedVocabularyStore = GeneratedVocabularyStore(context)

    fun currentWord(nowMillis: Long = System.currentTimeMillis()): WordEntry {
        val words = activeWords()
        val anchor = ensureAnchor(nowMillis)
        val index = WordRotator.indexFor(
            nowMillis = nowMillis,
            anchorMillis = anchor,
            wordCount = words.size,
            intervalMillis = overlayPreferences.rotationIntervalMillis,
        )
        return words[index]
    }

    fun advanceWord(nowMillis: Long = System.currentTimeMillis()): WordEntry {
        val words = activeWords()
        val current = currentWord(nowMillis)
        val currentIndex = words.indexOf(current).coerceAtLeast(0)
        val nextIndex = (currentIndex + 1) % words.size
        prefs.edit()
            .putLong(KEY_ANCHOR_MILLIS, nowMillis - (nextIndex.toLong() * overlayPreferences.rotationIntervalMillis))
            .apply()
        return words[nextIndex]
    }

    fun pinWord(word: WordEntry, nowMillis: Long = System.currentTimeMillis()) {
        val index = activeWords().indexOf(word).coerceAtLeast(0)
        prefs.edit()
            .putLong(KEY_ANCHOR_MILLIS, nowMillis - (index.toLong() * overlayPreferences.rotationIntervalMillis))
            .apply()
    }

    fun rotationInfo(nowMillis: Long = System.currentTimeMillis()): RotationInfo {
        val anchor = ensureAnchor(nowMillis)
        val intervalMillis = overlayPreferences.rotationIntervalMillis
        val elapsed = (nowMillis - anchor).coerceAtLeast(0L)
        val currentSlotStart = anchor + ((elapsed / intervalMillis) * intervalMillis)
        return RotationInfo(
            currentWord = currentWord(nowMillis),
            nextRotationMillis = currentSlotStart + intervalMillis,
            intervalMillis = intervalMillis,
        )
    }

    fun activeWords(): List<WordEntry> {
        val generated = generatedVocabularyStore.loadEntries().map { it.toWordEntry() }
        return ActiveVocabularySelector.select(
            builtIn = Vocabulary.words,
            generated = generated,
            sourceMode = aiLabPreferences.sourceMode,
        )
    }

    private fun ensureAnchor(nowMillis: Long): Long {
        val existing = prefs.getLong(KEY_ANCHOR_MILLIS, NO_ANCHOR)
        if (existing != NO_ANCHOR) return existing

        prefs.edit().putLong(KEY_ANCHOR_MILLIS, nowMillis).apply()
        return nowMillis
    }

    private companion object {
        const val PREFS_NAME = "chinese_word_state"
        const val KEY_ANCHOR_MILLIS = "anchor_millis"
        const val NO_ANCHOR = -1L
    }
}

data class RotationInfo(
    val currentWord: WordEntry,
    val nextRotationMillis: Long,
    val intervalMillis: Long,
)
