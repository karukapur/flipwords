package com.example.samsungzh

object ActiveVocabularySelector {
    fun select(
        builtIn: List<WordEntry>,
        generated: List<WordEntry>,
        custom: List<WordEntry>,
        sourceMode: AiVocabularySourceMode,
    ): List<WordEntry> {
        val selectedSources = if (generated.isEmpty()) {
            builtIn
        } else {
            when (sourceMode) {
                AiVocabularySourceMode.BUILT_IN_ONLY -> builtIn
                AiVocabularySourceMode.GENERATED_FIRST -> generated + builtIn
                AiVocabularySourceMode.GENERATED_ONLY -> generated
                AiVocabularySourceMode.MIX_BOTH -> mixWords(generated, builtIn)
            }
        }
        return distinctByHanzi(custom + selectedSources)
    }

    fun select(
        builtIn: List<WordEntry>,
        generated: List<WordEntry>,
        sourceMode: AiVocabularySourceMode,
    ): List<WordEntry> = select(
        builtIn = builtIn,
        generated = generated,
        custom = emptyList(),
        sourceMode = sourceMode,
    )

    private fun mixWords(generated: List<WordEntry>, builtIn: List<WordEntry>): List<WordEntry> {
        val mixed = ArrayList<WordEntry>(generated.size + builtIn.size)
        val maxSize = maxOf(generated.size, builtIn.size)
        for (index in 0 until maxSize) {
            generated.getOrNull(index)?.let(mixed::add)
            builtIn.getOrNull(index)?.let(mixed::add)
        }
        return mixed
    }

    private fun distinctByHanzi(words: List<WordEntry>): List<WordEntry> {
        val seen = hashSetOf<String>()
        return words.filter { word -> seen.add(normalizedHanziKey(word.hanzi)) }
    }
}
