package com.example.samsungzh

import kotlin.math.min

object EffectiveExposureCalculator {
    fun screenExposureUnits(
        eligibleExposureMillis: Long,
        sessionCount: Int,
    ): Double {
        val unitMillis = SchedulerConfig.SCREEN_EXPOSURE_UNIT_MINUTES *
            SchedulerConfig.MILLIS_PER_MINUTE.toDouble()
        val rawUnits = eligibleExposureMillis.coerceAtLeast(0L) / unitMillis
        val cap = sessionCount.coerceAtLeast(1) *
            SchedulerConfig.MAX_SCREEN_EXPOSURE_UNITS_PER_SESSION
        return min(rawUnits, cap)
    }

    fun effectiveExposures(
        validDisplaySessions: Int,
        eligibleExposureMillis: Long,
        unlocksWhileActive: Int,
        fullAppOpensWhileActive: Int,
        distinctDaysSeen: Int,
        tapsWhileActive: Int,
    ): Double {
        val screenOnExposureUnits = screenExposureUnits(
            eligibleExposureMillis = eligibleExposureMillis,
            sessionCount = validDisplaySessions,
        )
        return (1.0 * validDisplaySessions.coerceAtLeast(0)) +
            (0.5 * screenOnExposureUnits) +
            (0.8 * unlocksWhileActive.coerceAtLeast(0)) +
            (1.5 * fullAppOpensWhileActive.coerceAtLeast(0)) +
            (2.0 * distinctDaysSeen.coerceAtLeast(0)) +
            (2.5 * tapsWhileActive.coerceAtLeast(0))
    }

    fun update(
        progress: WordProgress,
        nowMillis: Long,
        minimumSpacingMillis: Long = SchedulerConfig.DEFAULT_MINIMUM_SPACING_MILLIS,
    ): WordProgress {
        val effective = effectiveExposures(
            validDisplaySessions = progress.timesDisplayed,
            eligibleExposureMillis = progress.totalEligibleExposureMillis,
            unlocksWhileActive = progress.unlocksWhileActive,
            fullAppOpensWhileActive = progress.fullAppOpensWhileActive,
            distinctDaysSeen = progress.distinctDaysSeen,
            tapsWhileActive = progress.tapsWhileActive,
        )
        val halfLife = HalfLifeMemoryModel.estimatedHalfLifeHours(effective)
        val predicted = HalfLifeMemoryModel.predictedRecall(
            lastSeenAtMillis = progress.lastSeenAt,
            nowMillis = nowMillis,
            estimatedHalfLifeHours = halfLife,
        )
        val familiarity = ((effective / SchedulerConfig.MASTERED_MIN_EFFECTIVE_EXPOSURES) * predicted)
            .coerceIn(0.0, 1.0)
        val status = if (progress.isHidden || progress.status == WordStatus.HIDDEN) {
            WordStatus.HIDDEN
        } else {
            statusFor(
                effectiveExposures = effective,
                distinctDaysSeen = progress.distinctDaysSeen,
                halfLifeHours = halfLife,
                currentStatus = progress.status,
            )
        }
        return progress.copy(
            effectiveExposures = effective,
            estimatedHalfLifeHours = halfLife,
            predictedRecall = predicted,
            familiarityScore = familiarity,
            nextEligibleAt = progress.lastSeenAt?.plus(minimumSpacingMillis),
            status = status,
            isHidden = status == WordStatus.HIDDEN,
        )
    }

    fun statusFor(
        effectiveExposures: Double,
        distinctDaysSeen: Int,
        halfLifeHours: Double,
        currentStatus: WordStatus = WordStatus.NEW,
    ): WordStatus {
        if (currentStatus == WordStatus.HIDDEN) return WordStatus.HIDDEN
        if (currentStatus == WordStatus.RETIRED) return WordStatus.RETIRED
        if (effectiveExposures >= SchedulerConfig.MASTERED_MIN_EFFECTIVE_EXPOSURES &&
            distinctDaysSeen >= SchedulerConfig.MASTERED_MIN_DISTINCT_DAYS &&
            halfLifeHours >= SchedulerConfig.MASTERED_MIN_HALF_LIFE_HOURS
        ) {
            return WordStatus.MASTERED
        }
        if (effectiveExposures >= SchedulerConfig.STABLE_MIN_EFFECTIVE_EXPOSURES &&
            distinctDaysSeen >= SchedulerConfig.STABLE_MIN_DISTINCT_DAYS &&
            halfLifeHours >= SchedulerConfig.STABLE_MIN_HALF_LIFE_HOURS
        ) {
            return WordStatus.STABLE
        }
        if (effectiveExposures >= 5.0 && distinctDaysSeen >= 2) return WordStatus.FAMILIAR
        if (effectiveExposures > 0.0) return WordStatus.LEARNING
        return WordStatus.NEW
    }
}
