package com.example.samsungzh

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId

class DisplaySessionLogger(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun activeSession(): ActiveDisplaySession? {
        val json = prefs.getString(KEY_ACTIVE_SESSION, null) ?: return null
        return runCatching { ActiveDisplaySession.fromJson(JSONObject(json)) }.getOrNull()
    }

    fun saveActiveSession(session: ActiveDisplaySession) {
        prefs.edit().putString(KEY_ACTIVE_SESSION, session.toJson().toString()).apply()
    }

    fun clearActiveSession() {
        prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
    }

    fun appendSession(session: DisplaySession) {
        val sessions = compactSessions() + session
        val cutoff = System.currentTimeMillis() - SESSION_RETENTION_MILLIS
        val array = JSONArray()
        sessions.filter { it.startedAt >= cutoff }.takeLast(MAX_SESSIONS).forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SESSIONS, array.toString()).apply()
    }

    fun compactSessions(): List<DisplaySession> {
        val raw = prefs.getString(KEY_SESSIONS, "[]") ?: "[]"
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                runCatching { displaySessionFromJson(array.getJSONObject(index)) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val PREFS_NAME = "display_session_log"
        private const val KEY_ACTIVE_SESSION = "active_session"
        private const val KEY_SESSIONS = "sessions"
        private const val MAX_SESSIONS = 240
        private const val SESSION_RETENTION_MILLIS = 45L * SchedulerConfig.MILLIS_PER_DAY
    }
}

data class ActiveDisplaySession(
    val id: String,
    val wordId: String,
    val startedAt: Long,
    val lastAccountedAt: Long,
    val displayMode: DisplayMode,
    val eligibleExposureMillis: Long = 0L,
    val screenOnMillis: Long = 0L,
    val lockedMillis: Long = 0L,
    val unlockedMillis: Long = 0L,
    val inactiveMillis: Long = 0L,
    val movingMillis: Long = 0L,
    val unlockCount: Int = 0,
    val fullAppOpenCount: Int = 0,
    val tapCount: Int = 0,
    val learningContext: PhoneLearningState = PhoneLearningState.UNKNOWN,
) {
    fun accountUntil(nowMillis: Long, state: PhoneLearningState): ActiveDisplaySession {
        val delta = (nowMillis - lastAccountedAt).coerceAtLeast(0L)
        val eligible = ExposureOpportunityScorer.eligibleMillis(delta, state)
        return copy(
            lastAccountedAt = nowMillis,
            eligibleExposureMillis = eligibleExposureMillis + eligible,
            screenOnMillis = screenOnMillis + if (state != PhoneLearningState.ASLEEP_OR_INACTIVE) delta else 0L,
            lockedMillis = lockedMillis + if (state == PhoneLearningState.LOCKED_IDLE) delta else 0L,
            unlockedMillis = unlockedMillis + if (
                state == PhoneLearningState.ACTIVE_PHONE_USE ||
                state == PhoneLearningState.FULL_APP_LEARNING ||
                state == PhoneLearningState.GLANCE_OPPORTUNITY
            ) delta else 0L,
            inactiveMillis = inactiveMillis + if (state == PhoneLearningState.ASLEEP_OR_INACTIVE) delta else 0L,
            movingMillis = movingMillis + if (state == PhoneLearningState.MOVING_OR_WORKOUT) delta else 0L,
            learningContext = state,
        )
    }

    fun toFinishedSession(endedAt: Long): DisplaySession =
        DisplaySession(
            id = id,
            wordId = wordId,
            startedAt = startedAt,
            endedAt = endedAt,
            displayMode = displayMode,
            rawDurationMillis = (endedAt - startedAt).coerceAtLeast(0L),
            eligibleExposureMillis = eligibleExposureMillis,
            screenOnMillis = screenOnMillis,
            lockedMillis = lockedMillis,
            unlockedMillis = unlockedMillis,
            inactiveMillis = inactiveMillis,
            movingMillis = movingMillis,
            unlockCount = unlockCount,
            fullAppOpenCount = fullAppOpenCount,
            tapCount = tapCount,
            learningContext = learningContext,
        )

    fun toJson(): JSONObject =
        JSONObject()
            .put("id", id)
            .put("wordId", wordId)
            .put("startedAt", startedAt)
            .put("lastAccountedAt", lastAccountedAt)
            .put("displayMode", displayMode.name)
            .put("eligibleExposureMillis", eligibleExposureMillis)
            .put("screenOnMillis", screenOnMillis)
            .put("lockedMillis", lockedMillis)
            .put("unlockedMillis", unlockedMillis)
            .put("inactiveMillis", inactiveMillis)
            .put("movingMillis", movingMillis)
            .put("unlockCount", unlockCount)
            .put("fullAppOpenCount", fullAppOpenCount)
            .put("tapCount", tapCount)
            .put("learningContext", learningContext.name)

    companion object {
        fun fromJson(json: JSONObject): ActiveDisplaySession =
            ActiveDisplaySession(
                id = json.getString("id"),
                wordId = json.getString("wordId"),
                startedAt = json.getLong("startedAt"),
                lastAccountedAt = json.optLong("lastAccountedAt", json.getLong("startedAt")),
                displayMode = enumValueOrDefault(json.optString("displayMode"), DisplayMode.FULL_CARD),
                eligibleExposureMillis = json.optLong("eligibleExposureMillis"),
                screenOnMillis = json.optLong("screenOnMillis"),
                lockedMillis = json.optLong("lockedMillis"),
                unlockedMillis = json.optLong("unlockedMillis"),
                inactiveMillis = json.optLong("inactiveMillis"),
                movingMillis = json.optLong("movingMillis"),
                unlockCount = json.optInt("unlockCount"),
                fullAppOpenCount = json.optInt("fullAppOpenCount"),
                tapCount = json.optInt("tapCount"),
                learningContext = enumValueOrDefault(json.optString("learningContext"), PhoneLearningState.UNKNOWN),
            )
    }
}

