package com.jooh.opic.feature.grammar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GrammarExplanationScreen(unit: GrammarUnit, onBack: () -> Unit, onStart: () -> Unit) {
    Column {
        TextButton(onClick = onBack) { Text("← 단원 목록") }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(unit.title, style = MaterialTheme.typography.headlineSmall)
            Text(unit.explanation.summary, style = MaterialTheme.typography.bodyLarge)
            Text("핵심 규칙", style = MaterialTheme.typography.titleMedium)
            unit.explanation.points.forEach { Text("• $it") }
            Text("예문", style = MaterialTheme.typography.titleMedium)
            unit.explanation.examples.forEach { example ->
                Column { Text(example.en); Text(example.ko, style = MaterialTheme.typography.bodySmall) }
            }
            Text("흔한 실수", style = MaterialTheme.typography.titleMedium)
            unit.explanation.commonMistakes.forEach { mistake ->
                Column {
                    Text("✗ ${mistake.wrong}", color = Color(0xFFB3261E))
                    Text("✓ ${mistake.right}", color = Color(0xFF146C2E))
                    Text(mistake.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Text("문제 풀기") }
    }
}
