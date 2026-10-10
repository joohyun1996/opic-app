package com.jooh.opic.feature.speaking

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.durationAdvice
import com.jooh.opic.core.common.fillerMask
import com.jooh.opic.core.common.paceAdvice
import com.jooh.opic.core.common.werWords
import com.jooh.opic.core.stt.PcmPlayer
import kotlinx.coroutines.launch

private val QUESTION_TYPE_KO = mapOf(
    "describe" to "묘사", "routine" to "습관·루틴", "experience" to "경험", "compare" to "비교",
    "roleplay_ask" to "롤플레이 · 질문하기", "roleplay_solve" to "롤플레이 · 문제 해결",
)

@Composable
fun SpeakingScreen(model: SpeakingViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (model.catalog == null) {
            TextButton(onClick = onBack) { Text("← 홈") }
            Text("스피킹 데이터를 불러오지 못했습니다", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        when (state.page) {
            SpeakingPage.TOPICS -> TopicsPage(model, onBack)
            SpeakingPage.QUESTION -> QuestionPage(model, state)
            SpeakingPage.RESULT -> ResultPage(model, state)
        }
    }
}

@Composable
private fun TopicsPage(model: SpeakingViewModel, onBack: () -> Unit) {
    TextButton(onClick = onBack) { Text("← 홈") }
    Text("스피킹", style = MaterialTheme.typography.headlineSmall)
    Text("질문을 듣고 바로 영어로 답해 보세요. 답변은 최대 2분입니다.")
    Button(onClick = model::randomQuestion, modifier = Modifier.fillMaxWidth()) { Text("무작위 질문") }
    model.catalog!!.topics.forEach { topic ->
        Card(onClick = { model.openTopic(topic.id) }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                Text(topic.titleKo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Text("${topic.questions.size}문항", color = Color.Gray)
            }
        }
    }
}

@Composable
private fun QuestionPage(model: SpeakingViewModel, state: SpeakingState) {
    val question = state.question ?: return
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) model.startRecording() }
    val topic = model.catalog?.topics?.firstOrNull { it.id == state.topicId }
    TextButton(onClick = model::backToTopics) { Text("← 주제") }
    Text("${topic?.titleKo.orEmpty()} · ${QUESTION_TYPE_KO[question.type].orEmpty()}", style = MaterialTheme.typography.titleMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = state.replaysLeft > 0 && !state.recording, onClick = model::replay) {
            Text(if (state.replaysLeft > 0) "다시 듣기 (1회)" else "다시 듣기 끝")
        }
        OutlinedButton(onClick = model::toggleText) { Text(if (state.showText) "질문 글 숨기기" else "질문 글 보기") }
    }
    if (state.showText) Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(question.en, style = MaterialTheme.typography.bodyLarge)
            Text(question.ko, color = Color.Gray)
            Text("팁: ${question.tip}", style = MaterialTheme.typography.bodySmall)
        }
    }
    HorizontalDivider()
    ModelSection(model, state)
    if (state.model == ModelState.READY) {
        if (state.recording) {
            Text("${clock(state.elapsedMs)} / ${clock(MAX_ANSWER_MS)}", style = MaterialTheme.typography.headlineMedium)
            LinearProgressIndicator(progress = { state.level }, modifier = Modifier.fillMaxWidth())
            Button(onClick = model::stopRecording, modifier = Modifier.fillMaxWidth()) { Text("■ 끝") }
        } else if (state.transcribing) {
            Text("받아 적는 중… (2분 답변은 30~40초 걸려요)")
            LinearProgressIndicator(Modifier.fillMaxWidth())
            TextButton(onClick = model::cancelTranscribe) { Text("취소") }
        } else {
            Button(onClick = { permission.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth()) { Text("● 답변 시작") }
        }
    }
    state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun ModelSection(model: SpeakingViewModel, state: SpeakingState) {
    when (state.model) {
        ModelState.READY -> Unit
        ModelState.DOWNLOADING -> Text("음성 인식 모델 받는 중 ${state.progress}%")
        ModelState.LOADING -> Text("음성 인식 모델 불러오는 중…")
        ModelState.MISSING -> Button(onClick = model::prepareModel) { Text("음성 인식 모델 받기 (190MB)") }
        ModelState.NOT_LOADED -> Button(onClick = model::prepareModel) { Text("음성 인식 모델 불러오기") }
        ModelState.FAILED -> Button(onClick = model::prepareModel) { Text("다시 시도") }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultPage(model: SpeakingViewModel, state: SpeakingState) {
    val transcript = state.transcript ?: return
    val metrics = state.metrics ?: return
    val scope = rememberCoroutineScope()
    TextButton(onClick = model::backToTopics) { Text("← 주제") }
    Text("내 답변", style = MaterialTheme.typography.titleMedium)
    val tokens = remember(transcript) { transcript.split(Regex("\\s+")).filter { it.isNotEmpty() } }
    val mask = remember(tokens) { fillerMask(tokens.map { werWords(it).firstOrNull().orEmpty() }) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        tokens.forEachIndexed { i, token -> Text(token, color = if (mask[i]) Color.Gray else Color.Unspecified) }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("말한 시간 ${clock(metrics.durationMs)}" + (durationAdvice(metrics.durationMs)?.let { " · $it" } ?: ""))
            Text("분당 ${metrics.wordsPerMinute}단어 · ${paceAdvice(metrics.wordsPerMinute)}")
            Text("단어 ${metrics.wordCount}개 · 문장 ${metrics.sentenceCount}개")
            Text("머뭇거림 ${metrics.fillerCount}번 (um, uh, you know 등 — 회색 표시)")
            if (metrics.repeatedWords.isNotEmpty()) Text("자주 쓴 단어: " + metrics.repeatedWords.joinToString { "${it.first} ${it.second}번" })
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = model::retry) { Text("다시 답하기") }
        OutlinedButton(onClick = model::nextQuestion) { Text("다음 질문") }
    }
    OutlinedButton(enabled = model.recordingFile.isFile, onClick = { scope.launch { PcmPlayer.play(model.recordingFile) } }) { Text("내 답변 듣기") }
}

private fun clock(ms: Long): String { val s = ms / 1000; return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }
