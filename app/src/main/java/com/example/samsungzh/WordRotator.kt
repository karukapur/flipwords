package com.example.samsungzh

object WordRotator {
    const val INTERVAL_MINUTES = 90L
    const val INTERVAL_MILLIS = INTERVAL_MINUTES * 60L * 1000L

    fun indexFor(
        nowMillis: Long,
        anchorMillis: Long,
        wordCount: Int,
        intervalMillis: Long = INTERVAL_MILLIS,
    ): Int {
        require(wordCount > 0) { "wordCount must be positive" }
        require(intervalMillis > 0L) { "intervalMillis must be positive" }
        val elapsed = (nowMillis - anchorMillis).coerceAtLeast(0L)
        val slots = elapsed / intervalMillis
        return (slots % wordCount).toInt()
    }
}
