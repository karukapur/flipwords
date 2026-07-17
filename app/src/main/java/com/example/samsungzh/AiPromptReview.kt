package com.example.samsungzh

internal data class AiPromptReviewSection(
    val title: String,
    val description: String,
    val prompt: String,
)

internal object AiPromptReview {
    private const val LOOKUP_PLACEHOLDER = "<user-provided word or phrase>"

    fun sections(
        hskLevel: AiHskLevel,
        targetCount: Int,
    ): List<AiPromptReviewSection> = listOf(
        AiPromptReviewSection(
            title = "Phrase pack generation",
            description = "Exact prompt for the current ${hskLevel.label} and $targetCount-word settings.",
            prompt = LiteRtVocabularyGenerator.promptFor(hskLevel, targetCount),
        ),
        AiPromptReviewSection(
            title = "Custom phrase lookup",
            description = "Template used when AI resolves one lookup. The placeholder is replaced with the user's text.",
            prompt = LiteRtSingleTermPrompt.initial(LOOKUP_PLACEHOLDER),
        ),
        AiPromptReviewSection(
            title = "Custom phrase retry",
            description = "Template used only when the first custom phrase response fails validation.",
            prompt = LiteRtSingleTermPrompt.corrective(LOOKUP_PLACEHOLDER),
        ),
    )
}
