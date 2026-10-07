package com.jooh.opic.feature.grammar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.llm.llmFailureLabel

@Composable
fun GrammarPreparationScreen(
    engineState: LlmEngineState,
    hasToken: Boolean,
    modelDownloaded: Boolean,
    modelBytes: Long,
    onSaveToken: (String) -> Unit,
    onDownload: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    var token by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← 결과") }
        Text("AI 교정 준비", style = MaterialTheme.typography.headlineSmall)
        when (engineState) {
            LlmEngineState.NotDownloaded -> Text("모델 없음")
            is LlmEngineState.Downloading -> {
                Text("다운로드 중 ${(engineState.progress * 100).toInt()}% · ${engineState.bytes / 1_000_000} / ${engineState.total / 1_000_000} MB")
                LinearProgressIndicator(progress = { engineState.progress }, modifier = Modifier.fillMaxWidth())
            }
            is LlmEngineState.Loading -> Text("모델을 불러오는 중…")
            is LlmEngineState.Ready -> Text("준비됨")
            is LlmEngineState.Generating -> Text("교정 중…")
            is LlmEngineState.Failed -> Text("실패: ${llmFailureLabel(engineState.reason)}")
            LlmEngineState.Closed -> Text("엔진을 사용할 수 없습니다")
        }
        if (engineState is LlmEngineState.Ready) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("쓰기 시작") }
        }
        if (engineState is LlmEngineState.NotDownloaded || engineState is LlmEngineState.Failed) {
            if (modelDownloaded) Text("저장된 모델을 다시 불러올 수 있습니다.")
            else Text("AI 교정을 쓰려면 모델(약 ${"%.1f".format(modelBytes / 1_000_000_000.0)} GB)을 한 번 받아야 합니다. Wi-Fi에서 받으세요.")
            if (!modelDownloaded) {
                Text(if (hasToken) "Hugging Face 토큰 저장됨" else "Hugging Face 토큰을 입력하세요")
                OutlinedTextField(value = token, onValueChange = { token = it }, visualTransformation = PasswordVisualTransformation(),
                    label = { Text("Hugging Face 토큰") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (token.isNotBlank()) Button(onClick = { onSaveToken(token); token = "" }) { Text("토큰 저장") }
            }
            Button(onClick = onDownload, enabled = modelDownloaded || hasToken,
                modifier = Modifier.fillMaxWidth()) { Text(if (modelDownloaded) "다시 불러오기" else "다운로드") }
        }
    }
}
