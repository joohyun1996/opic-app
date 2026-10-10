package com.jooh.opic.feature.grammar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GrammarResultScreen(state: GrammarUiState, onRetry: () -> Unit, onList: () -> Unit, onWrite: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (state.reviewMode) "오늘의 복습 결과" else "${state.unit?.title} 결과", style = MaterialTheme.typography.headlineSmall)
            Text("1차 정답 ${state.score.firstTry}개")
            Text("2차 정답 ${state.score.secondTry}개")
            Text("틀린 문제 ${state.score.wrong}개")
            if (state.missed.isNotEmpty()) Text("다시 볼 문제", style = MaterialTheme.typography.titleMedium)
            state.missed.forEach { exercise ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(exercise.sentence)
                        Text("정답: ${exercise.displayAnswer()}")
                    }
                }
            }
            if (state.reviewMode) Text("1차에 맞힌 문제는 다음 단계로, 나머지는 내일 다시 나옵니다.", style = MaterialTheme.typography.bodySmall)
            else if (state.unit?.writingTask != null) OutlinedButton(onClick = onWrite, modifier = Modifier.fillMaxWidth()) { Text("직접 써 보기") }
        }
        if (!state.reviewMode) Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("다시 풀기") }
        TextButton(onClick = onList, modifier = Modifier.fillMaxWidth()) { Text("단원 목록") }
    }
}
