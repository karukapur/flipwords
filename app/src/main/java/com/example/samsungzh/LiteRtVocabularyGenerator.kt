package com.example.samsungzh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File

class LiteRtVocabularyGenerator internal constructor(
    private val promptExecutor: LiteRtPromptExecutor,
) {
    constructor() : this(ReflectiveLiteRtPromptExecutor)

    suspend fun generate(
        modelFile: File,
        hskLevel: AiHskLevel = AiHskLevel.HSK_2,
        targetCount: Int = AiLabPreferences.DEFAULT_GENERATION_TARGET_COUNT,
    ): String = withContext(Dispatchers.Default) {
        LiteRtInferenceCoordinator.runSerialized {
            currentCoroutineContext().ensureActive()
            val output = promptExecutor.executeWithGpuFallback(
                modelFile = modelFile,
                prompt = promptWithSystemInstruction(hskLevel, targetCount),
            )
            currentCoroutineContext().ensureActive()
            output
        }
    }

    private fun promptWithSystemInstruction(hskLevel: AiHskLevel, targetCount: Int): String {
        val normalizedTarget = targetCount.coerceIn(
            AiLabPreferences.MIN_GENERATION_TARGET_COUNT,
            AiLabPreferences.MAX_GENERATION_TARGET_COUNT,
        )
        val candidateCount = (normalizedTarget + maxOf(20, normalizedTarget / 2))
            .coerceAtMost(AiLabPreferences.MAX_GENERATION_TARGET_COUNT + 60)
        return """
$SYSTEM_INSTRUCTION

Target level: ${hskLevel.promptDescription}.
The app needs $normalizedTarget valid saved entries. Generate $candidateCount candidates so validation can accept at least $normalizedTarget unique entries.

$PROMPT
""".trimIndent()
    }

    companion object {
        private const val SYSTEM_INSTRUCTION =
            "You generate compact Traditional Taiwanese Mandarin vocabulary for a phone cover screen."

        private const val PROMPT = """
Return only a JSON array. Do not include Markdown or explanations.
Generate beginner to intermediate Traditional Taiwanese Mandarin words or short phrases.
No full sentences. No simplified Chinese.
Each item must have exactly these fields: hanzi, pinyin, english.
Constraints:
- hanzi must be Traditional Chinese, 1 to 8 characters.
- pinyin must use spaces and tone marks, not tone numbers, max 48 characters.
- english must be compact, max 36 characters.
- Avoid duplicates.
Example:
[
  {"hanzi":"捷運站","pinyin":"jié yùn zhàn","english":"MRT station"},
  {"hanzi":"少冰","pinyin":"shǎo bīng","english":"less ice"}
]
"""
    }
}
