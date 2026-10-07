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
            Text("${state.unit?.title} 결과", style = MaterialTheme.typography.headlineSmall)
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
            OutlinedButton(onClick = onWrite, modifier = Modifier.fillMaxWidth()) { Text("직접 써 보기") }
        }
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("다시 풀기") }
        TextButton(onClick = onList, modifier = Modifier.fillMaxWidth()) { Text("단원 목록") }
    }
}
