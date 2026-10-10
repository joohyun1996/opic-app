package com.jooh.opic.core.correction

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.llm.SharedModel
import com.jooh.opic.core.llm.llmFailureLabel

@Composable
fun LlmPreparationScreen(
    engineState: LlmEngineState,
    hasToken: Boolean,
    modelDownloaded: Boolean,
    modelBytes: Long,
    onSaveToken: (String) -> Unit,
    onDownload: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    backLabel: String = "← 결과",
    continueLabel: String = "쓰기 시작",
) {
    var token by remember { mutableStateOf("") }
    val context = LocalContext.current
    // 공용 폴더(Develop/Core/llm)의 모델을 여러 앱이 같이 쓴다 (ADR 002) — "모든 파일 접근"을 허용하고 돌아오면 바로 불러온다
    var hasAccess by remember { mutableStateOf(SharedModel.hasAccess()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val now = SharedModel.hasAccess()
                if (now && !hasAccess && SharedModel.isValid(modelBytes)) onDownload()
                hasAccess = now
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text(backLabel) }
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
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text(continueLabel) }
        }
        if (engineState is LlmEngineState.NotDownloaded || engineState is LlmEngineState.Failed) {
            when {
                !hasAccess -> {
                    Text("다른 앱(머니로그)과 같은 모델을 쓰려면 ${SharedModel.FOLDER_HINT}의 파일을 읽어야 해요. 설정에서 '모든 파일 접근 허용'을 켜 주세요 (모델 파일 읽기에만 써요)")
                    OutlinedButton(onClick = { context.startActivity(SharedModel.accessSettingsIntent(context)) }, modifier = Modifier.fillMaxWidth()) {
                        Text("공용 모델 쓰기 — 모든 파일 접근 허용")
                    }
                }
                !SharedModel.isValid(modelBytes) -> Text("${SharedModel.FOLDER_HINT}에 gemma-3n-e4b-it.task가 없거나 크기가 달라요")
                else -> OutlinedButton(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("공용 모델 불러오기") }
            }
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
