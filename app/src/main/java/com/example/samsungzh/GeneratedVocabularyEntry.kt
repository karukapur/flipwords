package com.example.samsungzh

data class GeneratedVocabularyEntry(
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val source: String,
    val createdAtMillis: Long,
) {
    fun toWordEntry(): WordEntry = WordEntry(hanzi, pinyin, english, hskLevel = null)
}
