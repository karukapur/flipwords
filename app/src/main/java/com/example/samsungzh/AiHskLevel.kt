package com.example.samsungzh

enum class AiHskLevel(
    val storedValue: String,
    val label: String,
    val promptDescription: String,
) {
    HSK_1("hsk_1", "HSK 1", "HSK 1 absolute beginner"),
    HSK_2("hsk_2", "HSK 2", "HSK 2 beginner"),
    HSK_3("hsk_3", "HSK 3", "HSK 3 lower intermediate"),
    HSK_4("hsk_4", "HSK 4", "HSK 4 intermediate"),
    HSK_5("hsk_5", "HSK 5", "HSK 5 upper intermediate"),
    HSK_6("hsk_6", "HSK 6", "HSK 6 advanced"),
    ;

    companion object {
        fun fromStoredValue(value: String?): AiHskLevel =
            entries.firstOrNull { it.storedValue == value } ?: HSK_2
    }
}
