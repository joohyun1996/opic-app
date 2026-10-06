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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jooh.opic.core.database.ImportResult
import com.jooh.opic.core.database.OpicDatabase

private val Ink = Color(0xFF1A1A18)
private val Paper = Color(0xFFF8F7F4)
private val New = Color(0xFFF1EFE8)
private val Learned = Color(0xFFEAF3DE)
private val Learning = Color(0xFFFAEEDA)

@Composable
fun WordsApp(database: OpicDatabase, importResult: ImportResult?, debugContent: (@Composable (() -> Unit) -> Unit)? = null) {
    val model: WordsViewModel = viewModel(factory = remember(database) { WordsViewModel.Factory(database) })
    val state by model.state.collectAsState()
    LaunchedEffect(importResult) { model.refresh() }
    val nav = rememberNavController()
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
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("나의 언어 학습", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 20.dp))
                                Text("오늘도 한 걸음씩", style = MaterialTheme.typography.bodyLarge)
                                Card(onClick = { nav.navigate("days") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("영어 (OPIc)", style = MaterialTheme.typography.titleLarge)
                                        Text("습득 ${state.mastered} / ${state.total}개")
                                        LinearProgressIndicator(progress = { if (state.total == 0) 0f else state.mastered.toFloat() / state.total }, modifier = Modifier.fillMaxWidth(), color = Ink, trackColor = New)
                                        Text("Day ${state.days.size}개 · 학습 시작 →")
                                    }
                                }
                                Card(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(disabledContainerColor = New)) {
                                    Text("중국어 (HSK) — 준비 중", Modifier.padding(20.dp))
                                }
                                if (debugContent != null) TextButton(onClick = { nav.navigate("debug") }) { Text("LLM 검증") }
                            }
                        }
                        composable("days") {
                            Column {
                                TextButton(onClick = { nav.popBackStack() }) { Text("← 홈") }
                                Text("영어 · Day 목록", style = MaterialTheme.typography.headlineSmall)
                                Text("${state.days.size}일 · ${state.total}개 단어", Modifier.padding(vertical = 8.dp))
                                if (state.wrong > 0) Surface(color = Color(0xFFFCEBEB), shape = MaterialTheme.shapes.small) {
                                    Text("오답 ${state.wrong}개", Modifier.padding(8.dp), color = Color(0xFF9B2626))
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
                        composable("day/{day}") { entry ->
                            Column {
                                TextButton(onClick = { nav.popBackStack() }) { Text("← Day 목록") }
                                Text("Day ${entry.arguments?.getString("day")} — 준비 중", style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                        if (debugContent != null) composable("debug") { debugContent { nav.popBackStack() } }
                    }
                }
            }
        }
    }
}
