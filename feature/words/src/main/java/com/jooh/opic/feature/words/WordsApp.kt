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

enum class WordsPage { DAYS, DAY, STUDY, WRONG, WRONG_STUDY }

@Composable
fun WordsApp(
    database: OpicDatabase,
    importResult: ImportResult?,
    speaker: Speaker,
    page: WordsPage,
    grammarCount: Int?,
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
                    WordsPage.DAYS -> Column {
                        Spacer(Modifier.height(8.dp)) // 탭 첫 화면: 오른쪽 위 ≡와 같은 줄 (TASK 25)
                        Text("영단어", style = MaterialTheme.typography.headlineSmall)
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
