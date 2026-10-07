package com.jooh.opic.feature.grammar

import androidx.lifecycle.ViewModel
import com.jooh.opic.core.common.GrammarAttempt
import com.jooh.opic.core.common.GrammarFeedback
import com.jooh.opic.core.common.GrammarScore
import com.jooh.opic.core.common.gradeChoice
import com.jooh.opic.core.common.gradeText
import com.jooh.opic.core.common.record
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GrammarPage { LIST, EXPLANATION, EXERCISE, RESULT }

data class GrammarUiState(
    val catalog: GrammarLoadResult,
    val page: GrammarPage = GrammarPage.LIST,
    val unit: GrammarUnit? = null,
    val exercises: List<GrammarExercise> = emptyList(),
    val index: Int = 0,
    val input: String = "",
    val selected: Int? = null,
    val attempt: GrammarAttempt = GrammarAttempt(),
    val score: GrammarScore = GrammarScore(),
    val missed: List<GrammarExercise> = emptyList(),
) {
    val current: GrammarExercise? get() = exercises.getOrNull(index)
}

class GrammarViewModel(result: GrammarLoadResult) : ViewModel() {
    private val mutableState = MutableStateFlow(GrammarUiState(result))
    val state = mutableState.asStateFlow()

    fun openUnit(unit: GrammarUnit) {
        mutableState.value = GrammarUiState(mutableState.value.catalog, GrammarPage.EXPLANATION, unit)
    }

    fun start() {
        val unit = state.value.unit ?: return
        val shuffled = unit.exercises.shuffled()
        mutableState.value = state.value.copy(page = GrammarPage.EXERCISE, exercises = shuffled,
            index = 0, input = if (shuffled.first().kind == "fix") shuffled.first().sentence else "",
            selected = null, attempt = GrammarAttempt(), score = GrammarScore(), missed = emptyList())
    }

    fun setInput(value: String) { mutableState.update { it.copy(input = value) } }
    fun select(value: Int) { mutableState.update { it.copy(selected = value) } }

    fun submit() {
        val old = state.value
        val question = old.current ?: return
        if (old.attempt.isFinished()) return
        val correct = if (question.kind == "choice") old.selected?.let { gradeChoice(it, question.answer!!) } ?: false
            else gradeText(old.input, question.answers!!)
        val attempt = old.attempt.submit(correct)
        mutableState.value = old.copy(
            attempt = attempt,
            score = if (attempt.isFinished()) old.score.record(attempt) else old.score,
            missed = if (attempt.feedback == GrammarFeedback.REVEAL_ANSWER) old.missed + question else old.missed,
        )
    }

    fun next() {
        val old = state.value
        if (!old.attempt.isFinished()) return
        if (old.index == old.exercises.lastIndex) {
            mutableState.value = old.copy(page = GrammarPage.RESULT)
        } else {
            val next = old.exercises[old.index + 1]
            mutableState.value = old.copy(index = old.index + 1,
                input = if (next.kind == "fix") next.sentence else "",
                selected = null, attempt = GrammarAttempt())
        }
    }

    fun back(): Boolean {
        val old = state.value
        return when (old.page) {
            GrammarPage.LIST -> false
            GrammarPage.EXPLANATION -> { mutableState.value = old.copy(page = GrammarPage.LIST); true }
            GrammarPage.EXERCISE, GrammarPage.RESULT -> { mutableState.value = old.copy(page = GrammarPage.EXPLANATION); true }
        }
    }

    fun list() { mutableState.value = GrammarUiState(state.value.catalog) }
}
