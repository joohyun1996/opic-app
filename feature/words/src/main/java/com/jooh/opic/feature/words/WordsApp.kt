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
import com.jooh.opic.core.database.ImportResult
import com.jooh.opic.core.database.OpicDatabase

enum class WordsPage { HOME, DAYS, DAY, STUDY, WRONG, WRONG_STUDY }

@Composable
fun WordsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = Color.White, background = Paper,
        surface = Paper, onSurface = Ink, onBackground = Ink, outline = Color(0xFFE8E6E0)), content = content)
}

@Composable
fun WordsApp(
    database: OpicDatabase,
    importResult: ImportResult?,
    speaker: Speaker,
    page: WordsPage,
    grammarCount: Int?,
    debugAvailable: Boolean,
    grammarDue: Int = 0,
    day: Int = 0,
    mode: String? = null,
    navigate: (String) -> Unit,
    onBack: () -> Unit,
) {
    val model: WordsViewModel = viewModel(factory = remember(database) { WordsViewModel.Factory(database) })
    val state by model.state.collectAsState()
    LaunchedEffect(importResult, page) { model.refresh() }
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
                when (page) {
                    WordsPage.HOME -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("나의 언어 학습", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 20.dp))
                        Text("오늘도 한 걸음씩", style = MaterialTheme.typography.bodyLarge)
                        Card(onClick = { navigate("days") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("영단어", style = MaterialTheme.typography.titleLarge)
                                Text("습득 ${state.mastered} / ${state.total}개")
                                LinearProgressIndicator(progress = { if (state.total == 0) 0f else state.mastered.toFloat() / state.total },
                                    modifier = Modifier.fillMaxWidth(), color = Ink, trackColor = New, drawStopIndicator = {})
                                Text("Day ${state.days.size}개")
                            }
                        }
                        if (state.wrong > 0) TextButton(onClick = { navigate("wrong") }) { Text("오답노트 ${state.wrong}개 →", color = WrongInk) }
                        Card(onClick = { navigate("grammar") }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("영문법", style = MaterialTheme.typography.titleLarge)
                                Text(if (grammarCount == null) "문법 데이터를 확인하세요 →" else "단원 ${grammarCount}개")
                                if (grammarDue > 0) Text("오늘의 복습 ${grammarDue}개", color = WrongInk)
                            }
                        }
                        Card(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(disabledContainerColor = New)) {
                            Text("중국어 (HSK) — 준비 중", Modifier.padding(20.dp))
                        }
                        if (debugAvailable) TextButton(onClick = { navigate("debug") }) { Text("개발자 검증") }
                    }
                    WordsPage.DAYS -> Column {
                        TextButton(onClick = onBack) { Text("← 홈") }
                        Text("영어 · Day 목록", style = MaterialTheme.typography.headlineSmall)
                        Text("${state.days.size}일 · ${state.total}개 단어", Modifier.padding(vertical = 8.dp))
                        if (state.wrong > 0) Surface(onClick = { navigate("wrong") }, color = WrongBg, shape = MaterialTheme.shapes.small) {
                            Text("오답 ${state.wrong}개 →", Modifier.padding(8.dp), color = WrongInk)
                        }
                        LazyVerticalGrid(columns = GridCells.Fixed(4), contentPadding = PaddingValues(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.days, key = { it.day }) { item ->
                                val color = when { item.total > 0 && item.mastered == item.total -> Learned; item.mastered > 0 -> Learning; else -> New }
                                Card(onClick = { navigate("day/${item.day}") }, colors = CardDefaults.cardColors(containerColor = color)) {
                                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Day ${item.day}", style = MaterialTheme.typography.labelLarge)
                                        Text("${item.mastered}/${item.total}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    WordsPage.DAY -> DayScreen(database, day, speaker, onBack = onBack,
                        onStudy = { navigate("study/$day/${it.route}") })
                    WordsPage.STUDY -> StudyScreen(database, StudySource(day, wrongOnly = false), StudyMode.of(mode), speaker,
                        onRecorded = model::refresh, onBack = onBack)
                    WordsPage.WRONG -> WrongScreen(database, speaker, onBack = onBack,
                        onStudy = { selectedDay, selectedMode -> navigate("study/wrong/$selectedDay/${selectedMode.route}") })
                    WordsPage.WRONG_STUDY -> StudyScreen(database, StudySource(day, wrongOnly = true), StudyMode.of(mode), speaker,
                        onRecorded = model::refresh, onBack = onBack)
                }
            }
        }
    }
}
