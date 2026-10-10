package com.jooh.opic.feature.grammar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GrammarUnitListScreen(
    units: List<GrammarUnit>, aiStatus: String, onBack: () -> Unit, onUnit: (GrammarUnit) -> Unit,
    dueCount: Int = 0, onReview: () -> Unit = {},
    // OPIc 문법 | 실전 영문법 (TASK 26) — 선택은 GrammarFlow가 들고 있어 장에 들어갔다 나와도 유지
    track: String = GrammarTracks.OPIC, onTrack: (String) -> Unit = {},
) {
    val shown = units.filter { it.track == track }
    Column {
        Spacer(Modifier.height(8.dp)) // 탭 첫 화면: 오른쪽 위 ≡와 같은 줄 (TASK 25)
        Text("영문법", style = MaterialTheme.typography.headlineSmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            listOf(GrammarTracks.OPIC to "OPIc 문법", GrammarTracks.CORE to "실전 영문법").forEachIndexed { i, (id, label) ->
                SegmentedButton(selected = track == id, onClick = { onTrack(id) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
            }
        }
        if (dueCount > 0) Button(onClick = onReview, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("오늘의 복습 ${dueCount}개 →") }
        else Text("오늘 복습 끝!", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
        Text("AI 교정: $aiStatus", modifier = Modifier.padding(top = 8.dp))
        Text(if (track == GrammarTracks.CORE) "문장 구조부터 차근차근 · ${shown.size}장" else "${shown.size}개 단원", modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(shown.sortedBy { it.order }, key = { it.id }) { unit ->
                Card(onClick = { onUnit(unit) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        unit.part?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = com.jooh.opic.core.ui.Opic.colors.accent) }
                        Text(if (track == GrammarTracks.CORE) "${unit.order}장 · ${unit.title}" else "단원 ${unit.order} · ${unit.title}", style = MaterialTheme.typography.titleMedium)
                        Text(unit.opicUse)
                        Text("문제 ${unit.exercises.size}개 →", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
