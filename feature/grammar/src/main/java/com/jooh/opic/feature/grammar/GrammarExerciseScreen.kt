package com.jooh.opic.feature.grammar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.jooh.opic.core.ui.Opic
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.GrammarFeedback

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GrammarExerciseScreen(
    state: GrammarUiState,
    onBack: () -> Unit,
    onInput: (String) -> Unit,
    onSelect: (Int) -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
) {
    val exercise = state.current ?: return
    Column {
        TextButton(onClick = onBack) { Text("← 설명") }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("${state.index + 1} / ${state.exercises.size}", style = MaterialTheme.typography.labelLarge)
            Text(exercise.prompt, style = MaterialTheme.typography.titleMedium)
            if (exercise.kind !in setOf("spot", "structure")) Text(exercise.sentence, style = MaterialTheme.typography.bodyLarge)
            if (exercise.kind in setOf("spot", "structure")) {
                // 틀린 곳 찾기·구조 찾기 (TASK 26): 문장의 단어를 눌러 고른다
                Text(if (exercise.kind == "spot") "틀린 단어를 누르세요" else "해당하는 단어를 누르세요",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exercise.choices.orEmpty().forEachIndexed { index, word ->
                        val finished = state.attempt.isFinished()
                        val isAnswer = finished && index == exercise.answer
                        val picked = state.selected == index
                        Surface(onClick = { onSelect(index) }, enabled = !finished, shape = MaterialTheme.shapes.small,
                            color = when { isAnswer -> Opic.colors.successBg; picked -> MaterialTheme.colorScheme.secondaryContainer; else -> MaterialTheme.colorScheme.surfaceVariant },
                            border = if (picked) BorderStroke(2.dp, Opic.colors.accent) else null) {
                            Text(word, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), style = MaterialTheme.typography.bodyLarge,
                                color = if (isAnswer) Opic.colors.success else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            } else if (exercise.kind == "choice") {
                exercise.choices.orEmpty().forEachIndexed { index, choice ->
                    OutlinedButton(onClick = { onSelect(index) }, enabled = !state.attempt.isFinished(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (state.selected == index) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSurface)
                            else ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)) { Text(choice) }
                }
            } else {
                OutlinedTextField(value = state.input, onValueChange = onInput, enabled = !state.attempt.isFinished(),
                    label = { Text(if (exercise.kind == "fix") "문장 전체" else "빈칸") },
                    modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 3)
            }
            when (state.attempt.feedback) {
                GrammarFeedback.HINT -> {
                    Text("다시 생각해 보세요", color = Opic.colors.warning)
                    Text(exercise.hint)
                }
                GrammarFeedback.REVEAL_ANSWER -> {
                    Text("정답: ${exercise.displayAnswer()}", color = Opic.colors.error)
                    Text(exercise.explanation)
                }
                GrammarFeedback.CORRECT_FIRST, GrammarFeedback.CORRECT_SECOND -> {
                    Text(if (state.attempt.feedback == GrammarFeedback.CORRECT_FIRST) "정답! 첫 시도에 맞혔어요" else "정답! 두 번째 시도에 맞혔어요",
                        color = Opic.colors.success)
                    Text(exercise.explanation)
                }
                null -> Unit
            }
        }
        Button(onClick = if (state.attempt.isFinished()) onNext else onSubmit,
            enabled = state.attempt.isFinished() || if (exercise.kind in TAP_KINDS) state.selected != null else state.input.isNotBlank(),
            modifier = Modifier.fillMaxWidth().imePadding().padding(vertical = 12.dp)) {
            Text(if (state.attempt.isFinished()) "다음" else "정답 확인")
        }
    }
}
