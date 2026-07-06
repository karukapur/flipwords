package com.example.samsungzh

object ActiveVocabularySelector {
    fun select(
        builtIn: List<WordEntry>,
        generated: List<WordEntry>,
        sourceMode: AiVocabularySourceMode,
    ): List<WordEntry> {
        if (generated.isEmpty()) return builtIn

        return when (sourceMode) {
            AiVocabularySourceMode.BUILT_IN_ONLY -> builtIn
            AiVocabularySourceMode.GENERATED_FIRST -> generated + builtIn
            AiVocabularySourceMode.GENERATED_ONLY -> generated
            AiVocabularySourceMode.MIX_BOTH -> mixWords(generated, builtIn)
        }
    }

    private fun mixWords(generated: List<WordEntry>, builtIn: List<WordEntry>): List<WordEntry> {
        val mixed = ArrayList<WordEntry>(generated.size + builtIn.size)
        val maxSize = maxOf(generated.size, builtIn.size)
        for (index in 0 until maxSize) {
            generated.getOrNull(index)?.let(mixed::add)
            builtIn.getOrNull(index)?.let(mixed::add)
        }
        return mixed
    }
}
