package com.example.samsungzh

object ExposureOpportunityScorer {
    fun multiplier(state: PhoneLearningState): Double =
        when (state) {
            PhoneLearningState.ASLEEP_OR_INACTIVE -> 0.0
            PhoneLearningState.LOCKED_IDLE -> 0.1
            PhoneLearningState.GLANCE_OPPORTUNITY -> 0.5
            PhoneLearningState.ACTIVE_PHONE_USE -> 1.0
            PhoneLearningState.FULL_APP_LEARNING -> 1.5
            PhoneLearningState.MOVING_OR_WORKOUT -> 0.3
            PhoneLearningState.UNKNOWN -> 0.3
        }

    fun eligibleMillis(rawVisibleMillis: Long, state: PhoneLearningState): Long =
        (rawVisibleMillis.coerceAtLeast(0L) * multiplier(state)).toLong()

    fun isValidExposureOpportunity(state: PhoneLearningState): Boolean =
        multiplier(state) > 0.0
}
