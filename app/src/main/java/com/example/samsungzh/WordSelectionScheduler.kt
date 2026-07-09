package com.example.samsungzh

import kotlin.math.abs

object WordSelectionScheduler {
    fun selectWord(
        words: List<WordEntry>,
        progressById: Map<String, WordProgress>,
        currentWordId: String?,
        nowMillis: Long,
        phoneLearningState: PhoneLearningState,
        forceRotate: Boolean = false,
        hskLevel: Int? = null,
        minimumSpacingMillis: Long = SchedulerConfig.DEFAULT_MINIMUM_SPACING_MILLIS,
    ): SchedulerDecision {
        require(words.isNotEmpty()) { "words must not be empty" }

        val activeWords = words.filter { word ->
            val progress = progressById[word.stableId()]
            hskLevel == null || word.hskLevel == hskLevel
        }.ifEmpty { words }

        val currentWord = activeWords.firstOrNull { it.stableId() == currentWordId } ?: activeWords.first()
        val currentProgress = refreshedProgress(
            progressById[currentWord.stableId()] ?: WordProgress(currentWord.stableId()),
            nowMillis,
            minimumSpacingMillis,
        )

        if (SchedulerFeatureFlags.ENABLE_CONTEXT_AWARE_PAUSE &&
            phoneLearningState == PhoneLearningState.ASLEEP_OR_INACTIVE
        ) {
            return SchedulerDecision(
                word = currentWord,
                progress = currentProgress,
                displayMode = displayModeFor(currentProgress, currentWord, nowMillis, minimumSpacingMillis),
                rotated = false,
                paused = true,
            )
        }

        val minimumSpacingReached = currentProgress.lastSeenAt == null ||
            nowMillis >= currentProgress.lastSeenAt + minimumSpacingMillis
        if (!forceRotate && !minimumSpacingReached) {
            return SchedulerDecision(
                word = currentWord,
                progress = currentProgress,
                displayMode = displayModeFor(currentProgress, currentWord, nowMillis, minimumSpacingMillis),
                rotated = false,
                paused = false,
            )
        }

        val candidates = activeWords
            .map {
                it to refreshedProgress(
                    progressById[it.stableId()] ?: WordProgress(it.stableId()),
                    nowMillis,
                    minimumSpacingMillis,
                )
            }
            .filterNot { (_, progress) -> progress.isHidden || progress.status == WordStatus.HIDDEN }
            .filterNot { (word, _) -> !forceRotate && word.stableId() == currentWord.stableId() }

        val reviewCandidates = candidates.filter { (_, progress) ->
            isNormalReviewCandidate(progress, nowMillis)
        }
        val newCandidates = candidates.filter { (_, progress) ->
            progress.status == WordStatus.NEW || progress.timesDisplayed == 0
        }

        val chooseReview = shouldChooseReview(reviewCandidates.size, newCandidates.size, nowMillis, minimumSpacingMillis)
        val selected = when {
            chooseReview && reviewCandidates.isNotEmpty() -> reviewCandidates.maxBy { (word, progress) ->
                reviewPriority(word, progress, nowMillis, hskLevel)
            }
            newCandidates.isNotEmpty() -> newCandidates.minBy { (word, progress) ->
                newWordRank(word, progress, nowMillis)
            }
            reviewCandidates.isNotEmpty() -> reviewCandidates.maxBy { (word, progress) ->
                reviewPriority(word, progress, nowMillis, hskLevel)
            }
            else -> maintenanceCandidates(candidates, nowMillis).maxByOrNull { (word, progress) ->
                reviewPriority(word, progress, nowMillis, hskLevel)
            } ?: candidates.minByOrNull { (word, progress) ->
                newWordRank(word, progress, nowMillis)
            } ?: (currentWord to currentProgress)
        }

        val rotated = selected.first.stableId() != currentWord.stableId()
        return SchedulerDecision(
            word = selected.first,
            progress = selected.second,
            displayMode = displayModeFor(selected.second, selected.first, nowMillis, minimumSpacingMillis),
            rotated = rotated,
            paused = false,
        )
    }

