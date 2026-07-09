package com.example.samsungzh

import kotlin.math.pow

object HalfLifeMemoryModel {
    fun estimatedHalfLifeHours(effectiveExposures: Double): Double =
        (SchedulerConfig.BASE_HALF_LIFE_HOURS *
            SchedulerConfig.HALF_LIFE_GROWTH_FACTOR.pow(effectiveExposures.coerceAtLeast(0.0)))
            .coerceAtMost(SchedulerConfig.MAX_HALF_LIFE_HOURS)

    fun predictedRecall(
        lastSeenAtMillis: Long?,
        nowMillis: Long,
        estimatedHalfLifeHours: Double,
    ): Double {
        if (lastSeenAtMillis == null) return 0.0
        val elapsedHours = ((nowMillis - lastSeenAtMillis).coerceAtLeast(0L)).toDouble() /
            SchedulerConfig.MILLIS_PER_HOUR.toDouble()
        val halfLife = estimatedHalfLifeHours.coerceAtLeast(0.01)
        return 2.0.pow(-elapsedHours / halfLife).coerceIn(0.0, 1.0)
    }

    fun isReviewEligible(progress: WordProgress): Boolean =
        progress.predictedRecall < SchedulerConfig.REVIEW_RECALL_THRESHOLD

    fun isTooSoon(progress: WordProgress): Boolean =
        progress.predictedRecall > SchedulerConfig.TOO_SOON_RECALL_THRESHOLD
}
