package com.jooh.opic.feature.grammar

import com.jooh.opic.core.correction.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.splitSentences

@Composable
fun GrammarWritingScreen(
    state: WritingUiState,
    onBack: () -> Unit,
    onInput: (String) -> Unit,
    onCorrect: () -> Unit,
    onCancel: () -> Unit,
    onRetry: (Int) -> Unit,
    onAgain: () -> Unit,
    onList: () -> Unit,
) {
    val unit = state.unit ?: return
    val count = splitSentences(state.input).size
    Column {
        TextButton(onClick = onBack) { Text("← 결과") }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(unit.writingTask.promptKo, style = MaterialTheme.typography.headlineSmall)
            Text(unit.writingTask.promptEn, style = MaterialTheme.typography.bodySmall)
            Text("최소 ${unit.writingTask.minSentences}문장 · 현재 ${count}문장")
            if (count > 5) Text("앞의 5문장만 교정합니다", color = MaterialTheme.colorScheme.error)
            OutlinedTextField(value = state.input, onValueChange = onInput, enabled = !state.isCorrecting,
                label = { Text("영어로 직접 쓰기") }, modifier = Modifier.fillMaxWidth(), minLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences,
                    autoCorrect = false, keyboardType = KeyboardType.Ascii))
            if (state.isCorrecting) {
                Text("${state.currentNumber} / ${state.total} 문장 교정 중… (문장당 약 20초)")
                TextButton(onClick = onCancel) { Text("취소") }
            }
            state.results.forEachIndexed { index, outcome ->
                CorrectionResultCard(outcome, onRetry = { onRetry(index) }, retryEnabled = !state.isCorrecting)
            }
            if (state.results.isNotEmpty() && !state.isCorrecting) {
                OutlinedButton(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text("다시 쓰기") }
                TextButton(onClick = onList, modifier = Modifier.fillMaxWidth()) { Text("단원 목록") }
            }
        }
        if (!state.isCorrecting && state.results.isEmpty()) {
            Button(onClick = onCorrect, enabled = count >= unit.writingTask.minSentences,
                modifier = Modifier.fillMaxWidth().imePadding().padding(vertical = 12.dp)) { Text("교정 받기") }
        }
    }
}
