package com.jooh.opic.feature.grammar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GrammarUnitListScreen(units: List<GrammarUnit>, onBack: () -> Unit, onUnit: (GrammarUnit) -> Unit) {
    Column {
        TextButton(onClick = onBack) { Text("← 홈") }
        Text("영어 문법", style = MaterialTheme.typography.headlineSmall)
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