    fun displayModeFor(
        progress: WordProgress,
        word: WordEntry,
        nowMillis: Long,
        minimumSpacingMillis: Long = SchedulerConfig.DEFAULT_MINIMUM_SPACING_MILLIS,
    ): DisplayMode {
        if (!SchedulerFeatureFlags.ENABLE_PASSIVE_PROMPTS) return DisplayMode.FULL_CARD
        if (progress.status == WordStatus.NEW || progress.timesDisplayed <= 1) return DisplayMode.FULL_CARD
        val bucket = abs((word.stableId().hashCode() * 31) + (nowMillis / minimumSpacingMillis).toInt()) % 10
        return when {
            bucket < 2 -> DisplayMode.MEANING_PROMPT
            bucket < 4 -> DisplayMode.PINYIN_PROMPT
            bucket == 4 -> DisplayMode.REVEAL_CARD
            else -> DisplayMode.FULL_CARD
        }
    }

    fun isNormalReviewCandidate(progress: WordProgress, nowMillis: Long): Boolean {
        if (progress.status == WordStatus.HIDDEN || progress.isHidden) return false
        if (progress.status == WordStatus.RETIRED) return isMaintenanceDue(progress, nowMillis)
        if (progress.status == WordStatus.MASTERED) return isMaintenanceDue(progress, nowMillis)
        return progress.timesDisplayed > 0 &&
            progress.predictedRecall < SchedulerConfig.REVIEW_RECALL_THRESHOLD &&
            (progress.nextEligibleAt == null || nowMillis >= progress.nextEligibleAt)
    }

    fun isMaintenanceDue(progress: WordProgress, nowMillis: Long): Boolean {
        val lastSeen = progress.lastSeenAt ?: return true
        return nowMillis - lastSeen >=
            SchedulerConfig.RARE_MAINTENANCE_REVIEW_DAYS * SchedulerConfig.MILLIS_PER_DAY
    }

    private fun refreshedProgress(
        progress: WordProgress,
        nowMillis: Long,
        minimumSpacingMillis: Long,
    ): WordProgress =
        EffectiveExposureCalculator.update(progress, nowMillis, minimumSpacingMillis)

    private fun shouldChooseReview(
        reviewCount: Int,
        newCount: Int,
        nowMillis: Long,
        minimumSpacingMillis: Long,
    ): Boolean {
        if (reviewCount <= 0) return false
        if (newCount <= 0) return true
        val reviewRatio = when {
            reviewCount >= 10 -> SchedulerConfig.OVERDUE_HEAVY_REVIEW_RATIO
            reviewCount <= 2 -> SchedulerConfig.LOW_OVERDUE_REVIEW_RATIO
            else -> SchedulerConfig.DEFAULT_REVIEW_WORD_RATIO
        }
        val bucket = abs((nowMillis / minimumSpacingMillis).toInt()) % 100
        return bucket < (reviewRatio * 100.0).toInt()
    }

    private fun reviewPriority(
        word: WordEntry,
        progress: WordProgress,
        nowMillis: Long,
        hskLevel: Int?,
    ): Double {
        val overdueScore = SchedulerConfig.REVIEW_RECALL_THRESHOLD - progress.predictedRecall
        val lowFamiliarityBonus = 1.0 - progress.familiarityScore
        val daysSinceLastSeen = ((nowMillis - (progress.lastSeenAt ?: 0L)).coerceAtLeast(0L)).toDouble() /
            SchedulerConfig.MILLIS_PER_DAY.toDouble()
        val hskBonus = if (hskLevel != null && word.hskLevel == hskLevel) 0.25 else 0.0
        return overdueScore + (0.25 * lowFamiliarityBonus) + (0.05 * daysSinceLastSeen) + hskBonus
    }

    private fun newWordRank(word: WordEntry, progress: WordProgress, nowMillis: Long): Long {
        val lastSeen = progress.lastSeenAt ?: Long.MIN_VALUE
        val stableNoise = abs(word.stableId().hashCode() % 97)
        return if (lastSeen == Long.MIN_VALUE) {
            Long.MIN_VALUE + stableNoise
        } else {
            lastSeen + stableNoise + (nowMillis % 13L)
        }
    }

    private fun maintenanceCandidates(
        candidates: List<Pair<WordEntry, WordProgress>>,
        nowMillis: Long,
    ): List<Pair<WordEntry, WordProgress>> =
        candidates.filter { (_, progress) ->
            (progress.status == WordStatus.MASTERED || progress.status == WordStatus.RETIRED) &&
                isMaintenanceDue(progress, nowMillis)
        }
}
