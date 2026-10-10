package com.jooh.opic.core.model

enum class WordStatus { NEW, LEARNING, MASTERED }

fun wordStatus(correctCount: Int?): WordStatus = when {
    correctCount == null -> WordStatus.NEW
    correctCount >= 3 -> WordStatus.MASTERED
    else -> WordStatus.LEARNING
}
