package com.example.samsungzh

object DailyStatsAggregator {
    fun aggregate(
        words: List<WordEntry>,
        progressById: Map<String, WordProgress>,
        sessions: List<DisplaySession>,
        nowMillis: Long,
        hskLevel: Int? = null,
    ): DailyLearningStats {
        val today = epochDay(nowMillis)
        val filteredWords = words.filter { hskLevel == null || it.hskLevel == hskLevel }
        val filteredIds = filteredWords.map { it.stableId() }.toSet()
        val filteredProgress = filteredIds.map { id ->
            progressById[id] ?: WordProgress(id)
        }
        val todaySessions = sessions.filter { session ->
            epochDay(session.startedAt) == today && session.wordId in filteredIds
        }
        return DailyLearningStats(
            dateEpochDay = today,
            hskLevel = hskLevel,
            newCount = filteredProgress.count { it.status == WordStatus.NEW },
            learningCount = filteredProgress.count { it.status == WordStatus.LEARNING },
            familiarCount = filteredProgress.count { it.status == WordStatus.FAMILIAR },
            stableCount = filteredProgress.count { it.status == WordStatus.STABLE },
            masteredCount = filteredProgress.count { it.status == WordStatus.MASTERED },
            retiredCount = filteredProgress.count { it.status == WordStatus.RETIRED },
            hiddenCount = filteredProgress.count { it.status == WordStatus.HIDDEN || it.isHidden },
            wordsDisplayedToday = todaySessions.size,
            uniqueWordsDisplayedToday = todaySessions.map { it.wordId }.distinct().size,
            totalEligibleExposureMillisToday = todaySessions.sumOf { it.eligibleExposureMillis },
            unlocksWhileWordActiveToday = todaySessions.sumOf { it.unlockCount },
            fullAppOpensWhileWordActiveToday = todaySessions.sumOf { it.fullAppOpenCount },
            movedToFamiliarToday = filteredProgress.count {
                it.status == WordStatus.FAMILIAR && it.lastSeenAt != null && epochDay(it.lastSeenAt) == today
            },
            movedToStableToday = filteredProgress.count {
                it.status == WordStatus.STABLE && it.lastSeenAt != null && epochDay(it.lastSeenAt) == today
            },
            movedToMasteredToday = filteredProgress.count {
                it.status == WordStatus.MASTERED && it.lastSeenAt != null && epochDay(it.lastSeenAt) == today
            },
        )
    }

    fun weeklySeries(
        words: List<WordEntry>,
        progressById: Map<String, WordProgress>,
        nowMillis: Long,
        hskLevel: Int? = null,
    ): List<DailyLearningStats> {
        val current = aggregate(words, progressById, emptyList(), nowMillis, hskLevel)
        return (6 downTo 0).map { daysAgo ->
            current.copy(dateEpochDay = current.dateEpochDay - daysAgo)
        }
    }
}
