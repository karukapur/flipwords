package com.example.samsungzh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedVocabularyStoreTest {
    @Test
    fun metadataForEmptyEntriesClearsGeneratedPackState() {
        val metadata = GeneratedVocabularyStore.metadataFor(emptyList())

        assertEquals(0, metadata.count)
        assertEquals(0L, metadata.lastGenerationMillis)
        assertFalse(metadata.hasEntries)
    }

    @Test
    fun metadataForEntriesUsesCountAndLatestCreatedTime() {
        val metadata = GeneratedVocabularyStore.metadataFor(
            listOf(
                GeneratedVocabularyEntry("捷運站", "jié yùn zhàn", "MRT station", "test", 100L),
                GeneratedVocabularyEntry("少冰", "shǎo bīng", "less ice", "test", 300L),
                GeneratedVocabularyEntry("排隊", "pái duì", "line up", "test", 200L),
            ),
        )

        assertEquals(3, metadata.count)
        assertEquals(300L, metadata.lastGenerationMillis)
        assertTrue(metadata.hasEntries)
    }
}