fun DisplaySession.toJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("wordId", wordId)
        .put("startedAt", startedAt)
        .put("endedAt", endedAt)
        .put("displayMode", displayMode.name)
        .put("rawDurationMillis", rawDurationMillis)
        .put("eligibleExposureMillis", eligibleExposureMillis)
        .put("screenOnMillis", screenOnMillis)
        .put("lockedMillis", lockedMillis)
        .put("unlockedMillis", unlockedMillis)
        .put("inactiveMillis", inactiveMillis)
        .put("movingMillis", movingMillis)
        .put("unlockCount", unlockCount)
        .put("fullAppOpenCount", fullAppOpenCount)
        .put("tapCount", tapCount)
        .put("learningContext", learningContext.name)

fun displaySessionFromJson(json: JSONObject): DisplaySession =
    DisplaySession(
        id = json.getString("id"),
        wordId = json.getString("wordId"),
        startedAt = json.getLong("startedAt"),
        endedAt = if (json.isNull("endedAt")) null else json.optLong("endedAt"),
        displayMode = enumValueOrDefault(json.optString("displayMode"), DisplayMode.FULL_CARD),
        rawDurationMillis = json.optLong("rawDurationMillis"),
        eligibleExposureMillis = json.optLong("eligibleExposureMillis"),
        screenOnMillis = json.optLong("screenOnMillis"),
        lockedMillis = json.optLong("lockedMillis"),
        unlockedMillis = json.optLong("unlockedMillis"),
        inactiveMillis = json.optLong("inactiveMillis"),
        movingMillis = json.optLong("movingMillis"),
        unlockCount = json.optInt("unlockCount"),
        fullAppOpenCount = json.optInt("fullAppOpenCount"),
        tapCount = json.optInt("tapCount"),
        learningContext = enumValueOrDefault(json.optString("learningContext"), PhoneLearningState.UNKNOWN),
    )

fun epochDay(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long =
    Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().toEpochDay()

inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T =
    runCatching { enumValueOf<T>(name.orEmpty()) }.getOrDefault(default)
