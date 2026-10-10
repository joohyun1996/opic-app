package com.jooh.opic.feature.grammar

import com.jooh.opic.core.correction.*

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.sentencesToCorrect
import com.jooh.opic.core.llm.HfTokenStore
import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.llm.OnDeviceLlmEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class WritingPage { PREPARATION, EDITOR }

data class WritingUiState(
    val active: Boolean = false,
    val page: WritingPage = WritingPage.PREPARATION,
    val unit: GrammarUnit? = null,
    val input: String = "",
    val hasToken: Boolean = false,
    val isCorrecting: Boolean = false,
    val total: Int = 0,
    val results: List<CorrectionOutcome> = emptyList(),
) {
    val currentNumber: Int get() = (results.size + 1).coerceAtMost(total)
}

class GrammarWritingViewModel(
    private val engine: OnDeviceLlmEngine,
    private val tokenStore: HfTokenStore,
    private val modelDownloaded: () -> Boolean,
) : ViewModel() {
    private val coordinator = CorrectionCoordinator(engine)
    private val mutableState = MutableStateFlow(WritingUiState(hasToken = tokenStore.getToken() != null))
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var preparation: Job? = null

    fun prepareExisting() {
        if (!modelDownloaded() || engine.state.value !is LlmEngineState.NotDownloaded || preparation?.isActive == true) return
        preparation = viewModelScope.launch(Dispatchers.IO) { engine.ensureModelReady() }
    }

    fun enter(unit: GrammarUnit) {
        val sameUnit = state.value.unit?.id == unit.id
        mutableState.value = state.value.copy(active = true, unit = unit,
            page = WritingPage.PREPARATION,
            input = if (sameUnit) state.value.input else "", results = emptyList(), total = 0, isCorrecting = false)
        prepareExisting()
    }

    fun openEditor() {
        if (state.value.active && engine.state.value is LlmEngineState.Ready)
            mutableState.update { it.copy(page = WritingPage.EDITOR) }
    }

    fun saveToken(token: String) {
        if (token.isBlank()) return
        tokenStore.setToken(token)
        mutableState.update { it.copy(hasToken = true) }
    }

    fun download() {
        if (preparation?.isActive == true || (!modelDownloaded() && !state.value.hasToken)) return
        preparation = viewModelScope.launch(Dispatchers.IO) { engine.ensureModelReady() }
    }

    fun setInput(value: String) { mutableState.update { it.copy(input = value) } }

    fun startCorrection() {
        val old = state.value
        val unit = old.unit ?: return
        val sentences = sentencesToCorrect(old.input)
        if (!old.active || old.page != WritingPage.EDITOR || old.isCorrecting || sentences.size < unit.writingTask.minSentences) return
        mutableState.value = old.copy(isCorrecting = true, total = sentences.size, results = emptyList())
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                coordinator.correct(sentences) { result ->
                    currentCoroutineContext().ensureActive() // 취소 뒤 늦게 끝난 결과는 붙이지 않는다
                    mutableState.update { it.copy(results = it.results + result) }
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                mutableState.update { it.copy(isCorrecting = false) }
            }
        }
    }

    fun retry(index: Int) {
        val old = state.value
        val failed = old.results.getOrNull(index) as? CorrectionOutcome.Failed ?: return
        if (old.isCorrecting) return
        mutableState.update { it.copy(isCorrecting = true) }
        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val replacement = coordinator.correctOne(failed.sentence)
                // 취소 후 "다시 쓰기"로 목록이 바뀌었으면 늦게 끝난 결과를 버린다 (TASK 11 REVIEW S1).
                mutableState.update { state ->
                    if (state.results.getOrNull(index) != failed) state
                    else state.copy(results = state.results.toMutableList().also { it[index] = replacement })
                }
            } catch (error: CancellationException) {
                throw error
            } finally {
                mutableState.update { it.copy(isCorrecting = false) }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        mutableState.update { it.copy(isCorrecting = false) }
    }

    fun again() { mutableState.update { it.copy(results = emptyList(), total = 0, isCorrecting = false) } }

    fun exit() {
        cancel()
        mutableState.update { it.copy(active = false) }
    }

    class Factory(
        private val engine: OnDeviceLlmEngine,
        private val tokenStore: HfTokenStore,
        private val modelDownloaded: () -> Boolean,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = GrammarWritingViewModel(engine, tokenStore, modelDownloaded) as T
    }
}
