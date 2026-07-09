package com.example.samsungzh

data class WordProgress(
    val wordId: String,
    val timesDisplayed: Int = 0,
    val totalVisibleMillis: Long = 0L,
    val totalEligibleExposureMillis: Long = 0L,
    val unlocksWhileActive: Int = 0,
    val fullAppOpensWhileActive: Int = 0,
    val tapsWhileActive: Int = 0,
    val firstSeenAt: Long? = null,
    val lastSeenAt: Long? = null,
    val distinctDaysSeen: Int = 0,
    val spacedReappearances: Int = 0,
    val effectiveExposures: Double = 0.0,
    val estimatedHalfLifeHours: Double = SchedulerConfig.BASE_HALF_LIFE_HOURS,
    val predictedRecall: Double = 0.0,
    val familiarityScore: Double = 0.0,
    val nextEligibleAt: Long? = null,
    val status: WordStatus = WordStatus.NEW,
    val isHidden: Boolean = false,
)

enum class WordStatus {
    NEW,
    LEARNING,
    FAMILIAR,
    STABLE,
    MASTERED,
    RETIRED,
    HIDDEN,
}

data class DisplaySession(
    val id: String,
    val wordId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val displayMode: DisplayMode,
    val rawDurationMillis: Long,
    val eligibleExposureMillis: Long,
    val screenOnMillis: Long,
    val lockedMillis: Long,
    val unlockedMillis: Long,
    val inactiveMillis: Long,
    val movingMillis: Long,
    val unlockCount: Int,
    val fullAppOpenCount: Int,
    val tapCount: Int,
    val learningContext: PhoneLearningState,
)

enum class DisplayMode {
    FULL_CARD,
    HANZI_ONLY,
    PINYIN_PROMPT,
    MEANING_PROMPT,
    REVEAL_CARD,
}

enum class PhoneLearningState {
    ASLEEP_OR_INACTIVE,
    LOCKED_IDLE,
    GLANCE_OPPORTUNITY,
    ACTIVE_PHONE_USE,
    FULL_APP_LEARNING,
    MOVING_OR_WORKOUT,
    UNKNOWN,
}

data class DailyLearningStats(
    val dateEpochDay: Long,
    val hskLevel: Int?,
    val newCount: Int,
    val learningCount: Int,
    val familiarCount: Int,
    val stableCount: Int,
    val masteredCount: Int,
    val retiredCount: Int,
    val hiddenCount: Int,
    val wordsDisplayedToday: Int,
    val uniqueWordsDisplayedToday: Int,
    val totalEligibleExposureMillisToday: Long,
    val unlocksWhileWordActiveToday: Int,
    val fullAppOpensWhileWordActiveToday: Int,
    val movedToFamiliarToday: Int,
    val movedToStableToday: Int,
    val movedToMasteredToday: Int,
)

data class AdaptiveRotationInfo(
    val currentWord: WordEntry,
    val displayMode: DisplayMode,
    val nextOpportunityMillis: Long,
    val minimumSpacingMillis: Long,
    val phoneLearningState: PhoneLearningState,
    val progress: WordProgress,
)

data class SchedulerDecision(
    val word: WordEntry,
    val progress: WordProgress,
    val displayMode: DisplayMode,
    val rotated: Boolean,
    val paused: Boolean,
)

fun WordEntry.stableId(): String = listOf(hanzi, pinyin, english).joinToString(separator = "|")
