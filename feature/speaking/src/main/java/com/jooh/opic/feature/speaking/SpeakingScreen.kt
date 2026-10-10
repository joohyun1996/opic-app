package com.jooh.opic.feature.speaking

import com.jooh.opic.core.correction.LocalGrammarLink

import com.jooh.opic.core.ui.Opic
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
import com.jooh.opic.core.common.editedText
import com.jooh.opic.core.common.flapWords
import com.jooh.opic.core.common.linkingPairs
import com.jooh.opic.core.common.pronunciationTips
import com.jooh.opic.core.common.reductions
import com.jooh.opic.core.common.unclearWords
import com.jooh.opic.core.ui.PronunciationHintsCard
import com.jooh.opic.core.ui.UnclearHint
import com.jooh.opic.core.correction.CorrectionResultCard
import com.jooh.opic.core.database.SpeakingAnswerEntity
import com.jooh.opic.core.correction.LlmPreparationScreen
import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.common.fillerMask
import com.jooh.opic.core.common.paceAdvice
import com.jooh.opic.core.common.werWords
import com.jooh.opic.core.stt.PcmPlayer
import kotlinx.coroutines.launch

private val QUESTION_TYPE_KO = mapOf(
    "describe" to "묘사", "routine" to "습관·루틴", "experience" to "경험", "compare" to "비교",
    "roleplay_ask" to "롤플레이 · 질문하기", "roleplay_solve" to "롤플레이 · 문제 해결", "issue" to "사회 이슈·의견",
)

@Composable
fun SpeakingScreen(model: SpeakingViewModel, onBack: () -> Unit) {
    val state by model.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (model.catalog == null) {
            Text("스피킹 데이터를 불러오지 못했습니다", color = MaterialTheme.colorScheme.error)
            return@Column
        }
        when (state.page) {
            SpeakingPage.TOPICS -> TopicsPage(model, onBack)
            SpeakingPage.QUESTION -> QuestionPage(model, state)
            SpeakingPage.RESULT -> ResultPage(model, state)
            SpeakingPage.MOCK_TRANSCRIBE -> MockTranscribePage(model, state)
            SpeakingPage.MOCK_SUMMARY -> MockSummaryPage(model, state)
            SpeakingPage.HISTORY -> HistoryPage(model, state)
            SpeakingPage.TEMPLATES -> TemplatesPage(model)
            SpeakingPage.TEMPLATE -> TemplatePage(model, state)
        }
    }
}

private val CATEGORY_KO = listOf("intro" to "자기소개", "survey" to "설문 주제", "unexpected" to "돌발 주제", "roleplay" to "롤플레이")
private val LEVEL_ORDER = listOf("IM", "IH", "AL")

