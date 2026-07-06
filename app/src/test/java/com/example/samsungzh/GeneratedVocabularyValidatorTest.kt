package com.example.samsungzh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedVocabularyValidatorTest {
    @Test
    fun parsesValidJsonCandidates() {
        val raw = """
            [
              {"hanzi":"捷運站","pinyin":"jie yun zhan","english":"MRT station"},
              {"hanzi":"少冰","pinyin":"shao bing","english":"less ice"}
            ]
        """.trimIndent()

        val entries = GeneratedVocabularyValidator.parseValidEntries(raw, 123L)

        assertEquals(2, entries.size)
        assertEquals("捷運站", entries.first().hanzi)
        assertEquals(123L, entries.first().createdAtMillis)
    }

    @Test
    fun rejectsSimplifiedCharacters() {
        val entry = GeneratedVocabularyEntry("学习", "xue xi", "study", "test", 0L)

        assertFalse(GeneratedVocabularyValidator.isValid(entry))
    }

    @Test
    fun rejectsSentencePunctuation() {
        val entry = GeneratedVocabularyEntry("你好嗎？", "ni hao ma", "how are you", "test", 0L)

        assertFalse(GeneratedVocabularyValidator.isValid(entry))
    }

    @Test
    fun rejectsOverlongFieldsAndDuplicates() {
        val longHanzi = GeneratedVocabularyEntry("我想去便利商店買東西", "wo xiang qu", "go shopping", "test", 0L)
        val duplicate = GeneratedVocabularyEntry("捷運站", "jie yun zhan", "MRT station", "test", 0L)
        val valid = GeneratedVocabularyEntry("捷運站", "jie yun zhan", "MRT station", "test", 0L)

        assertFalse(GeneratedVocabularyValidator.isValid(longHanzi))
        assertFalse(GeneratedVocabularyValidator.isValid(duplicate, setOf("捷運站")))
        assertTrue(GeneratedVocabularyValidator.isValid(valid))
    }
}
