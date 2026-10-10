package com.jooh.opic.feature.words

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.gradeMeaning
import com.jooh.opic.core.common.gradeWord
import com.jooh.opic.core.common.seqRange
import com.jooh.opic.core.common.isWordReviewDue
import com.jooh.opic.core.common.WORD_REVIEW_SESSION_SIZE
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.jooh.opic.core.common.StudyLanguage
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

/** 학습 대상: Day N 전체, 또는 오답 단어(day = 0이면 모든 Day). */
/** review = 오늘 복습할 단어 (간격 복습, day 무시). */
data class StudySource(val day: Int, val wrongOnly: Boolean, val review: Boolean = false) {
    val key get() = when { review -> "review"; wrongOnly -> "wrong-$day"; else -> "day-$day" }
    val backLabel get() = when { review -> "← 돌아가기"; wrongOnly -> "← 오답 모음"; else -> "← Day $day" }
}

/** 오늘 복습할 단어 (오래된 순, 최대 [WORD_REVIEW_SESSION_SIZE]개). 날짜는 기기 시간대 기준. */
suspend fun dueReviewWords(database: OpicDatabase, language: StudyLanguage, today: Long = LocalDate.now().toEpochDay(),
    zone: ZoneId = ZoneId.systemDefault()): List<WordEntity> =
    database.wordDao().reviewCandidates(language.code)
        .filter { isWordReviewDue(it.correctCount, Instant.ofEpochMilli(it.lastStudiedAt).atZone(zone).toLocalDate().toEpochDay(), today) }
        .take(WORD_REVIEW_SESSION_SIZE).map { it.word }

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
    val saveFailed: Boolean = false,
    val hintShown: Boolean = false,
) {
    val current get() = cards.getOrNull(index)
    val finished get() = !loading && index >= cards.size
    val correctCount get() = results.count { it.value }
    val wrongWords get() = cards.filter { results[it.id] == false }
}

class StudyViewModel(
    private val database: OpicDatabase,
    private val language: StudyLanguage,
    private val source: StudySource,
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
            // 카드 목록은 시작할 때 한 번만 읽는다. 세션 중 습득해도 카드가 빠지지 않는다.
            val dao = database.wordDao()
            val range = if (source.day == 0) 1..Int.MAX_VALUE else seqRange(source.day)
            val cards = if (source.review) dueReviewWords(database, language)
                else if (source.wrongOnly) dao.getWrongWords(language.code, range.first, range.last).map { it.word }
                else dao.getDayWords(language.code, range.first, range.last)
            // 순서로 외우지 않도록 매번 섞는다.
            mutableState.value = StudyState(loading = false, cards = cards.shuffled())
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
            save { database.userWordDao().recordResult(word.id, correct, System.currentTimeMillis()) }
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
            save { database.userWordDao().correctLastWrong(word.id, System.currentTimeMillis()) }
        }
    }

    private suspend fun save(write: suspend () -> Unit) {
        try {
            write()
            onRecorded()
        } catch (error: kotlin.coroutines.cancellation.CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("WordStudy", "학습 기록 저장 실패", error)
            mutableState.update { it.copy(saveFailed = true) }
        }
    }

    fun showHint() = mutableState.update { it.copy(hintShown = true) }

    fun next() = mutableState.update {
        if (it.checked == null) it else it.copy(index = it.index + 1, input = "", checked = null, corrected = false, hintShown = false)
    }

    fun restart() = mutableState.update { StudyState(loading = false, cards = it.cards.shuffled(), saveFailed = it.saveFailed) }

    class Factory(
        private val database: OpicDatabase, private val language: StudyLanguage, private val source: StudySource, private val mode: StudyMode, private val onRecorded: () -> Unit,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = StudyViewModel(database, language, source, mode, onRecorded) as T
    }
}
