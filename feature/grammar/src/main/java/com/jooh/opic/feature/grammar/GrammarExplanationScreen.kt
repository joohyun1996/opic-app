package com.jooh.opic.feature.grammar

import com.jooh.opic.core.ui.Opic
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun FormTable(rows: List<List<String>>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            rows.forEachIndexed { r, row ->
                if (r > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { cell ->
                        Text(cell, Modifier.weight(1f), style = if (r == 0) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                            color = if (r == 0) Opic.colors.accent else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

/** 역할 → 이름·색. S 주어(파랑), V 동사(초록), O 목적어(주황), C 보어(보라), M 수식어(회색) — 밝기까지 달라 구분되게. */
@Composable
internal fun roleStyle(role: String): Pair<String, Color> {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val blue = if (dark) Color(0xFF8EA0FF) else Color(0xFF1E5BD8)
    val purple = if (dark) Color(0xFFD2A8FF) else Color(0xFF7A3BB8)
    return when (role) {
        "S" -> "주어" to blue
        "V" -> "동사" to Opic.colors.success
        "O" -> "목적어" to Opic.colors.warning
        "IO" -> "간접목적어" to Opic.colors.warning
        "DO" -> "직접목적어" to Opic.colors.warning
        "C" -> "보어" to purple
        "OC" -> "목적격보어" to purple
        "M" -> "수식어" to MaterialTheme.colorScheme.onSurfaceVariant
        else -> role to MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoleLegend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("S", "V", "O", "C", "M").forEach { val (name, color) = roleStyle(it); Text("$it $name", color = color, style = MaterialTheme.typography.labelMedium) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Breakdown(item: GrammarBreakdown) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item.parts.forEach { part ->
                    val (name, color) = roleStyle(part.role)
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(part.text, style = MaterialTheme.typography.bodyLarge, color = color)
                        Text("${part.role} $name", style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
            item.note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun GrammarExplanationScreen(unit: GrammarUnit, onBack: () -> Unit, onStart: () -> Unit) {
    Column {
        TextButton(onClick = onBack) { Text("← 단원 목록") }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(unit.title, style = MaterialTheme.typography.headlineSmall)
            Text(unit.explanation.summary, style = MaterialTheme.typography.bodyLarge)
            // 실전 영문법 항목 (TASK 26) — 있을 때만
            unit.explanation.concept?.let { Section("왜 이렇게 쓸까") { Text(it, style = MaterialTheme.typography.bodyLarge) } }
            unit.explanation.table?.takeIf { it.isNotEmpty() }?.let { Section("한눈에 보기") { FormTable(it) } }
            unit.explanation.koreanNote?.let { Section("한국어와 다른 점") { Text(it) } }
            if (unit.explanation.breakdowns.isNotEmpty()) Section("문장 구조 분해") {
                RoleLegend()
                unit.explanation.breakdowns.forEach { Breakdown(it) }
            }
            if (unit.explanation.points.isNotEmpty()) Text("핵심 규칙", style = MaterialTheme.typography.titleMedium)
            unit.explanation.points.forEach { Text("• $it") }
            if (unit.explanation.examples.isNotEmpty()) Text("예문", style = MaterialTheme.typography.titleMedium)
            unit.explanation.examples.forEach { example ->
                Column { Text(example.en); Text(example.ko, style = MaterialTheme.typography.bodySmall) }
            }
            Text("흔한 실수", style = MaterialTheme.typography.titleMedium)
            unit.explanation.commonMistakes.forEach { mistake ->
                Column {
                    Text("✗ ${mistake.wrong}", color = Opic.colors.error)
                    Text("✓ ${mistake.right}", color = Opic.colors.success)
                    Text(mistake.note, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Text("문제 풀기") }
    }
}
