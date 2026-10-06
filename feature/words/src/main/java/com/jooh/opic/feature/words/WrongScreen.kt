package com.jooh.opic.feature.words

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jooh.opic.core.common.dayOf
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.WordWithProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class WrongViewModel(private val database: OpicDatabase) : ViewModel() {
    private val mutableRows = MutableStateFlow<List<WordWithProgress>?>(null)
    val rows = mutableRows.asStateFlow()

    fun refresh() {
        viewModelScope.launch { mutableRows.value = database.wordDao().getWrongWords("en", 1, Int.MAX_VALUE) }
    }

    class Factory(private val database: OpicDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = WrongViewModel(database) as T
    }
}

/** ⑥ 오답 모음. day = 0은 전체. */
@Composable
internal fun WrongScreen(database: OpicDatabase, speaker: Speaker, onBack: () -> Unit, onStudy: (day: Int, StudyMode) -> Unit) {
    val model: WrongViewModel = viewModel(key = "wrong", factory = WrongViewModel.Factory(database))
    val rows by model.rows.collectAsState()
    LaunchedEffect(Unit) { model.refresh() }
    DisposableEffect(Unit) { onDispose { speaker.stop() } }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val all = rows.orEmpty()
    val byDay = all.groupBy { dayOf(it.word.seq) }
    if (filter != 0 && filter !in byDay) filter = 0
    val shown = if (filter == 0) all else byDay[filter].orEmpty()

    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) { Text("← 뒤로") }
        Text("오답 모음", style = MaterialTheme.typography.headlineSmall)
        Text("오답 ${all.size}개", Modifier.padding(vertical = 8.dp), color = WrongInk)
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == 0, onClick = { filter = 0 }, label = { Text("전체") }, colors = chipColors())
            for ((day, words) in byDay.toSortedMap()) {
                FilterChip(selected = filter == day, onClick = { filter = day }, label = { Text("Day $day (${words.size})") }, colors = chipColors())
            }
        }
        when {
            rows == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink) }
            all.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("오답이 없습니다") }
            else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.word.id }) { row ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.height(IntrinsicSize.Min)) {
                            Box(Modifier.width(4.dp).fillMaxHeight().background(WrongInk))
                            Column(Modifier.weight(1f).padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(row.word.word, style = MaterialTheme.typography.titleMedium)
                                    SpeakButton(speaker, row.word.word)
                                    Text(row.word.phonetic, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Text(row.word.meaningKo.removePrefix("*"))
                                Text("틀린 횟수 ${row.wrongCount ?: 0} · Day ${dayOf(row.word.seq)}",
                                    style = MaterialTheme.typography.bodySmall, color = WrongInk)
                            }
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (mode in StudyMode.entries) {
                Button(onClick = { onStudy(filter, mode) }, enabled = shown.isNotEmpty(), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("${mode.title} 학습") }
            }
        }
    }
}

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(selectedContainerColor = WrongBg, selectedLabelColor = WrongInk)
