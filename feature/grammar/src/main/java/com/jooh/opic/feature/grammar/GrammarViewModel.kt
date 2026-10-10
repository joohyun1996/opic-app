package com.jooh.opic.feature.grammar

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.REVIEW_SESSION_SIZE
import kotlinx.coroutines.launch
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
    /** 오늘의 복습 세션이면 true (단원 연습과 기록 방식이 다르다) */
    val reviewMode: Boolean = false,
    val dueCount: Int = 0,
) {
    val current: GrammarExercise? get() = exercises.getOrNull(index)
}

class GrammarViewModel(result: GrammarLoadResult, private val reviews: GrammarReviewStore? = null) : ViewModel() {
    private val mutableState = MutableStateFlow(GrammarUiState(result))
    val state = mutableState.asStateFlow()
    private val book get() = (state.value.catalog as? GrammarLoadResult.Loaded)?.book
    private val unitOf by lazy { book?.units?.flatMap { unit -> unit.exercises.map { it.id to unit.id } }?.toMap().orEmpty() }

    fun refreshDue() {
        val store = reviews ?: return
        val loaded = book ?: return
        viewModelScope.launch {
            val count = runCatching { store.dueExercises(loaded).size }.getOrElse { Log.e("GrammarReview", "복습 목록 읽기 실패", it); 0 }
            mutableState.update { it.copy(dueCount = count) }
        }
    }

    /** 오늘 복습할 문제 중 오래된 것부터 최대 10개를 섞어서 낸다. */
    fun startReview() {
        val store = reviews ?: return
        val loaded = book ?: return
        viewModelScope.launch {
            val due = runCatching { store.dueExercises(loaded) }.getOrDefault(emptyList()).take(REVIEW_SESSION_SIZE).shuffled()
            if (due.isEmpty()) return@launch
            mutableState.value = GrammarUiState(state.value.catalog, GrammarPage.EXERCISE, unit = null, exercises = due,
                input = if (due.first().kind == "fix") due.first().sentence else "", reviewMode = true, dueCount = state.value.dueCount)
        }
    }

    private fun record(question: GrammarExercise, feedback: GrammarFeedback?, reviewMode: Boolean) {
        val store = reviews ?: return
        viewModelScope.launch {
            runCatching {
                if (reviewMode) store.recordReview(question.id, feedback == GrammarFeedback.CORRECT_FIRST)
                else if (feedback == GrammarFeedback.CORRECT_SECOND || feedback == GrammarFeedback.REVEAL_ANSWER)
                    unitOf[question.id]?.let { store.recordPractice(question.id, it) }
            }.onFailure { Log.e("GrammarReview", "복습 기록 저장 실패", it) }
        }
    }

    fun openUnit(unit: GrammarUnit) {
        mutableState.value = GrammarUiState(mutableState.value.catalog, GrammarPage.EXPLANATION, unit, dueCount = mutableState.value.dueCount)
    }

    fun start() {
        val unit = state.value.unit ?: return
        val shuffled = unit.exercises.shuffled()
        mutableState.value = state.value.copy(page = GrammarPage.EXERCISE, exercises = shuffled, reviewMode = false,
            index = 0, input = if (shuffled.first().kind == "fix") shuffled.first().sentence else "",
            selected = null, attempt = GrammarAttempt(), score = GrammarScore(), missed = emptyList())
    }

    fun setInput(value: String) { mutableState.update { it.copy(input = value) } }
    fun select(value: Int) { mutableState.update { it.copy(selected = value) } }

    fun submit() {
        val old = state.value
        val question = old.current ?: return
        if (old.attempt.isFinished()) return
        val correct = if (question.kind in TAP_KINDS) old.selected?.let { gradeChoice(it, question.answer!!) } ?: false
            else gradeText(old.input, question.answers!!)
        val attempt = old.attempt.submit(correct)
        if (attempt.isFinished()) record(question, attempt.feedback, old.reviewMode)
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
            GrammarPage.EXERCISE, GrammarPage.RESULT -> {
                if (old.reviewMode) list() else mutableState.value = old.copy(page = GrammarPage.EXPLANATION)
                true
            }
        }
    }

    fun list() {
        mutableState.value = GrammarUiState(state.value.catalog, dueCount = state.value.dueCount)
        refreshDue()
    }
}
