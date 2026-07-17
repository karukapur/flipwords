package com.example.samsungzh

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomVocabularyStoreTest {
    @Test
    fun roundTripPreservesDurableIdsAndCustomWordUsesSchedulerId() {
        withStore { store ->
            val entry = entry(
                recordId = "record-1",
                schedulerId = "scheduler-1",
                hanzi = "捷運",
            )

            store.saveEntries(listOf(entry))

            val loaded = store.loadEntries().single()
            assertEquals(entry, loaded)
            assertEquals("scheduler-1", loaded.toWordEntry().stableId())
            assertEquals(null, loaded.toWordEntry().hskLevel)
        }
    }

    @Test
    fun normalizedHanziUpsertKeepsIdsAndUpdatesEditableFields() {
        withStore { store ->
            store.saveEntries(
                listOf(entry(recordId = "record-1", schedulerId = "scheduler-1", hanzi = "捷運")),
            )

            val result = store.upsert(
                entry(
                    recordId = "new-record",
                    schedulerId = "new-scheduler",
                    hanzi = " 捷 運 ",
                    pinyin = "jié  yùn",
                    english = "Taipei metro",
                    updatedAtMillis = 200L,
                ),
            )

            assertTrue(result.wasExisting)
            assertEquals("record-1", result.entry.recordId)
            assertEquals("scheduler-1", result.entry.schedulerId)
            assertEquals("捷運", result.entry.hanzi)
            assertEquals("jié yùn", result.entry.pinyin)
            assertEquals("Taipei metro", result.entry.english)
            assertEquals(result.entry, store.loadEntries().single())
        }
    }

    @Test
    fun malformedRowsAreSkippedWithoutDiscardingValidRows() {
        withStore { store ->
            val valid = entry(recordId = "record-1", schedulerId = "scheduler-1")
            val raw = """
                [
                  {"recordId":"", "schedulerId":"bad", "hanzi":"錯", "pinyin":"cuò", "english":"wrong"},
                  "not-an-object",
                  {"recordId":"${valid.recordId}", "schedulerId":"${valid.schedulerId}",
                   "hanzi":"${valid.hanzi}", "pinyin":"${valid.pinyin}", "english":"${valid.english}",
                   "createdAtMillis":${valid.createdAtMillis}, "updatedAtMillis":${valid.updatedAtMillis}}
                ]
            """.trimIndent()
            store.fileForTest().writeText(raw)

            assertEquals(listOf(valid), store.loadEntries())
        }
    }

    @Test
    fun corruptPrimaryRecoversLastGoodBankBeforeAtomicUpsert() {
        withStore { store ->
            val first = entry(recordId = "record-1", schedulerId = "scheduler-1")
            val second = entry(
                recordId = "record-2",
                schedulerId = "scheduler-2",
                hanzi = "捷運",
                pinyin = "jié yùn",
                english = "MRT",
            )
            store.saveEntries(listOf(first))
            store.saveEntries(listOf(first, second))
            store.fileForTest().writeText("truncated [")

            assertEquals(listOf(first), store.loadEntries())

            val third = entry(
                recordId = "record-3",
                schedulerId = "scheduler-3",
                hanzi = "開心",
                pinyin = "kāi xīn",
                english = "happy",
            )
            store.upsert(third)

            assertEquals(listOf(first, third), store.loadEntries())
        }
    }

    @Test
    fun corruptPrimaryWithoutLastGoodBackupRefusesDestructiveUpsert() {
        withStore { store ->
            store.fileForTest().writeText("truncated [")

            try {
                store.upsert(entry(recordId = "new", schedulerId = "new-scheduler"))
                throw AssertionError("Expected corrupt custom bank to reject the upsert")
            } catch (expected: java.io.IOException) {
                assertTrue(expected.message.orEmpty().contains("corrupt"))
            }
            assertEquals("truncated [", store.fileForTest().readText())
        }
    }

    @Test
    fun contentBasedIdsRemainUnchangedUnlessExplicitIdIsProvided() {
        val legacy = WordEntry("捷運", "jié yùn", "MRT; metro")
        val custom = legacy.copy(explicitId = "custom-word:123")

        assertEquals("捷運|jié yùn|MRT; metro", legacy.stableId())
        assertEquals("custom-word:123", custom.stableId())
        assertFalse(legacy.stableId() == custom.stableId())
    }

    private fun withStore(block: (TestStore) -> Unit) {
        val directory = Files.createTempDirectory("custom-vocabulary-test").toFile()
        try {
            block(TestStore(directory.resolve(CustomVocabularyStore.FILE_NAME)))
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun entry(
        recordId: String,
        schedulerId: String,
        hanzi: String = "開會",
        pinyin: String = "kāi huì",
        english: String = "have a meeting",
        updatedAtMillis: Long = 100L,
    ): CustomVocabularyEntry = CustomVocabularyEntry(
        recordId = recordId,
        schedulerId = schedulerId,
        hanzi = hanzi,
        pinyin = pinyin,
        english = english,
        createdAtMillis = 100L,
        updatedAtMillis = updatedAtMillis,
    )

    /** Keeps the backing file available for corrupt-row fixtures without widening production API. */
    private class TestStore(private val file: java.io.File) {
        private val delegate = CustomVocabularyStore(file)

        fun saveEntries(entries: List<CustomVocabularyEntry>) = delegate.saveEntries(entries)

        fun loadEntries(): List<CustomVocabularyEntry> = delegate.loadEntries()

        fun upsert(entry: CustomVocabularyEntry): CustomVocabularyUpsertResult = delegate.upsert(entry)

        fun fileForTest(): java.io.File = file
    }
}
