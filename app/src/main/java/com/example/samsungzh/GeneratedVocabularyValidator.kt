package com.example.samsungzh

import org.json.JSONArray
import org.json.JSONObject

object GeneratedVocabularyValidator {
    const val DEFAULT_BATCH_SIZE = 50
    const val MAX_HANZI_CHARS = 8
    const val MAX_PINYIN_CHARS = 48
    const val MAX_ENGLISH_CHARS = 36

    private val simplifiedOnlyCharacters = setOf(
        '谢', '学', '习', '欢', '为', '时', '间', '师', '东',
        '气', '饭', '觉', '书', '听', '说', '兴', '题', '帮', '机',
        '电', '国', '现', '车', '发', '关', '问', '钱', '贵', '买',
        '卖', '带', '队', '订', '账', '据', '边', '楼', '会', '决',
        '计', '划', '经', '验', '惯', '环', '运', '动',
        '别', '处', '这', '个', '里', '吗', '写', '来', '点',
    )
    private val sentencePunctuation = setOf('。', '！', '？', '；', '：', '，', '、')

    fun parseValidEntries(
        rawOutput: String,
        createdAtMillis: Long,
        targetCount: Int = DEFAULT_BATCH_SIZE,
    ): List<GeneratedVocabularyEntry> {
        val json = extractJsonArray(rawOutput) ?: return emptyList()
        val seen = linkedSetOf<String>()
        val entries = mutableListOf<GeneratedVocabularyEntry>()
        val array = JSONArray(json)
        val normalizedTarget = targetCount.coerceIn(
            AiLabPreferences.MIN_GENERATION_TARGET_COUNT,
            AiLabPreferences.MAX_GENERATION_TARGET_COUNT,
        )

        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val entry = entryFromJson(obj, createdAtMillis) ?: continue
            if (isValid(entry, seen)) {
                seen.add(entry.hanzi)
                entries.add(entry)
            }
            if (entries.size == normalizedTarget) break
        }

        return entries
    }

    fun toJson(entries: List<GeneratedVocabularyEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("hanzi", entry.hanzi)
                    .put("pinyin", entry.pinyin)
                    .put("english", entry.english)
                    .put("source", entry.source)
                    .put("createdAtMillis", entry.createdAtMillis),
            )
        }
        return array.toString(2)
    }

    fun fromJson(rawJson: String): List<GeneratedVocabularyEntry> {
        if (rawJson.isBlank()) return emptyList()

        val array = JSONArray(rawJson)
        val entries = mutableListOf<GeneratedVocabularyEntry>()
        for (index in 0 until array.length()) {
            val obj = array.optJSONObject(index) ?: continue
            val entry = GeneratedVocabularyEntry(
                hanzi = obj.optString("hanzi").trim(),
                pinyin = obj.optString("pinyin").trim(),
                english = obj.optString("english").trim(),
                source = obj.optString("source", "ai").trim().ifBlank { "ai" },
                createdAtMillis = obj.optLong("createdAtMillis", 0L),
            )
            if (isValid(entry, entries.map { it.hanzi }.toSet())) {
                entries.add(entry)
            }
        }
        return entries
    }

    fun isValid(entry: GeneratedVocabularyEntry, existingHanzi: Set<String> = emptySet()): Boolean {
        if (entry.hanzi.isBlank() || entry.pinyin.isBlank() || entry.english.isBlank()) return false
        if (entry.hanzi.length > MAX_HANZI_CHARS) return false
        if (entry.pinyin.length > MAX_PINYIN_CHARS) return false
        if (entry.english.length > MAX_ENGLISH_CHARS) return false
        if (entry.hanzi in existingHanzi) return false
        if (entry.hanzi.any { it in simplifiedOnlyCharacters }) return false
        if (entry.hanzi.any { it in sentencePunctuation }) return false
        if (entry.pinyin.none { it in toneMarks }) return false
        return entry.hanzi.any { it.code in CJK_RANGE }
    }

    private fun entryFromJson(obj: JSONObject, createdAtMillis: Long): GeneratedVocabularyEntry? {
        val hanzi = obj.optString("hanzi").trim()
        val pinyin = obj.optString("pinyin").trim()
        val english = obj.optString("english").trim()
        if (hanzi.isBlank() || pinyin.isBlank() || english.isBlank()) return null

        return GeneratedVocabularyEntry(
            hanzi = hanzi,
            pinyin = pinyin,
            english = english,
            source = "litert-lm",
            createdAtMillis = createdAtMillis,
        )
    }

    private fun extractJsonArray(rawOutput: String): String? {
        val start = rawOutput.indexOf('[')
        val end = rawOutput.lastIndexOf(']')
        if (start == -1 || end == -1 || end <= start) return null
        return rawOutput.substring(start, end + 1)
    }

    private val CJK_RANGE = 0x4E00..0x9FFF
    private val toneMarks = setOf(
        'ā', 'á', 'ǎ', 'à', 'ē', 'é', 'ě', 'è', 'ī', 'í', 'ǐ', 'ì',
        'ō', 'ó', 'ǒ', 'ò', 'ū', 'ú', 'ǔ', 'ù', 'ǖ', 'ǘ', 'ǚ', 'ǜ',
    )
}
