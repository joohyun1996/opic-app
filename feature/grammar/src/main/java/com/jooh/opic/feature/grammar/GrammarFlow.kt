package com.jooh.opic.feature.grammar

import com.jooh.opic.core.correction.*

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jooh.opic.core.llm.HfTokenStore
import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.llm.OnDeviceLlmEngine

@Composable
fun GrammarFlow(
    catalog: GrammarLoadResult?,
    engine: OnDeviceLlmEngine,
    tokenStore: HfTokenStore,
    modelDownloaded: () -> Boolean,
    modelBytes: Long,
    onBack: () -> Unit,
    reviews: GrammarReviewStore? = null,
) {
    if (catalog == null) {
        Text("문법 데이터를 불러오는 중…")
        return
    }
    val model: GrammarViewModel = viewModel(factory = remember(catalog) { GrammarViewModelFactory(catalog, reviews) })
    val writing: GrammarWritingViewModel = viewModel(factory = remember(engine, tokenStore) {
        GrammarWritingViewModel.Factory(engine, tokenStore, modelDownloaded)
    })
    val state by model.state.collectAsState()
    var track by rememberSaveable { mutableStateOf("opic") }
    val writingState by writing.state.collectAsState()
    val engineState by engine.state.collectAsState()
    DisposableEffect(writing) { onDispose { writing.cancel() } }
    LaunchedEffect(Unit) { writing.prepareExisting(); model.refreshDue() }
    val back = {
        if (writingState.active) writing.exit()
        else if (!model.back()) onBack()
    }
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        if (writingState.active) {
            when (writingState.page) {
                WritingPage.PREPARATION -> LlmPreparationScreen(engineState, writingState.hasToken, modelDownloaded(), modelBytes,
                    writing::saveToken, writing::download, writing::openEditor, back)
                WritingPage.EDITOR -> GrammarWritingScreen(writingState, back, writing::setInput,
                    writing::startCorrection, writing::cancel, writing::retry, writing::again,
                    onList = { writing.exit(); model.list() })
            }
            return@Column
        }
        when (val loaded = state.catalog) {
            GrammarLoadResult.Failed -> Text("문법 데이터를 불러오지 못했습니다")
            is GrammarLoadResult.Loaded -> when (state.page) {
                GrammarPage.LIST -> GrammarUnitListScreen(loaded.book.units,
                    if (engineState is LlmEngineState.Ready) "준비됨" else "준비 필요", onBack, model::openUnit,
                    dueCount = state.dueCount, onReview = model::startReview, track = track, onTrack = { track = it })
                GrammarPage.EXPLANATION -> state.unit?.let { GrammarExplanationScreen(it, back, model::start) }
                GrammarPage.EXERCISE -> GrammarExerciseScreen(state, back, model::setInput, model::select, model::submit, model::next)
                GrammarPage.RESULT -> GrammarResultScreen(state, if (state.reviewMode) model::startReview else model::start, model::list,
                    onWrite = { state.unit?.let(writing::enter) })
            }
        }
    }
}

private class GrammarViewModelFactory(private val result: GrammarLoadResult, private val reviews: GrammarReviewStore?) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = GrammarViewModel(result, reviews) as T
}
