package com.example.samsungzh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptReviewTest {
    @Test
    fun reviewIncludesExactActivePackPromptAndReadOnlyLookupTemplates() {
        val sections = AiPromptReview.sections(AiHskLevel.HSK_4, 75)

        assertEquals(3, sections.size)
        assertEquals(
            LiteRtVocabularyGenerator.promptFor(AiHskLevel.HSK_4, 75),
            sections[0].prompt,
        )
        assertTrue(sections[0].prompt.contains("HSK 4 intermediate"))
        assertTrue(sections[0].prompt.contains("at least 75 unique entries"))
        assertTrue(sections[1].prompt.contains("<user-provided word or phrase>"))
        assertTrue(sections[2].prompt.contains("previous response failed"))
    }
}
