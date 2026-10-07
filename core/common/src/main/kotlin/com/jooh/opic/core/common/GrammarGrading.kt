package com.jooh.opic.core.common

fun normalizeAnswer(value: String): String = value.trim()
    .replace('’', '\'')
    .replace(Regex("\\s+"), " ")
    .trimEnd('.', '!', '?', ' ')
    .lowercase()

fun gradeText(input: String, answers: List<String>): Boolean {
    val normalized = normalizeAnswer(input)
    return normalized.isNotEmpty() && answers.any { normalizeAnswer(it) == normalized }
}

fun gradeChoice(selected: Int, answer: Int): Boolean = selected == answer

data class GrammarScore(val firstTry: Int = 0, val secondTry: Int = 0, val wrong: Int = 0)

enum class GrammarFeedback { HINT, CORRECT_FIRST, CORRECT_SECOND, REVEAL_ANSWER }

data class GrammarAttempt(val wrongAttempts: Int = 0, val feedback: GrammarFeedback? = null) {
    fun submit(correct: Boolean): GrammarAttempt {
        if (feedback != null && feedback != GrammarFeedback.HINT) return this
        return when {
            correct -> copy(feedback = if (wrongAttempts == 0) GrammarFeedback.CORRECT_FIRST else GrammarFeedback.CORRECT_SECOND)
            wrongAttempts == 0 -> GrammarAttempt(1, GrammarFeedback.HINT)
            else -> GrammarAttempt(2, GrammarFeedback.REVEAL_ANSWER)
        }
    }

    fun isFinished(): Boolean = feedback != null && feedback != GrammarFeedback.HINT
}

fun GrammarScore.record(attempt: GrammarAttempt): GrammarScore = when (attempt.feedback) {
    GrammarFeedback.CORRECT_FIRST -> copy(firstTry = firstTry + 1)
    GrammarFeedback.CORRECT_SECOND -> copy(secondTry = secondTry + 1)
    GrammarFeedback.REVEAL_ANSWER -> copy(wrong = wrong + 1)
    else -> this
}
