package com.jooh.opic.feature.grammar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GrammarUnitListScreen(
    units: List<GrammarUnit>, aiStatus: String, onBack: () -> Unit, onUnit: (GrammarUnit) -> Unit,
    dueCount: Int = 0, onReview: () -> Unit = {},
) {
    Column {
        Spacer(Modifier.height(8.dp)) // 탭 첫 화면: 오른쪽 위 ≡와 같은 줄 (TASK 25)
        Text("영문법", style = MaterialTheme.typography.headlineSmall)
        if (dueCount > 0) Button(onClick = onReview, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("오늘의 복습 ${dueCount}개 →") }
        else Text("오늘 복습 끝!", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodyMedium)
        Text("AI 교정: $aiStatus", modifier = Modifier.padding(top = 8.dp))
        Text("${units.size}개 단원", modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(units.sortedBy { it.order }, key = { it.id }) { unit ->
                Card(onClick = { onUnit(unit) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("단원 ${unit.order} · ${unit.title}", style = MaterialTheme.typography.titleMedium)
                        Text(unit.opicUse)
                        Text("문제 ${unit.exercises.size}개 →", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
