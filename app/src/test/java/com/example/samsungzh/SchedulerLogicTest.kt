package com.example.samsungzh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerLogicTest {
    private val now = 1_000_000_000L
    private val words = listOf(
        WordEntry("記得", "ji de", "to remember"),
        WordEntry("您好", "nin hao", "hello"),
        WordEntry("學習", "xue xi", "to study"),
    )

    @Test
    fun halfLifeGrowsAndIsCapped() {
        val low = HalfLifeMemoryModel.estimatedHalfLifeHours(1.0)
        val high = HalfLifeMemoryModel.estimatedHalfLifeHours(10.0)
        val capped = HalfLifeMemoryModel.estimatedHalfLifeHours(1_000.0)

        assertTrue(high > low)
        assertEquals(SchedulerConfig.MAX_HALF_LIFE_HOURS, capped, 0.001)
    }

    @Test
    fun predictedRecallDecaysAndBenefitsFromLongerHalfLife() {
        val lastSeen = now - SchedulerConfig.MILLIS_PER_HOUR
        val recent = HalfLifeMemoryModel.predictedRecall(lastSeen, now, 10.0)
        val later = HalfLifeMemoryModel.predictedRecall(lastSeen, now + SchedulerConfig.MILLIS_PER_HOUR, 10.0)
        val stronger = HalfLifeMemoryModel.predictedRecall(lastSeen, now, 100.0)

        assertTrue(recent > later)
        assertTrue(stronger > recent)
    }

    @Test
    fun exposureScoringIgnoresInactiveAndRewardsEngagement() {
        val inactive = ExposureOpportunityScorer.eligibleMillis(
            rawVisibleMillis = SchedulerConfig.MILLIS_PER_HOUR,
            state = PhoneLearningState.ASLEEP_OR_INACTIVE,
        )
        val base = EffectiveExposureCalculator.effectiveExposures(
            validDisplaySessions = 1,
            eligibleExposureMillis = 10L * SchedulerConfig.MILLIS_PER_MINUTE,
            unlocksWhileActive = 0,
            fullAppOpensWhileActive = 0,
            distinctDaysSeen = 1,
            tapsWhileActive = 0,
        )
        val engaged = EffectiveExposureCalculator.effectiveExposures(
            validDisplaySessions = 1,
            eligibleExposureMillis = 10L * SchedulerConfig.MILLIS_PER_MINUTE,
            unlocksWhileActive = 1,
            fullAppOpensWhileActive = 1,
            distinctDaysSeen = 1,
            tapsWhileActive = 0,
        )
        val capped = EffectiveExposureCalculator.screenExposureUnits(
            eligibleExposureMillis = 9L * SchedulerConfig.MILLIS_PER_HOUR,
            sessionCount = 1,
        )

        assertEquals(0L, inactive)
        assertTrue(engaged > base)
        assertEquals(SchedulerConfig.MAX_SCREEN_EXPOSURE_UNITS_PER_SESSION, capped, 0.001)
    }

    @Test
    fun sleepPauseDoesNotBurnThroughWords() {
        val progress = mapOf(
            words[0].stableId() to WordProgress(
                wordId = words[0].stableId(),
                timesDisplayed = 1,
                lastSeenAt = now - 9L * SchedulerConfig.MILLIS_PER_HOUR,
                nextEligibleAt = now - 7L * SchedulerConfig.MILLIS_PER_HOUR,
            ),
        )

        val decision = WordSelectionScheduler.selectWord(
            words = words,
            progressById = progress,
            currentWordId = words[0].stableId(),
            nowMillis = now,
            phoneLearningState = PhoneLearningState.ASLEEP_OR_INACTIVE,
        )

        assertTrue(decision.paused)
        assertFalse(decision.rotated)
        assertEquals(words[0], decision.word)
    }

    @Test
    fun reviewEligibilityUsesPredictedRecallThresholds() {
        val due = EffectiveExposureCalculator.update(
            WordProgress(
                wordId = words[0].stableId(),
                timesDisplayed = 2,
                lastSeenAt = now - 24L * SchedulerConfig.MILLIS_PER_HOUR,
                totalEligibleExposureMillis = 5L * SchedulerConfig.MILLIS_PER_MINUTE,
                distinctDaysSeen = 1,
            ),
            now,
        )
        val tooSoon = EffectiveExposureCalculator.update(
            due.copy(lastSeenAt = now - SchedulerConfig.MILLIS_PER_MINUTE),
            now,
        )

        assertTrue(due.predictedRecall < SchedulerConfig.REVIEW_RECALL_THRESHOLD)
        assertTrue(HalfLifeMemoryModel.isTooSoon(tooSoon))
    }

    @Test
    fun hiddenWordsAreNeverScheduled() {
        val hidden = WordProgress(
            wordId = words[1].stableId(),
            status = WordStatus.HIDDEN,
            isHidden = true,
        )

        val decision = WordSelectionScheduler.selectWord(
            words = words,
            progressById = mapOf(words[1].stableId() to hidden),
            currentWordId = words[0].stableId(),
            nowMillis = now + SchedulerConfig.DEFAULT_MINIMUM_SPACING_MILLIS,
            phoneLearningState = PhoneLearningState.ACTIVE_PHONE_USE,
            forceRotate = true,
        )

        assertFalse(decision.word.stableId() == words[1].stableId())
    }

    @Test
    fun customMinimumSpacingControlsRotationEligibility() {
        val customSpacing = 60L * SchedulerConfig.MILLIS_PER_MINUTE
        val currentProgress = WordProgress(
            wordId = words[0].stableId(),
            timesDisplayed = 1,
            lastSeenAt = now,
        )
        val refreshed = EffectiveExposureCalculator.update(
            currentProgress,
            now,
            customSpacing,
        )

        assertEquals(now + customSpacing, refreshed.nextEligibleAt)

        val tooSoon = WordSelectionScheduler.selectWord(
            words = words,
            progressById = mapOf(words[0].stableId() to currentProgress),
            currentWordId = words[0].stableId(),
            nowMillis = now + 30L * SchedulerConfig.MILLIS_PER_MINUTE,
            phoneLearningState = PhoneLearningState.ACTIVE_PHONE_USE,
            minimumSpacingMillis = customSpacing,
        )
        val due = WordSelectionScheduler.selectWord(
            words = words,
            progressById = mapOf(words[0].stableId() to currentProgress),
            currentWordId = words[0].stableId(),
            nowMillis = now + customSpacing,
            phoneLearningState = PhoneLearningState.ACTIVE_PHONE_USE,
            minimumSpacingMillis = customSpacing,
        )

        assertFalse(tooSoon.rotated)
        assertEquals(words[0], tooSoon.word)
        assertTrue(due.rotated)
    }

    @Test
    fun masteredWordsNeedMaintenanceWindow() {
        val mastered = WordProgress(
            wordId = words[1].stableId(),
            status = WordStatus.MASTERED,
            timesDisplayed = 12,
            lastSeenAt = now - 5L * SchedulerConfig.MILLIS_PER_DAY,
            distinctDaysSeen = 5,
            effectiveExposures = 12.0,
        )

        assertFalse(WordSelectionScheduler.isNormalReviewCandidate(mastered, now))
        assertTrue(
            WordSelectionScheduler.isMaintenanceDue(
                mastered.copy(lastSeenAt = now - 31L * SchedulerConfig.MILLIS_PER_DAY),
                now,
            ),
        )
    }

    @Test
    fun promptModesPreferFullCardForNewWords() {
        val newProgress = WordProgress(words[0].stableId())
        val familiarProgress = WordProgress(
            wordId = words[0].stableId(),
            timesDisplayed = 4,
            status = WordStatus.FAMILIAR,
        )

        assertEquals(DisplayMode.FULL_CARD, WordSelectionScheduler.displayModeFor(newProgress, words[0], now))
        val modes = (0..20).map {
            WordSelectionScheduler.displayModeFor(
                familiarProgress,
                words[0],
                now + it * SchedulerConfig.DEFAULT_MINIMUM_SPACING_MILLIS,
            )
        }
        assertTrue(modes.any { it == DisplayMode.MEANING_PROMPT || it == DisplayMode.PINYIN_PROMPT })
        assertTrue(modes.any { it == DisplayMode.REVEAL_CARD })
    }

    @Test
    fun dailyStatsAggregateCountsAndHskFilters() {
        val hskWords = listOf(
            WordEntry("一", "yi", "one", hskLevel = 1),
            WordEntry("二", "er", "two", hskLevel = 2),
        )
        val progress = mapOf(
            hskWords[0].stableId() to WordProgress(hskWords[0].stableId(), status = WordStatus.FAMILIAR),
            hskWords[1].stableId() to WordProgress(hskWords[1].stableId(), status = WordStatus.MASTERED),
        )
        val stats = DailyStatsAggregator.aggregate(
            words = hskWords,
            progressById = progress,
            sessions = emptyList(),
            nowMillis = now,
            hskLevel = 1,
        )

        assertEquals(1, stats.familiarCount)
        assertEquals(0, stats.masteredCount)
    }
}
