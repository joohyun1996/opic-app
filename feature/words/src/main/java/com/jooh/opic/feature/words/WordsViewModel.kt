package com.jooh.opic.feature.words

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.jooh.opic.core.common.WORDS_PER_DAY
import com.jooh.opic.core.common.totalDays
import com.jooh.opic.core.database.DayStats
import com.jooh.opic.core.database.OpicDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WordsState(val days: List<DayStats> = emptyList(), val failed: Boolean = false) {
    val total get() = days.sumOf { it.total }
    val mastered get() = days.sumOf { it.mastered }
    val wrong get() = days.sumOf { it.wrong }
}

class WordsViewModel(private val database: OpicDatabase) : ViewModel() {
    private val mutableState = MutableStateFlow(WordsState())
    val state = mutableState.asStateFlow()
    fun refresh() {
        viewModelScope.launch {
            try {
                mutableState.value = database.withTransaction {
                    val dao = database.wordDao()
                    val count = totalDays(dao.maxSeq("en") ?: 0)
                    val stats = dao.dayStats("en", WORDS_PER_DAY).associateBy { it.day }
                    WordsState((1..count).map { stats[it] ?: DayStats(it, 0, 0, 0) })
                }
            } catch (_: Exception) {
                mutableState.value = mutableState.value.copy(failed = true)
            }
        }
    }
    class Factory(private val database: OpicDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WordsViewModel(database) as T
    }
}
