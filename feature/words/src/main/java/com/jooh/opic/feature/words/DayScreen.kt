package com.jooh.opic.feature.words

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jooh.opic.core.common.seqRange
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.WordWithProgress
import com.jooh.opic.core.model.WordStatus
import com.jooh.opic.core.model.wordStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DayViewModel(private val database: OpicDatabase, private val day: Int) : ViewModel() {
    private val mutableRows = MutableStateFlow<List<WordWithProgress>?>(null)
    val rows = mutableRows.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val range = seqRange(day)
            mutableRows.value = database.wordDao().getDayWordsWithProgress("en", range.first, range.last)
        }
    }

    class Factory(private val database: OpicDatabase, private val day: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = DayViewModel(database, day) as T
    }
}

/** ③ Day 단어 목록 */
@Composable
internal fun DayScreen(database: OpicDatabase, day: Int, speaker: Speaker, onBack: () -> Unit, onStudy: (StudyMode) -> Unit) {
    val model: DayViewModel = viewModel(key = "day-$day", factory = DayViewModel.Factory(database, day))
    val rows by model.rows.collectAsState()
    LaunchedEffect(Unit) { model.refresh() }
    DisposableEffect(Unit) { onDispose { speaker.stop() } }
    val list = rows.orEmpty()
    var showKorean by koreanVisible
    val mastered = list.count { wordStatus(it.correctCount) == WordStatus.MASTERED }

    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack) { Text("← Day 목록") }
        Text("Day $day", style = MaterialTheme.typography.headlineSmall)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("습득 $mastered / ${list.size}", Modifier.weight(1f).padding(vertical = 8.dp))
            TextButton(onClick = { showKorean = !showKorean }) { Text(if (showKorean) "한국어 숨기기" else "한국어 보기") }
        }
        LinearProgressIndicator(
            progress = { if (list.isEmpty()) 0f else mastered.toFloat() / list.size },
            modifier = Modifier.fillMaxWidth(), color = Ink, trackColor = New, drawStopIndicator = {},
        )
        if (rows == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink) }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                items(list, key = { it.word.id }) { row ->
                    val masteredWord = wordStatus(row.correctCount) == WordStatus.MASTERED
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(row.word.word, style = MaterialTheme.typography.titleMedium)
                                SpeakButton(speaker, row.word.word)
                                if ((row.wrongCount ?: 0) > 0 && (row.correctCount ?: 0) < 3) WrongBadge(row.wrongCount ?: 0)
                            }
                            Text(row.word.phonetic, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (showKorean) Text(
                            row.word.meaningKo.removePrefix("*"), Modifier.weight(1f).padding(start = 8.dp),
                            color = if (masteredWord) RightInk else Ink, maxLines = 2, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End, style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    HorizontalDivider(color = Line)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (mode in StudyMode.entries) {
                Button(onClick = { onStudy(mode) }, enabled = list.isNotEmpty(), modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("${mode.title} 학습") }
            }
        }
    }
}

/** 한국어 보기 선택은 앱이 살아 있는 동안 화면을 오가도 유지한다. */
private val koreanVisible = mutableStateOf(true)
