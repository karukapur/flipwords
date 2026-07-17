package com.example.samsungzh

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.IOException
import java.util.UUID

class CustomVocabularyStore internal constructor(
    private val file: File,
) {
    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, FILE_NAME),
    )

    fun loadEntries(): List<CustomVocabularyEntry> = synchronized(STORE_LOCK) {
        loadEntriesLocked()
    }

    fun saveEntries(entries: List<CustomVocabularyEntry>) = synchronized(STORE_LOCK) {
        saveEntriesLocked(canonicalEntries(entries))
    }

    fun upsert(entry: CustomVocabularyEntry): CustomVocabularyUpsertResult = synchronized(STORE_LOCK) {
        val incoming = entry.canonicalized()
        require(incoming.isWellFormed()) { "Custom vocabulary entry is incomplete or exceeds field limits" }

        val entries = loadEntriesForMutationLocked().toMutableList()
        val key = normalizedHanziKey(incoming.hanzi)
        val index = entries.indexOfFirst { normalizedHanziKey(it.hanzi) == key }
        val stored = if (index >= 0) {
            val existing = entries[index]
            existing.copy(
                hanzi = incoming.hanzi,
                pinyin = incoming.pinyin,
                english = incoming.english,
                updatedAtMillis = maxOf(existing.updatedAtMillis, incoming.updatedAtMillis),
            ).canonicalized()
        } else {
            incoming
        }

        if (index >= 0) {
            entries[index] = stored
        } else {
            entries.add(stored)
        }
        saveEntriesLocked(canonicalEntries(entries))
        CustomVocabularyUpsertResult(entry = stored, wasExisting = index >= 0)
    }

    private fun loadEntriesLocked(): List<CustomVocabularyEntry> {
        return when (val primary = readEntries(file)) {
            is StoreRead.Valid -> primary.entries
            StoreRead.Missing,
            is StoreRead.Corrupt,
            -> (readEntries(backupFile) as? StoreRead.Valid)?.entries.orEmpty()
        }
    }

    /** Never turn an unreadable primary file into an empty-bank overwrite. */
    private fun loadEntriesForMutationLocked(): List<CustomVocabularyEntry> {
        return when (val primary = readEntries(file)) {
            is StoreRead.Valid -> primary.entries
            StoreRead.Missing -> when (val backup = readEntries(backupFile)) {
                is StoreRead.Valid -> backup.entries
                StoreRead.Missing -> emptyList()
                is StoreRead.Corrupt -> throw corruptStoreError(primary = null, backup = backup.error)
            }
            is StoreRead.Corrupt -> when (val backup = readEntries(backupFile)) {
                is StoreRead.Valid -> backup.entries
                StoreRead.Missing -> throw corruptStoreError(primary.error, backup = null)
                is StoreRead.Corrupt -> throw corruptStoreError(primary.error, backup.error)
            }
        }
    }

    private fun saveEntriesLocked(entries: List<CustomVocabularyEntry>) {
        // Keep the most recent readable generation. If the primary is already corrupt, leave the
        // last-good backup untouched so a recovered upsert can safely repair the primary.
        (readEntries(file) as? StoreRead.Valid)?.let { current ->
            writeEntriesAtomically(backupFile, current.entries)
        }
        writeEntriesAtomically(file, entries)
    }

    private fun writeEntriesAtomically(destination: File, entries: List<CustomVocabularyEntry>) {
        destination.parentFile?.let { parent ->
            check(parent.exists() || parent.mkdirs()) { "Unable to create custom vocabulary directory" }
        }
        val array = JSONArray()
        entries.forEach { entry -> array.put(entry.toJson()) }
        val temporary = File(
            destination.parentFile,
            ".${destination.name}.${UUID.randomUUID()}.tmp",
        )
        try {
            FileOutputStream(temporary).use { output ->
                output.write(array.toString(2).toByteArray(StandardCharsets.UTF_8))
                output.flush()
                output.fd.sync()
            }
            try {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(
                    temporary.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } finally {
            temporary.delete()
        }
    }

    private fun readEntries(source: File): StoreRead {
        if (!source.exists()) return StoreRead.Missing
        return try {
            val array = JSONArray(source.readText())
            val parsed = buildList {
                for (index in 0 until array.length()) {
                    val json = array.optJSONObject(index) ?: continue
                    entryFromJson(json)?.let(::add)
                }
            }
            StoreRead.Valid(canonicalEntries(parsed))
        } catch (error: Exception) {
            StoreRead.Corrupt(error)
        }
    }

    private fun corruptStoreError(primary: Throwable?, backup: Throwable?): IOException =
        IOException(
            "Custom vocabulary could not be updated because its saved bank is corrupt.",
            primary ?: backup,
        ).also { error ->
            if (primary != null && backup != null) error.addSuppressed(backup)
        }

    private fun canonicalEntries(entries: List<CustomVocabularyEntry>): List<CustomVocabularyEntry> {
        val byHanzi = linkedMapOf<String, CustomVocabularyEntry>()
        entries.forEach { rawEntry ->
            val entry = rawEntry.canonicalized()
            if (entry.isWellFormed()) {
                byHanzi.putIfAbsent(normalizedHanziKey(entry.hanzi), entry)
            }
        }
        return byHanzi.values.toList()
    }

    private fun entryFromJson(json: JSONObject): CustomVocabularyEntry? = try {
        CustomVocabularyEntry(
            recordId = json.optString("recordId").trim(),
            schedulerId = json.optString("schedulerId").trim(),
            hanzi = json.optString("hanzi").trim(),
            pinyin = json.optString("pinyin").trim(),
            english = json.optString("english").trim(),
            createdAtMillis = json.optLong("createdAtMillis", 0L),
            updatedAtMillis = json.optLong(
                "updatedAtMillis",
                json.optLong("createdAtMillis", 0L),
            ),
        ).canonicalized().takeIf(CustomVocabularyEntry::isWellFormed)
    } catch (_: Exception) {
        null
    }

    private fun CustomVocabularyEntry.toJson(): JSONObject = JSONObject()
        .put("recordId", recordId)
        .put("schedulerId", schedulerId)
        .put("hanzi", hanzi)
        .put("pinyin", pinyin)
        .put("english", english)
        .put("createdAtMillis", createdAtMillis)
        .put("updatedAtMillis", updatedAtMillis)

    companion object {
        const val FILE_NAME = "custom_vocabulary.json"

        private val STORE_LOCK = Any()
    }

    private val backupFile: File
        get() = File(file.parentFile, "$FILE_NAME.bak")

    private sealed interface StoreRead {
        data object Missing : StoreRead
        data class Valid(val entries: List<CustomVocabularyEntry>) : StoreRead
        data class Corrupt(val error: Throwable) : StoreRead
    }
}

data class CustomVocabularyUpsertResult(
    val entry: CustomVocabularyEntry,
    val wasExisting: Boolean,
)
