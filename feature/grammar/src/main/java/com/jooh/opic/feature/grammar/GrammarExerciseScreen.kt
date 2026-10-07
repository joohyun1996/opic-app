package com.jooh.opic.feature.grammar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.GrammarFeedback

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
            Text(exercise.sentence, style = MaterialTheme.typography.bodyLarge)
            if (exercise.kind == "choice") {
                exercise.choices.orEmpty().forEachIndexed { index, choice ->
                    OutlinedButton(onClick = { onSelect(index) }, enabled = !state.attempt.isFinished(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (state.selected == index) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFF1EFE8), contentColor = Color(0xFF1A1A18))
                            else ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1A1A18))) { Text(choice) }
                }
            } else {
                OutlinedTextField(value = state.input, onValueChange = onInput, enabled = !state.attempt.isFinished(),
                    label = { Text(if (exercise.kind == "fix") "문장 전체" else "빈칸") },
                    modifier = Modifier.fillMaxWidth(), singleLine = false, maxLines = 3)
            }
            when (state.attempt.feedback) {
                GrammarFeedback.HINT -> {
                    Text("다시 생각해 보세요", color = Color(0xFF9A5600))
                    Text(exercise.hint)
                }
                GrammarFeedback.REVEAL_ANSWER -> {
                    Text("정답: ${exercise.displayAnswer()}", color = Color(0xFFB3261E))
                    Text(exercise.explanation)
                }
                GrammarFeedback.CORRECT_FIRST, GrammarFeedback.CORRECT_SECOND -> {
                    Text(if (state.attempt.feedback == GrammarFeedback.CORRECT_FIRST) "정답! 첫 시도에 맞혔어요" else "정답! 두 번째 시도에 맞혔어요",
                        color = Color(0xFF146C2E))
                    Text(exercise.explanation)
                }
                null -> Unit
            }
        }
        Button(onClick = if (state.attempt.isFinished()) onNext else onSubmit,
            enabled = state.attempt.isFinished() || if (exercise.kind == "choice") state.selected != null else state.input.isNotBlank(),
            modifier = Modifier.fillMaxWidth().imePadding().padding(vertical = 12.dp)) {
            Text(if (state.attempt.isFinished()) "다음" else "정답 확인")
        }
    }
}
