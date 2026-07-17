package com.example.samsungzh

import java.text.Normalizer
import java.util.Locale

object KnownVocabularyResolver {
    fun resolve(rawInput: String, words: Sequence<WordEntry>): ResolvedVocabularyCandidate? {
        val normalizedInput = normalizeLookup(rawInput)
        if (normalizedInput.isBlank()) return null

        return words
            .mapNotNull { word -> match(word, normalizedInput)?.let { score -> score to word } }
            .maxWithOrNull(compareBy<Pair<Int, WordEntry>> { it.first }.thenBy { it.second.hanzi.length })
            ?.second
            ?.let { word ->
                ResolvedVocabularyCandidate(
                    hanzi = word.hanzi,
                    pinyin = PinyinToneFormatter.format(word),
                    english = word.english,
                )
            }
    }

    private fun match(word: WordEntry, input: String): Int? {
        val hanzi = normalizeLookup(word.hanzi)
        if (hanzi.isNotBlank() && containsTerm(input, hanzi)) return 300 + hanzi.length

        val pinyin = normalizeLookup(PinyinToneFormatter.format(word))
        if (pinyin.isNotBlank() && containsTerm(input, pinyin)) return 200 + pinyin.length

        val englishScore = englishLookupTerms(word.english)
            .filter { containsTerm(input, it) }
            .maxOfOrNull(String::length)
        return englishScore?.let { 100 + it }
    }

    private fun englishLookupTerms(english: String): Sequence<String> =
        english
            .splitToSequence(';', ',', '/', '(', ')')
            .map(::normalizeLookup)
            .filter { it.length >= 3 }

    private fun containsTerm(input: String, term: String): Boolean =
        input == term || input.contains(" $term ") || input.startsWith("$term ") || input.endsWith(" $term")

    private fun normalizeLookup(value: String): String {
        val ascii = Normalizer.normalize(value, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
            .joinToString(separator = "")
            .replace('ü', 'u')
            .replace('Ü', 'u')
        return ascii
            .lowercase(Locale.US)
            .replace(Regex("[^\\u3400-\\u9FFFa-z0-9]+"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
}
