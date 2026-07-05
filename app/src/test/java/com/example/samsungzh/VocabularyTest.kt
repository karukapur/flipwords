package com.example.samsungzh

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class VocabularyTest {
    @Test
    fun vocabularyHasOneHundredTwelveEntries() {
        assertEquals(112, Vocabulary.words.size)
    }

    @Test
    fun vocabularyUsesTraditionalCharacters() {
        val simplifiedCharacters = setOf(
            '谢', '学', '习', '欢', '为', '时', '间', '师', '东',
            '气', '饭', '觉', '书', '听', '说', '兴', '题', '帮', '机',
            '电', '国', '现', '车', '发', '关', '问', '钱', '贵', '买',
            '卖', '带', '队', '订', '账', '据', '边', '楼', '会', '决',
            '计', '划', '经', '验', '习', '惯', '环', '运', '动',
            '别', '处',
        )

        val hanzi = Vocabulary.words.joinToString(separator = "") { it.hanzi }
        val simplifiedFound = hanzi.filter { it in simplifiedCharacters }.toSet()

        assertFalse("Simplified characters found: $simplifiedFound", simplifiedFound.isNotEmpty())
    }
}
