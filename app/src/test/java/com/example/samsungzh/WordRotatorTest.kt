package com.example.samsungzh

import org.junit.Assert.assertEquals
import org.junit.Test

class WordRotatorTest {
    @Test
    fun sameWordWithinNinetyMinuteWindow() {
        val anchor = 1_000L
        val almostNinetyMinutes = anchor + WordRotator.INTERVAL_MILLIS - 1L

        assertEquals(0, WordRotator.indexFor(anchor, anchor, 10))
        assertEquals(0, WordRotator.indexFor(almostNinetyMinutes, anchor, 10))
    }

    @Test
    fun advancesAfterNinetyMinutes() {
        val anchor = 1_000L
        val afterNinetyMinutes = anchor + WordRotator.INTERVAL_MILLIS

        assertEquals(1, WordRotator.indexFor(afterNinetyMinutes, anchor, 10))
    }

    @Test
    fun supportsCustomShortIntervals() {
        val anchor = 1_000L
        val fiveSeconds = 5_000L
        val afterTwoSlots = anchor + (2L * fiveSeconds)

        assertEquals(2, WordRotator.indexFor(afterTwoSlots, anchor, 10, fiveSeconds))
    }

    @Test
    fun wrapsAtEndOfVocabulary() {
        val anchor = 1_000L
        val afterThreeSlots = anchor + (3L * WordRotator.INTERVAL_MILLIS)

        assertEquals(0, WordRotator.indexFor(afterThreeSlots, anchor, 3))
    }

    @Test
    fun timesBeforeAnchorUseFirstWord() {
        val anchor = 1_000L

        assertEquals(0, WordRotator.indexFor(anchor - 1L, anchor, 10))
    }
}
