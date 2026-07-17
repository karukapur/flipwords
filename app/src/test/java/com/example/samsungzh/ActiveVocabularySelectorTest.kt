package com.example.samsungzh

import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveVocabularySelectorTest {
    private val builtIn = listOf(
        WordEntry("你好", "ni hao", "hello"),
        WordEntry("謝謝", "xie xie", "thank you"),
    )
    private val generated = listOf(
        WordEntry("捷運站", "jie yun zhan", "MRT station"),
        WordEntry("少冰", "shao bing", "less ice"),
    )
    private val custom = listOf(
        WordEntry("開會", "kāi huì", "have a meeting", explicitId = "custom:meeting"),
    )

    @Test
    fun builtInOnlyIgnoresGeneratedEntries() {
        assertEquals(
            builtIn,
            ActiveVocabularySelector.select(builtIn, generated, AiVocabularySourceMode.BUILT_IN_ONLY),
        )
    }

    @Test
    fun generatedFirstUsesGeneratedPackBeforeBuiltInFallback() {
        assertEquals(
            generated + builtIn,
            ActiveVocabularySelector.select(builtIn, generated, AiVocabularySourceMode.GENERATED_FIRST),
        )
    }

    @Test
    fun generatedOnlyUsesGeneratedPackWhenAvailable() {
        assertEquals(
            generated,
            ActiveVocabularySelector.select(builtIn, generated, AiVocabularySourceMode.GENERATED_ONLY),
        )
    }

    @Test
    fun mixedModeInterleavesGeneratedAndBuiltInEntries() {
        assertEquals(
            listOf(generated[0], builtIn[0], generated[1], builtIn[1]),
            ActiveVocabularySelector.select(builtIn, generated, AiVocabularySourceMode.MIX_BOTH),
        )
    }

    @Test
    fun missingGeneratedPackFallsBackToBuiltInForEveryMode() {
        AiVocabularySourceMode.values().forEach { mode ->
            assertEquals(builtIn, ActiveVocabularySelector.select(builtIn, emptyList(), mode))
        }
    }

    @Test
    fun customEntriesAreFirstAndActiveInEverySourceMode() {
        AiVocabularySourceMode.values().forEach { mode ->
            val selected = ActiveVocabularySelector.select(builtIn, generated, custom, mode)

            assertEquals(custom.first(), selected.first())
            assertEquals(1, selected.count { it.stableId() == "custom:meeting" })
        }
    }

    @Test
    fun customEntryWinsUnicodeAndWhitespaceNormalizedHanziCollision() {
        val builtInSubway = WordEntry("捷運", "jié yùn", "MRT; metro")
        val customSubway = WordEntry(
            "捷 運",
            "jié yùn",
            "subway",
            explicitId = builtInSubway.stableId(),
        )

        val selected = ActiveVocabularySelector.select(
            builtIn = builtIn + builtInSubway,
            generated = generated,
            custom = listOf(customSubway),
            sourceMode = AiVocabularySourceMode.MIX_BOTH,
        )

        assertEquals(customSubway, selected.first())
        assertEquals(1, selected.count { normalizedHanziKey(it.hanzi) == "捷運" })
    }
}
