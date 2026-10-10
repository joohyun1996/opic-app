package com.jooh.opic

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.database.BackupManager
import com.jooh.opic.core.llm.ModelCatalog
import com.jooh.opic.core.llm.SharedModel
import com.jooh.opic.core.stt.SttModels
import com.jooh.opic.core.ui.Opic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun gemmaStatus(app: OpicApplication): String {
    val bytes = ModelCatalog.config(app).models.first().expectedBytes
    return when {
        SharedModel.isValid(bytes) -> "공용 모델"
        ModelCatalog.isDownloaded(app) -> "앱 안"
        else -> "없음"
    }
}

/** AI 모델 상태 (TASK 24). 공용 Gemma(ADR 002)는 모든 파일 접근이 필요하다. */
@Composable
internal fun ModelsDialog(app: OpicApplication, onDismiss: () -> Unit) {
    val bytes = ModelCatalog.config(app).models.first().expectedBytes
    AlertDialog(onDismissRequest = onDismiss, title = { Text("AI 모델") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("문법 교정 (Gemma 3n E4B, 4.4GB): ${gemmaStatus(app)}")
            Text("공용 위치: 내장 저장공간/${SharedModel.FOLDER_HINT}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!SharedModel.hasAccess()) OutlinedButton(onClick = { app.startActivity(SharedModel.accessSettingsIntent(app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                Text("모든 파일 접근 허용 (공용 모델 읽기)")
            } else if (!SharedModel.isValid(bytes)) Text("공용 위치에 모델 파일이 없어요", color = Opic.colors.warning)
            Text("음성 인식 (Whisper small.en, 190MB): " + if (SttModels.isDownloaded(app.whisper.model)) "받음" else "없음 — 섀도잉·스피킹에서 받기")
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
}

/** 학습 기록 백업·복원 (TASK 22). 파일 위치는 시스템 선택 창으로 사용자가 고른다 (새 권한 없음). */
@Composable
internal fun BackupDialog(app: OpicApplication, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    val manager = remember(app) { BackupManager(app.database) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                val raw = manager.export(System.currentTimeMillis())
                withContext(Dispatchers.IO) { app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(raw.toByteArray()) } }
                "백업 파일을 만들었어요 (${raw.length / 1024}KB)"
            }.getOrElse { "백업 실패: ${it.message}" }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                val raw = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                val r = manager.restore(raw) ?: return@runCatching "백업 파일 형식이 아니에요. 아무것도 바꾸지 않았어요"
                "복원했어요 — 단어 ${r.userWords}개, 문법 복습 ${r.grammarReviews}개, 스피킹 답변 ${r.speakingAnswers}개, 섀도잉 ${r.shadowingAttempts}개" +
                    if (r.skippedWords > 0) " (없는 단어 ${r.skippedWords}개 건너뜀)" else ""
            }.getOrElse { "복원 실패: ${it.message}" }
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("학습 기록 백업·복원") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("단어 학습 기록, 문법 복습, 스피킹 답변, 섀도잉 연습을 파일 하나로 저장해요. 녹음 파일은 포함하지 않아요.")
            Text("복원은 기존 기록과 합쳐요: 더 최근 기록을 남기고, 같은 답변은 두 번 들어가지 않아요.")
            Button(onClick = { create.launch("opic-backup-${java.time.LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) { Text("백업 파일 만들기") }
            OutlinedButton(onClick = { open.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("백업에서 복원") }
            status?.let { Text(it) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
}