@Composable
private fun TopicsPage(model: SpeakingViewModel, onBack: () -> Unit) {
    val catalog = model.catalog!!
    // 탭 첫 화면: 제목이 오른쪽 위 ≡와 같은 줄 (TASK 25 — 화면 안쪽 여백만 사용)
    Text("스피킹", style = MaterialTheme.typography.headlineSmall)
    Text("질문을 듣고 바로 영어로 답해 보세요. 답변은 최대 2분입니다. ${catalog.topics.size}주제 ${catalog.topics.sumOf { it.questions.size }}문항")
    Button(onClick = model::startMock, modifier = Modifier.fillMaxWidth()) { Text("모의고사 (실제 시험 순서 15문항)") }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = model::randomQuestion, modifier = Modifier.weight(1f)) { Text("무작위 질문") }
        OutlinedButton(onClick = model::openHistory, modifier = Modifier.weight(1f)) { Text("내 기록") }
    }
    if (model.templates != null) OutlinedButton(onClick = model::openTemplates, modifier = Modifier.fillMaxWidth()) { Text("유형별 답변 템플릿") }
    CATEGORY_KO.forEach { (category, title) ->
        val topics = catalog.topics.filter { it.category == category }
        if (topics.isEmpty()) return@forEach
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        topics.forEach { topic ->
            val levels = topic.questions.map { it.level }.toSet().sortedBy(LEVEL_ORDER::indexOf)
            Card(onClick = { model.openTopic(topic.id) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(topic.titleKo, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text("${topic.questions.size}문항 · ${levels.first()}" + if (levels.size > 1) "~${levels.last()}" else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MockTranscribePage(model: SpeakingViewModel, state: SpeakingState) {
    val answered = state.mock.count { it.file != null }
    Text("모의고사 받아 적는 중", style = MaterialTheme.typography.headlineSmall)
    Text("${state.mockDone} / ${state.mock.size} 문항 (답한 문항 ${answered}개, 2분 답변 하나에 30~40초)")
    LinearProgressIndicator(progress = { if (state.mock.isEmpty()) 0f else state.mockDone.toFloat() / state.mock.size }, modifier = Modifier.fillMaxWidth())
    TextButton(onClick = model::cancelTranscribe) { Text("멈추고 지금까지 결과 보기") }
}

@Composable
private fun MockSummaryPage(model: SpeakingViewModel, state: SpeakingState) {
    TextButton(onClick = model::backToTopics) { Text("← 주제") }
    Text("모의고사 결과", style = MaterialTheme.typography.headlineSmall)
    val done = state.mock.mapNotNull { it.metrics }
    if (done.isNotEmpty()) Text("답한 문항 ${done.size}개 · 평균 ${done.sumOf { it.durationMs } / done.size / 1000}초 · 분당 ${done.sumOf { it.wordsPerMinute } / done.size}단어 · 머뭇거림 합계 ${done.sumOf { it.fillerCount }}번")
    Text("문항을 누르면 답변 고치기·문법 교정·발음 힌트를 볼 수 있어요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    state.mock.forEachIndexed { index, answer ->
        Card(onClick = { model.openMockAnswer(index) }, enabled = answer.metrics != null, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${answer.item.number}. ${answer.item.part} · ${answer.item.question.level}", style = MaterialTheme.typography.titleSmall)
                Text(answer.item.question.en, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                val m = answer.metrics
                Text(when {
                    m != null -> "${clock(m.durationMs)} · 분당 ${m.wordsPerMinute}단어 · 머뭇거림 ${m.fillerCount}번"
                    answer.failed != null -> answer.failed
                    answer.file == null -> "답하지 않음"
                    else -> "받아 적지 않음"
                }, color = if (m != null) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Button(onClick = model::startMock, modifier = Modifier.fillMaxWidth()) { Text("새 모의고사") }
}

@Composable
private fun QuestionPage(model: SpeakingViewModel, state: SpeakingState) {
    val question = state.question ?: return
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) model.startRecording() }
    val topic = model.catalog?.topics?.firstOrNull { it.id == state.topicId }
    if (state.mockMode) {
        val item = state.mock.getOrNull(state.mockIndex)?.item
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("모의고사 ${state.mockIndex + 1} / ${state.mock.size}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            TextButton(enabled = !state.recording, onClick = model::finishMock) { Text("끝내기") }
        }
        LinearProgressIndicator(progress = { (state.mockIndex + 1f) / state.mock.size.coerceAtLeast(1) }, modifier = Modifier.fillMaxWidth())
        Text("${item?.part.orEmpty()} · ${QUESTION_TYPE_KO[question.type].orEmpty()} · ${question.level}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        TextButton(onClick = model::backToTopics) { Text("← 주제") }
        Text("${topic?.titleKo.orEmpty()} · ${QUESTION_TYPE_KO[question.type].orEmpty()} · ${question.level}", style = MaterialTheme.typography.titleMedium)
    }
    state.lastPast?.let { last ->
        Text("지난 답변 ${state.pastCount}개 · 마지막 ${dateTime(last.createdAt)} · ${clock(last.durationMs)} · 분당 ${last.wordsPerMinute}단어 · 머뭇거림 ${last.fillerCount}번",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = state.replaysLeft > 0 && !state.recording, onClick = model::replay) {
            Text(if (state.replaysLeft > 0) "다시 듣기 (1회)" else "다시 듣기 끝")
        }
        OutlinedButton(onClick = model::toggleText) { Text(if (state.showText) "질문 글 숨기기" else "질문 글 보기") }
    }
    if (state.showText) Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(question.en, style = MaterialTheme.typography.bodyLarge)
            Text(question.ko, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            if (state.mockMode) OutlinedButton(onClick = model::mockAdvance, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.mockIndex + 1 < state.mock.size) "건너뛰기" else "건너뛰고 끝내기")
            }
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
    if (state.fromMock) TextButton(onClick = model::backToMockSummary) { Text("← 모의고사 결과") }
    else TextButton(onClick = model::backToTopics) { Text("← 주제") }
    state.question?.let { Text(it.en, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (state.showOriginal) "Whisper 받아 적기" else "내 답변", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = model::toggleOriginal) { Text(if (state.showOriginal) "내가 고친 글 보기" else "Whisper 원문 보기") }
    }
    if (state.showOriginal) {
        Text(transcript)
    } else {
        Text("Whisper는 작은 실수를 고쳐서 적을 때가 있어요. 내 목소리를 들으며 실제로 말한 대로 고친 뒤 교정을 받으세요",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("단어를 누르면 그 부분을 듣고, 꾹 누르면 고칠 수 있어요", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val mask = remember(state.words) { fillerMask(state.words.map { werWords(it.text).firstOrNull().orEmpty() }) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            state.words.forEachIndexed { i, word ->
                val color = when {
                    word.state == EditState.INSERTED -> EditBlue
                    mask[i] -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> Color.Unspecified
                }
                val decoration = when (word.state) {
                    EditState.REPLACED -> TextDecoration.Underline
                    EditState.DELETED -> TextDecoration.LineThrough
                    else -> null
                }
                Text(word.text, color = if (word.state == EditState.REPLACED) EditBlue else if (word.state == EditState.DELETED) MaterialTheme.colorScheme.onSurfaceVariant else color,
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
    state.lastPast?.let { last ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("지난번(${dateTime(last.createdAt)})과 비교", style = MaterialTheme.typography.titleSmall)
                Text("말한 시간 ${clock(last.durationMs)} → ${clock(metrics.durationMs)}")
                Text("분당 단어 ${last.wordsPerMinute} → ${metrics.wordsPerMinute} (${signed(metrics.wordsPerMinute - last.wordsPerMinute)})")
                Text("머뭇거림 ${last.fillerCount} → ${metrics.fillerCount}번 (${signed(metrics.fillerCount - last.fillerCount)})")
                Text("지난 답변: ${last.editedText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 4)
            }
        }
    }
    val edited = remember(state.words) { editedText(state.words) }
    val unclear = remember(state.words) {
        val kept = state.words.withIndex().filter { it.value.original != null && it.value.state == EditState.KEPT }
        unclearWords(kept.map { it.value.original!! }).map { kept[it] }.map { (index, word) ->
            UnclearHint(word.text, werWords(word.text).firstOrNull() ?: word.text, playMine = { model.playWord(index) })
        }
    }
    PronunciationHintsCard(unclear, "불명확하게 들린 단어", pronunciationTips(werWords(edited)),
        linkingPairs(edited), reductions(edited), flapWords(edited), model::say)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(enabled = model.recordingFile.isFile, onClick = { scope.launch { PcmPlayer.play(model.recordingFile) } }) { Text("내 답변 듣기") }
        if (!state.fromMock) {
            Button(onClick = model::retry) { Text("다시 답하기") }
            OutlinedButton(onClick = model::nextQuestion) { Text("다음 질문") }
        }
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
            word.original?.let { Text("Whisper: ${it.text}", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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

private val EditBlue: Color @Composable get() = Opic.colors.accent

@Composable
private fun HistoryPage(model: SpeakingViewModel, state: SpeakingState) {
    var open by remember { mutableStateOf<SpeakingAnswerEntity?>(null) }
    val questions = remember(model.catalog) { model.catalog?.topics.orEmpty().flatMap { it.questions }.associateBy { it.id } }
    TextButton(onClick = model::backToTopics) { Text("← 주제") }
    Text("내 기록", style = MaterialTheme.typography.headlineSmall)
    if (state.historyMocks.isNotEmpty()) {
        Text("모의고사", style = MaterialTheme.typography.titleMedium)
        state.historyMocks.forEach { m ->
            Text("${dateTime(m.createdAt)} · ${m.answered}문항 답함 · 평균 분당 ${m.avgWpm.toInt()}단어 · 머뭇거림 ${m.totalFillers}번")
        }
        HorizontalDivider()
    }
    Text("최근 답변 ${state.historyAnswers.size}개", style = MaterialTheme.typography.titleMedium)
    if (state.historyAnswers.isEmpty()) Text("아직 저장된 답변이 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
    state.historyAnswers.forEach { a ->
        Card(onClick = { open = a }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(questions[a.questionId]?.en ?: a.questionId, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text("${dateTime(a.createdAt)} · ${clock(a.durationMs)} · 분당 ${a.wordsPerMinute}단어 · 머뭇거림 ${a.fillerCount}번" +
                    if (a.mockId != null) " · 모의고사" else "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    open?.let { a ->
        AlertDialog(onDismissRequest = { open = null }, title = { Text(dateTime(a.createdAt)) }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(questions[a.questionId]?.en.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(a.editedText)
                if (a.editedText != a.transcript) Text("Whisper: ${a.transcript}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }, confirmButton = { TextButton(onClick = { open = null }) { Text("닫기") } })
    }
}

private fun signed(n: Int) = if (n > 0) "+$n" else "$n"
private val DATE_FORMAT = java.time.format.DateTimeFormatter.ofPattern("M/d HH:mm")
private fun dateTime(epochMs: Long): String =
    java.time.Instant.ofEpochMilli(epochMs).atZone(java.time.ZoneId.systemDefault()).format(DATE_FORMAT)

private fun clock(ms: Long): String { val s = ms / 1000; return "${s / 60}:${(s % 60).toString().padStart(2, '0')}" }

@Composable
private fun TemplatesPage(model: SpeakingViewModel) {
    TextButton(onClick = model::backToTopics) { Text("← 주제") }
    Text("유형별 답변 템플릿", style = MaterialTheme.typography.headlineSmall)
    Text("OPIc 질문은 유형마다 답하는 순서가 비슷해요. 뼈대를 익히고 바로 연습해 보세요.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    model.templates?.templates.orEmpty().forEach { t ->
        Card(onClick = { model.openTemplate(t.type) }, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(t.titleKo, style = MaterialTheme.typography.titleMedium)
                Text(t.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TemplatePage(model: SpeakingViewModel, state: SpeakingState) {
    val t = model.templates?.templates?.firstOrNull { it.type == state.templateType } ?: return
    var showKo by remember { mutableStateOf(false) }
    TextButton(onClick = model::openTemplates) { Text("← 템플릿 목록") }
    Text(t.titleKo, style = MaterialTheme.typography.headlineSmall)
    Text(t.summary)
    Button(onClick = { model.practiceType(t.type) }, modifier = Modifier.fillMaxWidth()) { Text("이 유형 문제 연습") }
    t.steps.forEachIndexed { i, step ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${i + 1}. ${step.name}", style = MaterialTheme.typography.titleMedium)
                Text(step.tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                step.expressions.forEach { Text("• $it") }
            }
        }
    }
    Text("예시 답변", style = MaterialTheme.typography.titleMedium)
    Text(t.sample)
    TextButton(onClick = { model.say(t.sample) }) { Text("예시 답변 듣기") }
    TextButton(onClick = { showKo = !showKo }) { Text(if (showKo) "한국어 뜻 접기" else "한국어 뜻 보기") }
    if (showKo) Text(t.sampleKo, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LocalGrammarLink.current?.let { link ->
        val titles = t.chapters.mapNotNull { id -> link.title(id)?.let { id to it } }
        if (titles.isNotEmpty()) Text("관련 문법", style = MaterialTheme.typography.titleMedium)
        titles.forEach { (id, title) -> OutlinedButton(onClick = { link.open(id) }, modifier = Modifier.fillMaxWidth()) { Text(title) } }
    }
}
