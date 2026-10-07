package com.jooh.opic.feature.words

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jooh.opic.core.database.ImportResult
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.feature.grammar.GrammarCatalog
import com.jooh.opic.feature.grammar.GrammarFlow
import com.jooh.opic.feature.grammar.GrammarLoadResult


@Composable
fun WordsApp(database: OpicDatabase, importResult: ImportResult?, debugContent: (@Composable (() -> Unit) -> Unit)? = null) {
    val model: WordsViewModel = viewModel(factory = remember(database) { WordsViewModel.Factory(database) })
    val state by model.state.collectAsState()
    LaunchedEffect(importResult) { model.refresh() }
    val nav = rememberNavController()
    val context = LocalContext.current
    val grammarCount = remember(context) {
        try {
            val raw = context.assets.open("grammar.json").bufferedReader().use { it.readText() }
            (GrammarCatalog.parse(raw) as? GrammarLoadResult.Loaded)?.book?.units?.size
        } catch (_: Exception) { null }
    }
    val speaker = remember { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    val ttsAvailable by speaker.available.collectAsState()
    var ttsNoticeShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ttsAvailable) {
        if (ttsAvailable == false && !ttsNoticeShown) {
            ttsNoticeShown = true
            Toast.makeText(context, "기기 설정에서 영어 음성을 설치하세요", Toast.LENGTH_LONG).show()
        }
    }
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = Color.White, background = Paper,
        surface = Paper, onSurface = Ink, onBackground = Ink, outline = Color(0xFFE8E6E0))) {
        Scaffold { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 16.dp)) {
                    if (importResult == null) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("단어 데이터를 불러오는 중…", Modifier.padding(vertical = 8.dp))
                    }
                    if (importResult is ImportResult.Failed || state.failed) {
                        Text("단어 데이터를 불러오지 못했습니다", color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp))
                    }
                    NavHost(navController = nav, startDestination = "home", modifier = Modifier.weight(1f)) {
                        composable("home") {
                            LaunchedEffect(Unit) { model.refresh() }
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("나의 언어 학습", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 20.dp))
                                Text("오늘도 한 걸음씩", style = MaterialTheme.typography.bodyLarge)
                                Card(onClick = { nav.navigate("days") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("영어 (OPIc)", style = MaterialTheme.typography.titleLarge)
                                        Text("습득 ${state.mastered} / ${state.total}개")
                                        LinearProgressIndicator(progress = { if (state.total == 0) 0f else state.mastered.toFloat() / state.total }, modifier = Modifier.fillMaxWidth(), color = Ink, trackColor = New, drawStopIndicator = {})
                                        Text("Day ${state.days.size}개 · 학습 시작 →")
                                    }
                                }
                                if (state.wrong > 0) TextButton(onClick = { nav.navigate("wrong") }) { Text("오답 ${state.wrong}개 다시 보기 →", color = WrongInk) }
                                Card(onClick = { nav.navigate("grammar") }, modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("영어 문법", style = MaterialTheme.typography.titleLarge)
                                        Text(if (grammarCount == null) "문법 데이터를 확인하세요 →" else "단원 ${grammarCount}개 · 학습 시작 →")
                                    }
                                }
                                Card(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(disabledContainerColor = New)) {
                                    Text("중국어 (HSK) — 준비 중", Modifier.padding(20.dp))
                                }
                                if (debugContent != null) TextButton(onClick = { nav.navigate("debug") }) { Text("LLM 검증") }
                            }
                        }
                        composable("days") {
                            LaunchedEffect(Unit) { model.refresh() }
                            Column {
                                TextButton(onClick = { nav.safeBack() }) { Text("← 홈") }
                                Text("영어 · Day 목록", style = MaterialTheme.typography.headlineSmall)
                                Text("${state.days.size}일 · ${state.total}개 단어", Modifier.padding(vertical = 8.dp))
                                if (state.wrong > 0) Surface(onClick = { nav.navigate("wrong") }, color = WrongBg, shape = MaterialTheme.shapes.small) {
                                    Text("오답 ${state.wrong}개 →", Modifier.padding(8.dp), color = WrongInk)
                                }
                                LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.days, key = { it.day }) { day ->
                                        val color = when { day.total > 0 && day.mastered == day.total -> Learned; day.mastered > 0 -> Learning; else -> New }
                                        Card(onClick = { nav.navigate("day/${day.day}") }, colors = CardDefaults.cardColors(containerColor = color)) {
                                            Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Day ${day.day}", style = MaterialTheme.typography.labelLarge)
                                                Text("${day.mastered}/${day.total}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        composable("day/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            val day = entry.arguments?.getInt("day") ?: 1
                            DayScreen(database, day, speaker, onBack = { nav.safeBack() },
                                onStudy = { mode -> nav.navigate("study/$day/${mode.route}") })
                        }
                        composable("study/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            val day = entry.arguments?.getInt("day") ?: 1
                            StudyScreen(database, StudySource(day, wrongOnly = false), StudyMode.of(entry.arguments?.getString("mode")), speaker,
                                onRecorded = { model.refresh() }, onBack = { nav.safeBack() })
                        }
                        composable("wrong") {
                            WrongScreen(database, speaker, onBack = { nav.safeBack() },
                                onStudy = { day, mode -> nav.navigate("study/wrong/$day/${mode.route}") })
                        }
                        composable("study/wrong/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            val day = entry.arguments?.getInt("day") ?: 0
                            StudyScreen(database, StudySource(day, wrongOnly = true), StudyMode.of(entry.arguments?.getString("mode")), speaker,
                                onRecorded = { model.refresh() }, onBack = { nav.safeBack() })
                        }
                        if (debugContent != null) composable("debug") { debugContent { nav.safeBack() } }
                        composable("grammar") { GrammarFlow(onBack = { nav.safeBack() }) }
                    }
                }
            }
        }
    }
}

/** 뒤로 가기를 빠르게 여러 번 눌러 시작 화면까지 지워져 빈 화면이 되는 것을 막는다. */
private fun NavController.safeBack() {
    val current = currentBackStackEntry ?: return
    if (previousBackStackEntry != null && current.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}
