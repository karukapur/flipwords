package com.example.samsungzh

import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinToneFormatterTest {
    @Test
    fun addsToneMarksForBuiltInWords() {
        assertEquals(
            "jié yùn zhàn",
            PinyinToneFormatter.format(WordEntry("捷運站", "jie yun zhan", "MRT station")),
        )
    }

    @Test
    fun keepsAlreadyMarkedGeneratedPinyin() {
        assertEquals(
            "shǎo bīng",
            PinyinToneFormatter.format(WordEntry("少冰", "shǎo bīng", "less ice")),
        )
    }
}
