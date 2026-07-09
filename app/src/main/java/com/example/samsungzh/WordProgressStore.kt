package com.example.samsungzh

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class WordProgressStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun currentWordId(): String? = prefs.getString(KEY_CURRENT_WORD_ID, null)

    fun currentDisplayMode(): DisplayMode =
        enumValueOrDefault(prefs.getString(KEY_CURRENT_DISPLAY_MODE, null), DisplayMode.FULL_CARD)

    fun setCurrent(wordId: String, displayMode: DisplayMode) {
        prefs.edit()
            .putString(KEY_CURRENT_WORD_ID, wordId)
            .putString(KEY_CURRENT_DISPLAY_MODE, displayMode.name)
            .apply()
    }

    fun loadProgress(): Map<String, WordProgress> {
        val raw = prefs.getString(KEY_PROGRESS_JSON, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            buildMap {
                for (index in 0 until array.length()) {
                    val progress = wordProgressFromJson(array.getJSONObject(index))
                    put(progress.wordId, progress)
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveProgress(progressById: Map<String, WordProgress>) {
        val array = JSONArray()
        progressById.values.sortedBy { it.wordId }.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_PROGRESS_JSON, array.toString()).apply()
    }

    fun updateProgress(progress: WordProgress) {
        val updated = loadProgress().toMutableMap()
        updated[progress.wordId] = progress
        saveProgress(updated)
    }

    private companion object {
        const val PREFS_NAME = "word_learning_progress"
        const val KEY_PROGRESS_JSON = "progress_json"
        const val KEY_CURRENT_WORD_ID = "current_word_id"
        const val KEY_CURRENT_DISPLAY_MODE = "current_display_mode"
    }
}

fun WordProgress.toJson(): JSONObject =
    JSONObject()
        .put("wordId", wordId)
        .put("timesDisplayed", timesDisplayed)
        .put("totalVisibleMillis", totalVisibleMillis)
        .put("totalEligibleExposureMillis", totalEligibleExposureMillis)
        .put("unlocksWhileActive", unlocksWhileActive)
        .put("fullAppOpensWhileActive", fullAppOpensWhileActive)
        .put("tapsWhileActive", tapsWhileActive)
        .put("firstSeenAt", firstSeenAt)
        .put("lastSeenAt", lastSeenAt)
        .put("distinctDaysSeen", distinctDaysSeen)
        .put("spacedReappearances", spacedReappearances)
        .put("effectiveExposures", effectiveExposures)
        .put("estimatedHalfLifeHours", estimatedHalfLifeHours)
        .put("predictedRecall", predictedRecall)
        .put("familiarityScore", familiarityScore)
        .put("nextEligibleAt", nextEligibleAt)
        .put("status", status.name)
        .put("isHidden", isHidden)

fun wordProgressFromJson(json: JSONObject): WordProgress =
    WordProgress(
        wordId = json.getString("wordId"),
        timesDisplayed = json.optInt("timesDisplayed"),
        totalVisibleMillis = json.optLong("totalVisibleMillis"),
        totalEligibleExposureMillis = json.optLong("totalEligibleExposureMillis"),
        unlocksWhileActive = json.optInt("unlocksWhileActive"),
        fullAppOpensWhileActive = json.optInt("fullAppOpensWhileActive"),
        tapsWhileActive = json.optInt("tapsWhileActive"),
        firstSeenAt = nullableLong(json, "firstSeenAt"),
        lastSeenAt = nullableLong(json, "lastSeenAt"),
        distinctDaysSeen = json.optInt("distinctDaysSeen"),
        spacedReappearances = json.optInt("spacedReappearances"),
        effectiveExposures = json.optDouble("effectiveExposures", 0.0),
        estimatedHalfLifeHours = json.optDouble(
            "estimatedHalfLifeHours",
            SchedulerConfig.BASE_HALF_LIFE_HOURS,
        ),
        predictedRecall = json.optDouble("predictedRecall", 0.0),
        familiarityScore = json.optDouble("familiarityScore", 0.0),
        nextEligibleAt = nullableLong(json, "nextEligibleAt"),
        status = enumValueOrDefault(json.optString("status"), WordStatus.NEW),
        isHidden = json.optBoolean("isHidden", false),
    )

fun nullableLong(json: JSONObject, key: String): Long? =
    if (json.has(key) && !json.isNull(key)) json.optLong(key) else null
