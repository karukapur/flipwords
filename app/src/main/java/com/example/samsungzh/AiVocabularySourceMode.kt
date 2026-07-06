package com.example.samsungzh

enum class AiVocabularySourceMode(val storedValue: String, val label: String) {
    BUILT_IN_ONLY("built_in_only", "Built-in only"),
    GENERATED_FIRST("generated_first", "Generated first"),
    GENERATED_ONLY("generated_only", "Generated only"),
    MIX_BOTH("mix_both", "Mix both");

    companion object {
        fun fromStoredValue(value: String?): AiVocabularySourceMode =
            values().firstOrNull { it.storedValue == value } ?: GENERATED_FIRST
    }
}
