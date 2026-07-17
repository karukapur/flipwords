package com.example.samsungzh

data class ResolvedVocabularyCandidate(
    val hanzi: String,
    val pinyin: String,
    val english: String,
)

interface VocabularyResolver {
    suspend fun resolve(rawInput: String): ResolvedVocabularyCandidate
}

class VocabularyResolutionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
