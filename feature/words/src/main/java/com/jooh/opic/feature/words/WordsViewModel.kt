package com.jooh.opic.feature.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.WORDS_PER_DAY
import com.jooh.opic.core.common.StudyLanguage
import com.jooh.opic.core.common.totalDays
import com.jooh.opic.core.database.DayStats
import com.jooh.opic.core.database.OpicDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class WordsState(val days: List<DayStats> = emptyList(), val failed: Boolean = false) {
    val total get() = days.sumOf { it.total }
    val mastered get() = days.sumOf { it.mastered }
    val wrong get() = days.sumOf { it.wrong }
}

/**
 * Day 통계를 언어별로 한 번만 계산해 홈·메뉴·단어 탭이 함께 쓴다 (TASK 39).
 * Room이 words·user_words 변경을 추적해 기록이 바뀔 때만 다시 계산한다 — 화면에 들어올 때마다 다시 세지 않는다.
 */
private object WordStatsCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val flows = mutableMapOf<Pair<OpicDatabase, String>, StateFlow<WordsState?>>()

    @Synchronized
    fun of(database: OpicDatabase, language: StudyLanguage): StateFlow<WordsState?> = flows.getOrPut(database to language.code) {
        val dao = database.wordDao()
        dao.observeDayStats(language.code, WORDS_PER_DAY).map { list ->
            val stats = list.associateBy { it.day }
            WordsState((1..totalDays(dao.maxSeq(language.code) ?: 0)).map { stats[it] ?: DayStats(it, 0, 0, 0) })
        }.catch { emit(WordsState(failed = true)) }.stateIn(scope, SharingStarted.Eagerly, null)
    }
}

class WordsViewModel(database: OpicDatabase, language: StudyLanguage) : ViewModel() {
    private val shared = WordStatsCache.of(database, language)
    val state: StateFlow<WordsState> = shared.map { it ?: WordsState() }.stateIn(viewModelScope, SharingStarted.Eagerly, shared.value ?: WordsState())
    class Factory(private val database: OpicDatabase, private val language: StudyLanguage) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WordsViewModel(database, language) as T
    }
}
