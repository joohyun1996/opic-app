package com.jooh.opic.feature.speaking

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.EditState
import com.jooh.opic.core.common.EditableWord
import com.jooh.opic.core.common.durationAdvice
import com.jooh.opic.core.correction.CorrectionResultCard
import com.jooh.opic.core.correction.LlmPreparationScreen
import com.jooh.opic.core.llm.LlmEngineState
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ResultPage(model: SpeakingViewModel, state: SpeakingState) {
    val transcript = state.transcript ?: return
    val metrics = state.metrics ?: return
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Int?>(null) }
    TextButton(onClick = model::backToTopics) { Text("← 주제") }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (state.showOriginal) "Whisper 받아 적기" else "내 답변", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = model::toggleOriginal) { Text(if (state.showOriginal) "내가 고친 글 보기" else "Whisper 원문 보기") }
    }
    if (state.showOriginal) {
        Text(transcript)
    } else {
        Text("Whisper는 작은 실수를 고쳐서 적을 때가 있어요. 내 목소리를 들으며 실제로 말한 대로 고친 뒤 교정을 받으세요",
            style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text("단어를 누르면 그 부분을 듣고, 꾹 누르면 고칠 수 있어요", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        val mask = remember(state.words) { fillerMask(state.words.map { werWords(it.text).firstOrNull().orEmpty() }) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.words.forEachIndexed { i, word ->
                val color = when {
                    word.state == EditState.INSERTED -> EditBlue
                    mask[i] -> Color.Gray
                    else -> Color.Unspecified
                }
                val decoration = when (word.state) {
                    EditState.REPLACED -> TextDecoration.Underline
                    EditState.DELETED -> TextDecoration.LineThrough
                    else -> null
                }
                Text(word.text, color = if (word.state == EditState.REPLACED) EditBlue else if (word.state == EditState.DELETED) Color.Gray else color,
                    textDecoration = decoration,
                    modifier = Modifier.combinedClickable(onClick = { model.playWord(i) }, onLongClick = { editing = i }).padding(vertical = 2.dp))
            }
        }
        if (state.words.any { it.state != EditState.KEPT }) TextButton(onClick = model::restoreAllWords) { Text("전체 되돌리기") }
    }
    editing?.let { index -> state.words.getOrNull(index)?.let { word ->
        EditWordDialog(word, onDismiss = { editing = null },
            onReplace = { model.replaceWord(index, it); editing = null },
            onDelete = { model.deleteWord(index); editing = null },
            onInsert = { model.insertWord(index, it); editing = null },
            onRestore = { model.restoreWord(index); editing = null })
    } }
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
        OutlinedButton(enabled = model.recordingFile.isFile, onClick = { scope.launch { PcmPlayer.play(model.recordingFile) } }) { Text("내 답변 듣기") }
        Button(onClick = model::retry) { Text("다시 답하기") }
        OutlinedButton(onClick = model::nextQuestion) { Text("다음 질문") }
    }
    HorizontalDivider()
    if (!state.correctionOpen) Button(onClick = model::openCorrection, modifier = Modifier.fillMaxWidth()) { Text("문법 교정 받기 (AI)") }
    else CorrectionSection(model, state)
}

@Composable
private fun CorrectionSection(model: SpeakingViewModel, state: SpeakingState) {
    val engine by model.llm.collectAsState()
    val engineState = engine?.state?.collectAsState()?.value
    if (engine == null || engineState == null) { Text("AI 교정 준비 중…"); return }
    if (engineState !is LlmEngineState.Ready && engineState !is LlmEngineState.Generating) {
        LlmPreparationScreen(engineState, state.hasToken, model.llmModelDownloaded(), model.llmModelBytes,
            onSaveToken = model::saveToken, onDownload = model::prepareLlm, onContinue = model::startCorrection,
            onBack = model::closeCorrection, backLabel = "← 닫기", continueLabel = "교정 시작")
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("문법 교정", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = model::closeCorrection) { Text("닫기") }
    }
    if (state.correctionTotal == 0 && !state.correcting) {
        Button(onClick = model::startCorrection, modifier = Modifier.fillMaxWidth()) { Text("교정 시작 (최대 ${MAX_CORRECTION_SENTENCES}문장)") }
    }
    if (state.correcting) {
        Text("${state.corrections.size} / ${state.correctionTotal} 문장 · 문장당 15초 안팎")
        LinearProgressIndicator(Modifier.fillMaxWidth())
        TextButton(onClick = model::cancelCorrection) { Text("교정 취소") }
    }
    state.corrections.forEachIndexed { index, outcome ->
        CorrectionResultCard(outcome, onRetry = { model.retryCorrection(index) }, retryEnabled = !state.correcting)
    }
}

@Composable
private fun EditWordDialog(word: EditableWord, onDismiss: () -> Unit, onReplace: (String) -> Unit, onDelete: () -> Unit,
                           onInsert: (String) -> Unit, onRestore: () -> Unit) {
    var text by remember(word) { mutableStateOf(word.text) }
    var insert by remember(word) { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("단어 고치기") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            word.original?.let { Text("Whisper: ${it.text}", color = Color.Gray) }
            OutlinedTextField(text, { text = it }, label = { Text("실제로 말한 단어") }, singleLine = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onReplace(text) }) { Text("바꾸기") }
                OutlinedButton(onClick = onDelete) { Text("지우기") }
            }
            OutlinedTextField(insert, { insert = it }, label = { Text("뒤에 넣을 단어") }, singleLine = true)
            OutlinedButton(enabled = insert.isNotBlank(), onClick = { onInsert(insert) }) { Text("뒤에 넣기") }
        }
    }, confirmButton = { TextButton(onClick = onRestore) { Text("원래대로") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
}

private val EditBlue = Color(0xFF1E5BD8)

private fun clock(ms: Long): String { val s = ms / 1000; return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }
