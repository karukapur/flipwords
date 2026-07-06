package com.example.samsungzh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

class LiteRtVocabularyGenerator {
    suspend fun generate(modelFile: File): String = withContext(Dispatchers.Default) {
        runCatching { generateWithBackend(modelFile, backendName = "GPU") }
            .getOrElse { generateWithBackend(modelFile, backendName = "CPU") }
    }

    private fun generateWithBackend(modelFile: File, backendName: String): String {
        val packageName = "com.google.ai.edge.litertlm"
        val backendClass = Class.forName("$packageName.Backend")
        val backend = createBackend(packageName, backendName)
        val engineConfig = createEngineConfig(packageName, backendClass, modelFile, backend)
        val engine = Class.forName("$packageName.Engine")
            .getConstructor(Class.forName("$packageName.EngineConfig"))
            .newInstance(engineConfig)

        return try {
            engine.javaClass.getMethod("initialize").invoke(engine)
            val conversationConfig = Class.forName("$packageName.ConversationConfig")
                .getConstructor()
                .newInstance()
            val conversation = engine.javaClass
                .getMethod("createConversation", conversationConfig.javaClass)
                .invoke(engine, conversationConfig)

            try {
                val message = conversation.javaClass
                    .getMethod("sendMessage", String::class.java, Map::class.java)
                    .invoke(conversation, promptWithSystemInstruction(), Collections.emptyMap<String, Any>())
                message.toString()
            } finally {
                conversation.closeIfPossible()
            }
        } finally {
            engine.closeIfPossible()
        }
    }

    private fun createBackend(packageName: String, backendName: String): Any =
        if (backendName == "GPU") {
            Class.forName("$packageName.Backend\$GPU")
                .getConstructor()
                .newInstance()
        } else {
            Class.forName("$packageName.Backend\$CPU")
                .getConstructor(Int::class.javaObjectType, Int::class.javaObjectType)
                .newInstance(null, null)
        }

    private fun createEngineConfig(
        packageName: String,
        backendClass: Class<*>,
        modelFile: File,
        backend: Any,
    ): Any =
        Class.forName("$packageName.EngineConfig")
            .getConstructor(
                String::class.java,
                backendClass,
                backendClass,
                backendClass,
                Int::class.javaObjectType,
                Int::class.javaObjectType,
                String::class.java,
            )
            .newInstance(
                modelFile.absolutePath,
                backend,
                null,
                null,
                null,
                null,
                modelFile.parentFile?.absolutePath,
            )

    private fun Any.closeIfPossible() {
        if (this is AutoCloseable) {
            close()
        }
    }

    private fun promptWithSystemInstruction(): String =
        "$SYSTEM_INSTRUCTION\n\n$PROMPT"

    companion object {
        private const val SYSTEM_INSTRUCTION =
            "You generate compact Traditional Taiwanese Mandarin vocabulary for a phone cover screen."

        private const val PROMPT = """
Return only a JSON array. Do not include Markdown or explanations.
Generate 80 candidate beginner to intermediate Traditional Taiwanese Mandarin words or short phrases.
No full sentences. No simplified Chinese.
Each item must have exactly these fields: hanzi, pinyin, english.
Constraints:
- hanzi must be Traditional Chinese, 1 to 8 characters.
- pinyin must use spaces, no tone marks, max 48 characters.
- english must be compact, max 36 characters.
- Avoid duplicates.
Example:
[
  {"hanzi":"捷運站","pinyin":"jie yun zhan","english":"MRT station"},
  {"hanzi":"少冰","pinyin":"shao bing","english":"less ice"}
]
"""
    }
}
