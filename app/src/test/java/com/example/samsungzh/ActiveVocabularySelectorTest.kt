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
}
