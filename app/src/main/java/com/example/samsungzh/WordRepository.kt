package com.example.samsungzh

import android.content.Context
import java.util.UUID

class WordRepository(context: Context) {
    private val appContext = context.applicationContext
    private val progressStore = WordProgressStore(appContext)
    private val sessionLogger = DisplaySessionLogger(appContext)
    private val phoneStateDetector = PhoneStateDetector(appContext)
    private val aiLabPreferences = AiLabPreferences(appContext)
    private val generatedVocabularyStore = GeneratedVocabularyStore(appContext)
    private val schedulerPreferences = SchedulerPreferences(appContext)

    fun currentWord(nowMillis: Long = System.currentTimeMillis()): WordEntry =
        schedulerTick(nowMillis).word

    fun currentOverlayCard(nowMillis: Long = System.currentTimeMillis()): OverlayCard {
        val info = schedulerTick(nowMillis)
        return OverlayCard(
            word = info.word,
            displayMode = info.displayMode,
        )
    }

    fun advanceWord(nowMillis: Long = System.currentTimeMillis()): WordEntry {
        recordTap(nowMillis)
        val state = phoneStateDetector.currentState(nowMillis)
        return schedulerTick(
            nowMillis = nowMillis,
            phoneLearningState = state,
            forceRotate = true,
        ).word
    }

    fun pinWord(word: WordEntry, nowMillis: Long = System.currentTimeMillis()) {
        val progressById = progressStore.loadProgress().toMutableMap()
        val wordId = word.stableId()
        val progress = markDisplayed(
            progress = progressById[wordId] ?: WordProgress(wordId),
            nowMillis = nowMillis,
            minimumSpacingMillis = schedulerPreferences.minimumSpacingMillis,
        )
        progressById[wordId] = progress
        progressStore.saveProgress(progressById)
        progressStore.setCurrent(
            wordId,
            WordSelectionScheduler.displayModeFor(
                progress,
                word,
                nowMillis,
                schedulerPreferences.minimumSpacingMillis,
            ),
        )
        startNewSession(wordId, progressStore.currentDisplayMode(), nowMillis)
    }

    fun rotationInfo(nowMillis: Long = System.currentTimeMillis()): RotationInfo {
        val info = adaptiveRotationInfo(nowMillis)
        return RotationInfo(
            currentWord = info.currentWord,
            nextRotationMillis = info.nextOpportunityMillis,
            intervalMillis = info.minimumSpacingMillis,
        )
    }

    fun adaptiveRotationInfo(nowMillis: Long = System.currentTimeMillis()): AdaptiveRotationInfo {
        val state = phoneStateDetector.currentState(nowMillis)
        val decision = schedulerTick(nowMillis, state)
        val minimumSpacingMillis = schedulerPreferences.minimumSpacingMillis
        val nextOpportunity = decision.progress.nextEligibleAt
            ?: (nowMillis + minimumSpacingMillis)
        return AdaptiveRotationInfo(
            currentWord = decision.word,
            displayMode = decision.displayMode,
            nextOpportunityMillis = if (decision.paused) {
                nowMillis + SchedulerConfig.MILLIS_PER_MINUTE * 15L
            } else {
                nextOpportunity
            },
            minimumSpacingMillis = minimumSpacingMillis,
            phoneLearningState = state,
            progress = decision.progress,
        )
    }

    fun activeWords(): List<WordEntry> {
        val generated = generatedVocabularyStore.loadEntries().map { it.toWordEntry() }
        return ActiveVocabularySelector.select(
            builtIn = Vocabulary.words,
            generated = generated,
            sourceMode = aiLabPreferences.sourceMode,
        )
    }

    fun progressById(nowMillis: Long = System.currentTimeMillis()): Map<String, WordProgress> =
        progressStore.loadProgress().mapValues { (_, progress) ->
            EffectiveExposureCalculator.update(progress, nowMillis, schedulerPreferences.minimumSpacingMillis)
        }

    fun compactSessions(): List<DisplaySession> = sessionLogger.compactSessions()

    fun hideCurrentWord(nowMillis: Long = System.currentTimeMillis()) {
        val current = currentWord(nowMillis)
        val id = current.stableId()
        val progressById = progressStore.loadProgress().toMutableMap()
        progressById[id] = (progressById[id] ?: WordProgress(id)).copy(
            status = WordStatus.HIDDEN,
            isHidden = true,
        )
        progressStore.saveProgress(progressById)
        schedulerTick(nowMillis = nowMillis, forceRotate = true)
    }

    fun restoreHiddenWords() {
        val updated = progressStore.loadProgress().mapValues { (_, progress) ->
            if (progress.status == WordStatus.HIDDEN || progress.isHidden) {
                progress.copy(status = WordStatus.LEARNING, isHidden = false)
            } else {
                progress
            }
        }
        progressStore.saveProgress(updated)
    }

    fun resetCurrentWordProgress(nowMillis: Long = System.currentTimeMillis()) {
        val current = currentWord(nowMillis)
        val updated = progressStore.loadProgress().toMutableMap()
        updated[current.stableId()] = WordProgress(current.stableId())
        progressStore.saveProgress(updated)
        progressStore.setCurrent(current.stableId(), DisplayMode.FULL_CARD)
        startNewSession(current.stableId(), DisplayMode.FULL_CARD, nowMillis)
    }

    fun recordPhoneEvent(action: String?, nowMillis: Long = System.currentTimeMillis()) {
        phoneStateDetector.recordSystemIntent(action, nowMillis)
        val state = phoneStateDetector.currentState(nowMillis)
        schedulerTick(nowMillis, state)
    }

