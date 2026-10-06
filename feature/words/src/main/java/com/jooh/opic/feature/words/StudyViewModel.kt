package com.jooh.opic.feature.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.gradeMeaning
import com.jooh.opic.core.common.gradeWord
import com.jooh.opic.core.common.seqRange
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.WordEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StudyMode(val route: String, val title: String) {
    EN_KO("en-ko", "영→한"), KO_EN("ko-en", "한→영");
    companion object { fun of(route: String?) = entries.firstOrNull { it.route == route } ?: EN_KO }
}

data class StudyState(
    val loading: Boolean = true,
    val cards: List<WordEntity> = emptyList(),
    val index: Int = 0,
    val input: String = "",
    /** null = 아직 확인 전, true/false = 판정 결과 */
    val checked: Boolean? = null,
    val corrected: Boolean = false,
    /** 단어 id → 최종 정답 여부 */
    val results: Map<Long, Boolean> = emptyMap(),
) {
    val current get() = cards.getOrNull(index)
    val finished get() = !loading && index >= cards.size
    val correctCount get() = results.count { it.value }
    val wrongWords get() = cards.filter { results[it.id] == false }
}

class StudyViewModel(
    private val database: OpicDatabase,
    private val day: Int,
    val mode: StudyMode,
    private val onRecorded: () -> Unit,
) : ViewModel() {
    private val mutableState = MutableStateFlow(StudyState())
    val state = mutableState.asStateFlow()
    /** 기록 쓰기 순서 보장: "맞았어요" 정정은 직전 오답 기록이 끝난 뒤에 실행한다. */
    private var lastWrite: Job? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            val range = seqRange(day)
            val cards = database.wordDao().getDayWords("en", range.first, range.last)
            mutableState.value = StudyState(loading = false, cards = cards)
        }
    }

    fun setInput(value: String) = mutableState.update { if (it.checked == null) it.copy(input = value) else it }

    fun check() {
        val now = mutableState.value
        val word = now.current ?: return
        if (now.checked != null || now.input.isBlank()) return
        val correct = when (mode) {
            StudyMode.EN_KO -> gradeMeaning(now.input, word.meaningKo)
            StudyMode.KO_EN -> gradeWord(now.input, word.word)
        }
        mutableState.value = now.copy(checked = correct, results = now.results + (word.id to correct))
        val previous = lastWrite
        lastWrite = viewModelScope.launch {
            previous?.join()
            database.userWordDao().recordResult(word.id, correct, System.currentTimeMillis())
            onRecorded()
        }
    }

    /** 영→한에서 자동 판정이 오답일 때만, 한 번. 오답 기록을 정답 기록으로 바꾼다. */
    fun markCorrect() {
        val now = mutableState.value
        val word = now.current ?: return
        if (mode != StudyMode.EN_KO || now.checked != false || now.corrected) return
        mutableState.value = now.copy(corrected = true, results = now.results + (word.id to true))
        val previous = lastWrite
        lastWrite = viewModelScope.launch {
            previous?.join()
            database.userWordDao().correctLastWrong(word.id, System.currentTimeMillis())
            onRecorded()
        }
    }

    fun next() = mutableState.update {
        if (it.checked == null) it else it.copy(index = it.index + 1, input = "", checked = null, corrected = false)
    }

    fun restart() = mutableState.update { StudyState(loading = false, cards = it.cards) }

    class Factory(
        private val database: OpicDatabase, private val day: Int, private val mode: StudyMode, private val onRecorded: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyViewModel(database, day, mode, onRecorded) as T
    }
}
