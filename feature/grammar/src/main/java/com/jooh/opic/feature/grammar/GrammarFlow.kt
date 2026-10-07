package com.jooh.opic.feature.grammar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GrammarFlow(onBack: () -> Unit) {
    val context = LocalContext.current
    val model: GrammarViewModel = viewModel(factory = remember(context) {
        val result = try {
            GrammarCatalog.parse(context.assets.open("grammar.json").bufferedReader().use { it.readText() })
        } catch (_: Exception) { GrammarLoadResult.Failed }
        GrammarViewModelFactory(result)
    })
    val state by model.state.collectAsState()
    val back = { if (!model.back()) onBack() }
    BackHandler(onBack = back)
    Column(Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        when (val catalog = state.catalog) {
            GrammarLoadResult.Failed -> Text("문법 데이터를 불러오지 못했습니다")
            is GrammarLoadResult.Loaded -> when (state.page) {
                GrammarPage.LIST -> GrammarUnitListScreen(catalog.book.units, onBack, model::openUnit)
                GrammarPage.EXPLANATION -> state.unit?.let { GrammarExplanationScreen(it, back, model::start) }
                GrammarPage.EXERCISE -> GrammarExerciseScreen(state, back, model::setInput, model::select, model::submit, model::next)
                GrammarPage.RESULT -> GrammarResultScreen(state, model::start, model::list)
            }
        }
    }
}

private class GrammarViewModelFactory(private val result: GrammarLoadResult) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = GrammarViewModel(result) as T
}
