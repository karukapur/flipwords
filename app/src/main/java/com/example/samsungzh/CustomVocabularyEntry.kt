package com.example.samsungzh

import java.text.Normalizer

data class CustomVocabularyEntry(
    val recordId: String,
    val schedulerId: String,
    val hanzi: String,
    val pinyin: String,
    val english: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long = createdAtMillis,
) {
    fun toWordEntry(): WordEntry = WordEntry(
        hanzi = hanzi,
        pinyin = pinyin,
        english = english,
        hskLevel = null,
        explicitId = schedulerId,
    )

    internal fun canonicalized(): CustomVocabularyEntry = copy(
        recordId = recordId.trim(),
        schedulerId = schedulerId.trim(),
        hanzi = normalizeHanziValue(hanzi),
        pinyin = normalizeDisplayText(pinyin),
        english = normalizeDisplayText(english),
    )

    internal fun isWellFormed(): Boolean =
        recordId.isNotBlank() &&
            schedulerId.isNotBlank() &&
            hanzi.isNotBlank() &&
            pinyin.isNotBlank() &&
            english.isNotBlank() &&
            hanzi.length <= GeneratedVocabularyValidator.MAX_HANZI_CHARS &&
            pinyin.length <= GeneratedVocabularyValidator.MAX_PINYIN_CHARS &&
            english.length <= GeneratedVocabularyValidator.MAX_ENGLISH_CHARS
}

/** A Unicode- and whitespace-insensitive key used only for Hanzi identity comparisons. */
fun normalizedHanziKey(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFKC)
    .filterNot(Char::isWhitespace)

private fun normalizeHanziValue(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFC)
    .filterNot(Char::isWhitespace)

private fun normalizeDisplayText(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFC)
    .trim()
    .replace(Regex("\\s+"), " ")