    fun recordFullAppOpen(nowMillis: Long = System.currentTimeMillis()) {
        phoneStateDetector.recordFullAppOpen(nowMillis)
        val session = sessionLogger.activeSession()
        if (session != null) {
            sessionLogger.saveActiveSession(
                session.accountUntil(nowMillis, PhoneLearningState.FULL_APP_LEARNING)
                    .copy(fullAppOpenCount = session.fullAppOpenCount + 1),
            )
        }
        schedulerTick(nowMillis, PhoneLearningState.FULL_APP_LEARNING)
    }

    private fun recordTap(nowMillis: Long) {
        val state = phoneStateDetector.currentState(nowMillis)
        val session = sessionLogger.activeSession()
        if (session != null) {
            sessionLogger.saveActiveSession(
                session.accountUntil(nowMillis, state).copy(tapCount = session.tapCount + 1),
            )
        }
    }

    private fun schedulerTick(
        nowMillis: Long,
        phoneLearningState: PhoneLearningState = phoneStateDetector.currentState(nowMillis),
        forceRotate: Boolean = false,
    ): SchedulerDecision {
        val words = activeWords()
        val minimumSpacingMillis = schedulerPreferences.minimumSpacingMillis
        var progressById = progressStore.loadProgress().toMutableMap()
        accountActiveSession(nowMillis, phoneLearningState, progressById, endSession = false, minimumSpacingMillis)

        val decision = WordSelectionScheduler.selectWord(
            words = words,
            progressById = progressById,
            currentWordId = progressStore.currentWordId(),
            nowMillis = nowMillis,
            phoneLearningState = phoneLearningState,
            forceRotate = forceRotate,
            minimumSpacingMillis = minimumSpacingMillis,
        )

        if (decision.paused) {
            progressStore.saveProgress(progressById)
            return decision
        }

        val currentId = progressStore.currentWordId()
        if (decision.rotated || currentId == null || sessionLogger.activeSession() == null) {
            progressById = progressStore.loadProgress().toMutableMap()
            accountActiveSession(nowMillis, phoneLearningState, progressById, endSession = true, minimumSpacingMillis)
            val displayed = markDisplayed(
                progress = progressById[decision.word.stableId()] ?: WordProgress(decision.word.stableId()),
                nowMillis = nowMillis,
                minimumSpacingMillis = minimumSpacingMillis,
            )
            progressById[decision.word.stableId()] = displayed
            progressStore.saveProgress(progressById)
            progressStore.setCurrent(decision.word.stableId(), decision.displayMode)
            startNewSession(decision.word.stableId(), decision.displayMode, nowMillis)
            return decision.copy(progress = displayed)
        }

        progressStore.saveProgress(progressById)
        return decision
    }

    private fun accountActiveSession(
        nowMillis: Long,
        state: PhoneLearningState,
        progressById: MutableMap<String, WordProgress>,
        endSession: Boolean,
        minimumSpacingMillis: Long,
    ) {
        val active = sessionLogger.activeSession() ?: return
        val accounted = active.accountUntil(nowMillis, state)
        if (endSession) {
            val finished = accounted.toFinishedSession(nowMillis)
            sessionLogger.appendSession(finished)
            sessionLogger.clearActiveSession()
            val existing = progressById[finished.wordId] ?: WordProgress(finished.wordId)
            progressById[finished.wordId] = EffectiveExposureCalculator.update(
                existing.copy(
                    totalVisibleMillis = existing.totalVisibleMillis + finished.rawDurationMillis,
                    totalEligibleExposureMillis = existing.totalEligibleExposureMillis +
                        finished.eligibleExposureMillis,
                    unlocksWhileActive = existing.unlocksWhileActive + finished.unlockCount,
                    fullAppOpensWhileActive = existing.fullAppOpensWhileActive + finished.fullAppOpenCount,
                    tapsWhileActive = existing.tapsWhileActive + finished.tapCount,
                ),
                nowMillis,
                minimumSpacingMillis,
            )
        } else {
            sessionLogger.saveActiveSession(accounted)
        }
    }

    private fun markDisplayed(
        progress: WordProgress,
        nowMillis: Long,
        minimumSpacingMillis: Long,
    ): WordProgress {
        val previousLastSeen = progress.lastSeenAt
        val distinctDays = if (previousLastSeen == null || epochDay(previousLastSeen) != epochDay(nowMillis)) {
            progress.distinctDaysSeen + 1
        } else {
            progress.distinctDaysSeen
        }
        val spacedReappearances = if (
            previousLastSeen != null &&
            nowMillis - previousLastSeen >= minimumSpacingMillis
        ) {
            progress.spacedReappearances + 1
        } else {
            progress.spacedReappearances
        }
        return EffectiveExposureCalculator.update(
            progress.copy(
                timesDisplayed = progress.timesDisplayed + 1,
                firstSeenAt = progress.firstSeenAt ?: nowMillis,
                lastSeenAt = nowMillis,
                distinctDaysSeen = distinctDays,
                spacedReappearances = spacedReappearances,
                nextEligibleAt = nowMillis + minimumSpacingMillis,
            ),
            nowMillis,
            minimumSpacingMillis,
        )
    }

    private fun startNewSession(wordId: String, displayMode: DisplayMode, nowMillis: Long) {
        sessionLogger.saveActiveSession(
            ActiveDisplaySession(
                id = UUID.randomUUID().toString(),
                wordId = wordId,
                startedAt = nowMillis,
                lastAccountedAt = nowMillis,
                displayMode = displayMode,
            ),
        )
    }
}

data class RotationInfo(
    val currentWord: WordEntry,
    val nextRotationMillis: Long,
    val intervalMillis: Long,
)

data class OverlayCard(
    val word: WordEntry,
    val displayMode: DisplayMode,
)
