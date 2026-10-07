package com.jooh.opic

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jooh.opic.core.llm.ModelCatalog
import com.jooh.opic.feature.grammar.GrammarFlow
import com.jooh.opic.feature.grammar.GrammarLoadResult
import com.jooh.opic.feature.words.Speaker
import com.jooh.opic.feature.words.WordsApp
import com.jooh.opic.feature.words.WordsPage
import com.jooh.opic.feature.words.WordsTheme

@Composable
fun OpicRoot(app: OpicApplication) {
    val importResult by app.importResult.collectAsState()
    val grammarResult by app.grammarResult.collectAsState()
    val grammarCount = (grammarResult as? GrammarLoadResult.Loaded)?.book?.units?.size
    val nav = rememberNavController()
    val speaker = remember(app) { Speaker(app) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    val ttsAvailable by speaker.available.collectAsState()
    var ttsNoticeShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ttsAvailable) {
        if (ttsAvailable == false && !ttsNoticeShown) {
            ttsNoticeShown = true
            Toast.makeText(app, "기기 설정에서 영어 음성을 설치하세요", Toast.LENGTH_LONG).show()
        }
    }
    val debug = debugContent(app)
    val navigate: (String) -> Unit = { nav.navigate(it) }
    val back: () -> Unit = { nav.safeBack() }
    WordsTheme {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") { WordsApp(app.database, importResult, speaker, WordsPage.HOME, grammarCount, debug != null,
                navigate = navigate, onBack = back) }
            composable("days") { WordsApp(app.database, importResult, speaker, WordsPage.DAYS, grammarCount, debug != null,
                navigate = navigate, onBack = back) }
            composable("day/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                WordsApp(app.database, importResult, speaker, WordsPage.DAY, grammarCount, debug != null,
                    day = entry.arguments?.getInt("day") ?: 1, navigate = navigate, onBack = back)
            }
            composable("study/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                WordsApp(app.database, importResult, speaker, WordsPage.STUDY, grammarCount, debug != null,
                    day = entry.arguments?.getInt("day") ?: 1, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
            }
            composable("wrong") { WordsApp(app.database, importResult, speaker, WordsPage.WRONG, grammarCount, debug != null,
                navigate = navigate, onBack = back) }
            composable("study/wrong/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                WordsApp(app.database, importResult, speaker, WordsPage.WRONG_STUDY, grammarCount, debug != null,
                    day = entry.arguments?.getInt("day") ?: 0, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
            }
            composable("grammar") {
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 16.dp)) {
                        GrammarFlow(grammarResult, app.llmEngine, app.hfTokenStore, { ModelCatalog.isDownloaded(app) },
                            ModelCatalog.config(app).models.first().expectedBytes, back)
                    }
                }
            }
            if (debug != null) composable("debug") { debug { back() } }
        }
    }
}

private fun NavController.safeBack() {
    val current = currentBackStackEntry ?: return
    if (previousBackStackEntry != null && current.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}
