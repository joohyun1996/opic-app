package com.jooh.opic.core.correction

import com.jooh.opic.core.ui.Opic
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.CorrectionError
import com.jooh.opic.core.common.errorTypeGuide

@Composable
fun CorrectionResultCard(outcome: CorrectionOutcome, onRetry: () -> Unit, retryEnabled: Boolean) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (outcome) {
                is CorrectionOutcome.Correct -> {
                    Text("✓ 맞아요", color = Opic.colors.success, style = MaterialTheme.typography.titleMedium)
                    Text(outcome.sentence)
                }
                is CorrectionOutcome.Incorrect -> {
                    Text("고쳐 볼 부분", style = MaterialTheme.typography.titleMedium)
                    Text(highlightOriginal(outcome.sentence, outcome.result.errors, Opic.colors.error))
                    Text("→ ${outcome.result.corrected}", color = Opic.colors.success)
                    outcome.result.errors.forEach { error ->
                        val guide = errorTypeGuide(error.type)
                        Text(guide.name, style = MaterialTheme.typography.titleMedium)
                        Text(guide.explanation, style = MaterialTheme.typography.bodyLarge)
                        Text("${error.original} → ${error.fix}")
                        var expanded by remember { mutableStateOf(false) }
                        TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "AI 설명 접기" else "AI 설명 보기") }
                        if (expanded) {
                            Text("AI가 쓴 설명이라 틀릴 수 있어요", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            Text(error.explanationKo, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 6, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                is CorrectionOutcome.Failed -> {
                    Text("이 문장은 교정하지 못했습니다", color = MaterialTheme.colorScheme.error)
                    Text(outcome.sentence)
                    TextButton(onClick = onRetry, enabled = retryEnabled) { Text("다시 시도") }
                }
            }
            Text("${outcome.elapsedMs}ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun highlightOriginal(sentence: String, errors: List<CorrectionError>, wrong: Color) = buildAnnotatedString {
    append(sentence)
    errors.forEach { error ->
        val index = sentence.indexOf(error.original, ignoreCase = true)
        if (index >= 0) addStyle(SpanStyle(color = wrong, textDecoration = TextDecoration.LineThrough),
            index, index + error.original.length)
    }
}
