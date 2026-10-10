package com.jooh.opic.feature.speaking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.AnswerTemplates
import com.jooh.opic.core.common.AnswerFeedback
import com.jooh.opic.core.common.buildAnswerFeedbackPrompt
import com.jooh.opic.core.common.parseAnswerFeedback
import com.jooh.opic.core.correction.saveHfToken
import com.jooh.opic.core.common.EditableWord
import com.jooh.opic.core.common.SpeakingCatalog
import com.jooh.opic.core.database.MockSummaryRow
import com.jooh.opic.core.database.SpeakingAnswerEntity
import com.jooh.opic.core.database.SpeakingDao
import com.jooh.opic.core.common.MockItem
import com.jooh.opic.core.common.buildMockExam
import com.jooh.opic.core.common.editable
import com.jooh.opic.core.common.editedText
import com.jooh.opic.core.common.deleteAt
import com.jooh.opic.core.common.insertAfter
import com.jooh.opic.core.common.replaceAt
import com.jooh.opic.core.common.restoreAll
import com.jooh.opic.core.common.restoreAt
import com.jooh.opic.core.common.playRange
import com.jooh.opic.core.common.SpokenWord
import com.jooh.opic.core.common.sentencesToCorrect
import com.jooh.opic.core.correction.CorrectionCoordinator
import com.jooh.opic.core.correction.CorrectionOutcome
import com.jooh.opic.core.llm.HfTokenStore
import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.llm.OnDeviceLlmEngine
import com.jooh.opic.core.stt.PcmPlayer
import com.jooh.opic.core.common.SpeakingMetrics
import com.jooh.opic.core.common.SpeakingQuestion
import com.jooh.opic.core.common.StudyLanguage
import com.jooh.opic.core.common.cleanWhisperText
import com.jooh.opic.core.common.recordingTooShort
import com.jooh.opic.core.common.speakingMetrics
import com.jooh.opic.core.stt.PcmRecorder
import com.jooh.opic.core.stt.SttModels
import com.jooh.opic.core.stt.UserWhisper
import com.jooh.opic.core.stt.WavDecoder
import com.jooh.opic.core.stt.WhisperEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** 머뭇거림을 지우지 않게 하는 Whisper 앞 문맥. */
internal const val FILLER_PROMPT = "Um, uh, so, like, you know, I mean."
internal const val MAX_ANSWER_MS = 120_000L
internal const val MAX_CORRECTION_SENTENCES = 15

enum class SpeakingPage { TOPICS, QUESTION, RESULT, MOCK_TRANSCRIBE, MOCK_SUMMARY, HISTORY, TEMPLATES, TEMPLATE }

/** 모의고사 답변 하나 (TASK 20). 받아 적기는 15문항이 끝난 뒤 한꺼번에. */
data class MockAnswer(
    val item: MockItem, val file: File? = null, val durationMs: Long = 0, val transcript: String? = null,
    val words: List<EditableWord> = emptyList(), val metrics: SpeakingMetrics? = null, val failed: String? = null, val savedId: Long? = null,
)
enum class ModelState { MISSING, DOWNLOADING, NOT_LOADED, LOADING, READY, FAILED }

data class SpeakingState(
    val page: SpeakingPage = SpeakingPage.TOPICS,
    /** 답변 내용·구조 피드백 (Gemma, 답변 전체 1회) */
    val feedbackLoading: Boolean = false,
    val feedback: AnswerFeedback? = null,
    val feedbackFailed: Boolean = false,
    /** 답변 템플릿 상세에서 보는 유형 (TASK 33) */
    val templateType: String? = null,
    val topicId: String? = null,
    val question: SpeakingQuestion? = null,
    val replaysLeft: Int = 1,
    val showText: Boolean = false,
    val model: ModelState = ModelState.MISSING,
    val progress: Int = 0,
    val recording: Boolean = false,
    val elapsedMs: Long = 0,
    val level: Float = 0f,
    val transcribing: Boolean = false,
    val transcript: String? = null,
    val metrics: SpeakingMetrics? = null,
    val durationMs: Long = 0,
    val words: List<EditableWord> = emptyList(),
    val showOriginal: Boolean = false,
    val correctionOpen: Boolean = false,
    val correcting: Boolean = false,
    val correctionTotal: Int = 0,
    val corrections: List<CorrectionOutcome> = emptyList(),
    val hasToken: Boolean = false,
    val audioFile: File? = null,
    val mockMode: Boolean = false,
    val mock: List<MockAnswer> = emptyList(),
    val mockIndex: Int = 0,
    val mockDone: Int = 0,
    val fromMock: Boolean = false,
    val savedId: Long? = null,
    val pastCount: Int = 0,
    val lastPast: SpeakingAnswerEntity? = null,
    val historyAnswers: List<SpeakingAnswerEntity> = emptyList(),
    val historyMocks: List<MockSummaryRow> = emptyList(),
    val message: String? = null,
)

class SpeakingViewModel(
    app: Application,
    private val language: StudyLanguage,
    val catalog: SpeakingCatalog?,
    private val whisper: UserWhisper,
    private val beforeLoad: () -> Unit,
    private val speak: (String) -> Unit,
    private val stopSpeaking: () -> Unit,
    private val llmEngine: () -> OnDeviceLlmEngine,
    private val tokenStore: HfTokenStore,
    val llmModelDownloaded: () -> Boolean,
    val llmModelBytes: Long,
    private val history: SpeakingDao? = null,
    val templates: AnswerTemplates? = null,
) : AndroidViewModel(app) {
    /** 생성자 인자가 많아 앱이 만드는 방법만 넘긴다 (TASK 38). */
    class Factory(private val create: () -> SpeakingViewModel) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T = create() as T
    }

    private val mutableLlm = MutableStateFlow<OnDeviceLlmEngine?>(null)
    /** 교정을 열었을 때만 만든다 (Whisper와 동시 적재 금지). */
    val llm: StateFlow<OnDeviceLlmEngine?> = mutableLlm.asStateFlow()
    private var correctionJob: Job? = null
    private var preparation: Job? = null
    private val mutable = MutableStateFlow(SpeakingState())
    val state = mutable.asStateFlow()
    private val defaultFile = File(app.filesDir, "speaking-last.pcm")
    /** 지금 결과 화면의 녹음 (모의고사 답변이면 mock-NN.pcm). */
    val recordingFile: File get() = state.value.audioFile ?: defaultFile
    private fun mockFile(index: Int) = File(getApplication<Application>().filesDir, "mock-%02d.pcm".format(index + 1))
    private val abort = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private var transcribeJob: Job? = null

    init { refreshModel(); mutable.update { it.copy(hasToken = tokenStore.getToken() != null) } }

    private fun refreshModel() = mutable.update {
        it.copy(model = when { whisper.ready -> ModelState.READY; SttModels.isDownloaded(whisper.model) -> ModelState.NOT_LOADED; else -> ModelState.MISSING })
    }

    private fun allQuestions() = catalog?.topics.orEmpty().flatMap { t -> t.questions.map { t.id to it } }

    fun openTopic(topicId: String) {
        val first = catalog?.topics?.firstOrNull { it.id == topicId }?.questions?.firstOrNull() ?: return
        ask(topicId, first)
    }

    // ---- 답변 템플릿 (TASK 33) ----
    fun openTemplates() { cancelWork(); stopSpeaking(); mutable.update { SpeakingState(page = SpeakingPage.TEMPLATES, model = it.model, hasToken = it.hasToken) } }
    fun openTemplate(type: String) = mutable.update { it.copy(page = SpeakingPage.TEMPLATE, templateType = type) }

    /** 그 유형 질문 중 하나로 녹음 화면을 연다. 질문이 없으면 아무것도 하지 않는다. */
    fun practiceType(type: String) {
        val (topicId, question) = allQuestions().filter { it.second.type == type }.randomOrNull() ?: return
        ask(topicId, question)
    }

    fun randomQuestion() {
        val (topicId, question) = allQuestions().randomOrNull() ?: return
        ask(topicId, question)
    }

    fun nextQuestion() {
        val topic = catalog?.topics?.firstOrNull { it.id == state.value.topicId } ?: return
        val index = topic.questions.indexOfFirst { it.id == state.value.question?.id }
        ask(topic.id, topic.questions[(index + 1) % topic.questions.size])
    }

    fun retry() { state.value.question?.let { ask(state.value.topicId!!, it) } }

    private fun ask(topicId: String, question: SpeakingQuestion) {
        cancelWork()
        mutable.update { SpeakingState(page = SpeakingPage.QUESTION, topicId = topicId, question = question, model = it.model, progress = it.progress, hasToken = it.hasToken) }
        speak(question.en)
        loadPast(question.id, exclude = null)
    }

    // ---- 기록 (TASK 22) ----
    private fun loadPast(questionId: String, exclude: Long?) {
        val dao = history ?: return
        viewModelScope.launch {
            val past = runCatching { dao.byQuestion(language.code, questionId) }.getOrDefault(emptyList()).filter { it.id != exclude }
            mutable.update { if (it.question?.id != questionId) it else it.copy(pastCount = past.size, lastPast = past.firstOrNull()) }
        }
    }

    private suspend fun save(questionId: String, topicId: String, durationMs: Long, transcript: String, words: List<EditableWord>, metrics: SpeakingMetrics, mockId: Long?): Long? =
        history?.let { dao -> runCatching { dao.insert(SpeakingAnswerEntity(language = language.code, questionId = questionId, topicId = topicId,
            createdAt = System.currentTimeMillis(), durationMs = durationMs, transcript = transcript, editedText = editedText(words),
            wordCount = metrics.wordCount, wordsPerMinute = metrics.wordsPerMinute, fillerCount = metrics.fillerCount,
            sentenceCount = metrics.sentenceCount, mockId = mockId)) }.getOrNull() }

    fun openHistory() {
        cancelWork(); stopSpeaking()
        mutable.update { SpeakingState(page = SpeakingPage.HISTORY, model = it.model, hasToken = it.hasToken) }
        val dao = history ?: return
        viewModelScope.launch {
            val answers = runCatching { dao.recent(language.code) }.getOrDefault(emptyList())
            val mocks = runCatching { dao.mockSummaries(language.code) }.getOrDefault(emptyList())
            mutable.update { it.copy(historyAnswers = answers, historyMocks = mocks) }
        }
    }

    fun replay() {
        val s = state.value
        if (s.replaysLeft <= 0 || s.recording || s.question == null) return
        mutable.update { it.copy(replaysLeft = it.replaysLeft - 1) }
        speak(s.question.en)
    }

    /** 발음 힌트의 원어민 발음 (TTS). */
    fun say(text: String) = speak(text)

    fun toggleText() = mutable.update { it.copy(showText = !it.showText) }

    fun backToTopics() { cancelWork(); stopSpeaking(); mutable.update { SpeakingState(model = it.model, hasToken = it.hasToken) } }

    fun prepareModel() {
        val s = state.value
        if (s.model in setOf(ModelState.DOWNLOADING, ModelState.LOADING, ModelState.READY) || s.recording) return
        mutable.update { it.copy(model = if (SttModels.isDownloaded(whisper.model)) ModelState.LOADING else ModelState.DOWNLOADING, message = null) }
        viewModelScope.launch {
            try {
                whisper.prepare(true, { beforeLoad(); mutable.update { it.copy(model = ModelState.LOADING) } }) { done, total ->
                    mutable.update { it.copy(progress = (done * 100 / total).toInt()) }
                }
                mutable.update { it.copy(model = ModelState.READY) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutable.update { it.copy(model = ModelState.FAILED, message = e.message) } }
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (recordingJob?.isActive == true || state.value.transcribing) return
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            mutable.update { it.copy(message = "마이크 권한이 필요합니다") }; return
        }
        if (!whisper.ready) { refreshModel(); mutable.update { it.copy(message = "음성 인식 모델을 다시 불러와 주세요") }; return }
        stopSpeaking()
        mutable.update { it.copy(recording = true, elapsedMs = 0, level = 0f, transcript = null, metrics = null, message = null) }
        recordingJob = viewModelScope.launch {
            val started = System.currentTimeMillis()
            val ticker = launch { while (true) { mutable.update { it.copy(elapsedMs = System.currentTimeMillis() - started) }; delay(200) } }
            try {
                val target = if (state.value.mockMode) mockFile(state.value.mockIndex) else defaultFile
                val bytes = PcmRecorder.record(target, MAX_ANSWER_MS, { state.value.recording }) { level -> mutable.update { it.copy(level = level) } }
                ticker.cancel()
                mutable.update { it.copy(recording = false, level = 0f) }
                val durationMs = bytes * 1000 / (WhisperEngine.SAMPLE_RATE * 2)
                if (recordingTooShort(bytes)) mutable.update { it.copy(message = "너무 짧아요 — 질문에 답해 보세요") }
                else if (state.value.mockMode) {
                    mutable.update { s -> s.copy(mock = s.mock.toMutableList().also { it[s.mockIndex] = it[s.mockIndex].copy(file = target, durationMs = durationMs) }) }
                    mockAdvance()
                } else transcribe(durationMs)
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutable.update { it.copy(message = "녹음 실패: ${e.message}") }
            } finally { ticker.cancel(); mutable.update { it.copy(recording = false, level = 0f) } }
        }
    }

    fun stopRecording() = mutable.update { it.copy(recording = false) }

    private data class Answer(val text: String, val words: List<EditableWord>, val metrics: SpeakingMetrics)

    /** 녹음 파일 하나를 받아 적는다. 말소리가 없으면 null. */
    private suspend fun transcribeFile(file: File, durationMs: Long): Answer? {
        val audio = withContext(Dispatchers.IO) { WavDecoder.pcm16ToFloat(file.readBytes()) }
        val result = whisper.transcribe(audio, abort, FILLER_PROMPT, withWords = true)
        val text = cleanWhisperText(result.text)
        if (text.isBlank()) return null
        val spoken = result.words.filter { cleanWhisperText(it.text).isNotEmpty() }
            .ifEmpty { text.split(Regex("\\s+")).filter { it.isNotEmpty() }.map { SpokenWord(it, 0, durationMs, 1f) } }
        return Answer(text, editable(spoken), speakingMetrics(text, durationMs))
    }

    private fun transcribe(durationMs: Long) {
        abort.set(false)
        mutable.update { it.copy(transcribing = true) }
        transcribeJob = viewModelScope.launch {
            try {
                val answer = transcribeFile(defaultFile, durationMs)
                if (answer == null) mutable.update { it.copy(message = "말소리가 잘 들리지 않았어요 — 폰을 입에 가까이 대 주세요") }
                else {
                    val q = state.value.question
                    val id = q?.let { save(it.id, state.value.topicId.orEmpty(), durationMs, answer.text, answer.words, answer.metrics, null) }
                    mutable.update { it.copy(page = SpeakingPage.RESULT, transcript = answer.text, durationMs = durationMs,
                        words = answer.words, metrics = answer.metrics, audioFile = null, savedId = id) }
                    q?.let { loadPast(it.id, exclude = id) }
                }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutable.update { it.copy(message = "받아 적기 실패: ${e.message}") }
            } finally { mutable.update { it.copy(transcribing = false) } }
        }
    }

    // ---- 모의고사 (TASK 20) ----
    fun startMock() {
        val exam = catalog?.let { buildMockExam(it, kotlin.random.Random.Default) }
        if (exam == null) { mutable.update { it.copy(message = "모의고사를 만들 문항이 부족합니다") }; return }
        cancelWork()
        exam.indices.forEach { mockFile(it).delete() }
        val first = exam.first()
        mutable.update { SpeakingState(page = SpeakingPage.QUESTION, topicId = first.topicId, question = first.question, model = it.model,
            hasToken = it.hasToken, mockMode = true, mock = exam.map { item -> MockAnswer(item) }) }
        speak(first.question.en)
    }

    /** 다음 문항 (마지막이면 받아 적기로). 건너뛰기도 같다. */
    fun mockAdvance() {
        val s = state.value
        if (!s.mockMode || s.recording) return
        val next = s.mockIndex + 1
        if (next >= s.mock.size) { finishMock(); return }
        val item = s.mock[next].item
        mutable.update { it.copy(mockIndex = next, topicId = item.topicId, question = item.question, replaysLeft = 1, showText = false, message = null) }
        speak(item.question.en)
    }

    fun finishMock() {
        if (state.value.recording) return
        stopSpeaking()
        abort.set(false)
        mutable.update { it.copy(page = SpeakingPage.MOCK_TRANSCRIBE, mockDone = 0, transcribing = true) }
        transcribeJob = viewModelScope.launch {
            try {
                if (!whisper.ready) runCatching { prepareModelNow() }
                val mockId = System.currentTimeMillis()
                for ((i, answer) in state.value.mock.withIndex()) {
                    val file = answer.file
                    val updated = if (file == null) answer else try {
                        transcribeFile(file, answer.durationMs)?.let {
                            val id = save(answer.item.question.id, answer.item.topicId, answer.durationMs, it.text, it.words, it.metrics, mockId)
                            answer.copy(transcript = it.text, words = it.words, metrics = it.metrics, savedId = id)
                        }
                            ?: answer.copy(failed = "말소리가 들리지 않음")
                    } catch (e: CancellationException) { throw e } catch (e: Exception) { answer.copy(failed = "받아 적기 실패") }
                    mutable.update { s -> s.copy(mockDone = i + 1, mock = s.mock.toMutableList().also { it[i] = updated }) }
                }
            } finally { mutable.update { it.copy(transcribing = false, page = SpeakingPage.MOCK_SUMMARY) } }
        }
    }

    private suspend fun prepareModelNow() {
        whisper.prepare(false, { beforeLoad() }) { _, _ -> }
        refreshModel()
    }

    fun openMockAnswer(index: Int) {
        val answer = state.value.mock.getOrNull(index) ?: return
        val metrics = answer.metrics ?: return
        mutable.update { it.copy(page = SpeakingPage.RESULT, fromMock = true, mockIndex = index, topicId = answer.item.topicId,
            question = answer.item.question, transcript = answer.transcript, words = answer.words, metrics = metrics,
            durationMs = answer.durationMs, audioFile = answer.file, savedId = answer.savedId, lastPast = null, pastCount = 0, correctionOpen = false, corrections = emptyList(), correctionTotal = 0, showOriginal = false) }
    }

    /** 결과 화면에서 고친 글을 모의고사 목록에 되돌려 놓고 요약으로. */
    fun backToMockSummary() {
        cancelCorrection()
        mutable.update { s -> s.copy(page = SpeakingPage.MOCK_SUMMARY, fromMock = false, correctionOpen = false,
            mock = s.mock.toMutableList().also { it[s.mockIndex] = it[s.mockIndex].copy(words = s.words, metrics = s.metrics) }) }
    }

    // ---- 받아 적은 글 직접 고치기 (TASK 18) ----
    private fun edit(block: (List<EditableWord>) -> List<EditableWord>) {
        mutable.update {
            val words = block(it.words)
            it.copy(words = words, metrics = speakingMetrics(editedText(words), it.durationMs))
        }
        val s = state.value
        val id = s.savedId ?: return
        val m = s.metrics ?: return
        viewModelScope.launch { runCatching { history?.updateEdit(language.code, id, editedText(s.words), m.wordCount, m.wordsPerMinute, m.fillerCount, m.sentenceCount) } }
    }
    fun replaceWord(index: Int, text: String) = edit { it.replaceAt(index, text) }
    fun deleteWord(index: Int) = edit { it.deleteAt(index) }
    fun insertWord(index: Int, text: String) = edit { it.insertAfter(index, text) }
    fun restoreWord(index: Int) = edit { it.restoreAt(index) }
    fun restoreAllWords() = edit { it.restoreAll() }
    fun toggleOriginal() = mutable.update { it.copy(showOriginal = !it.showOriginal) }
    fun playWord(index: Int) {
        val range = playRange(state.value.words, index) ?: return
        viewModelScope.launch { PcmPlayer.play(recordingFile, range.first, range.last) }
    }

    // ---- Gemma 문법 교정 (TASK 18) ----
    fun openCorrection() {
        if (state.value.recording || state.value.transcribing) return
        mutable.update { it.copy(correctionOpen = true, corrections = emptyList(), correctionTotal = 0) }
        viewModelScope.launch {
            whisper.close() // Whisper를 내리고 Gemma를 올린다
            refreshModel()
            val engine = llmEngine().also { mutableLlm.value = it }
            if (llmModelDownloaded() && engine.state.value is LlmEngineState.NotDownloaded) prepareLlm()
        }
    }
    fun closeCorrection() { cancelCorrection(); mutable.update { it.copy(correctionOpen = false) } }
    fun saveToken(token: String): Boolean {
        if (!saveHfToken(tokenStore, token)) return false
        mutable.update { it.copy(hasToken = true) }
        return true
    }
    fun prepareLlm() {
        val engine = mutableLlm.value ?: return
        if (preparation?.isActive == true) return
        preparation = viewModelScope.launch(Dispatchers.IO) { engine.ensureModelReady() }
    }
    fun startCorrection() {
        val engine = mutableLlm.value ?: return
        val old = state.value
        val sentences = sentencesToCorrect(editedText(old.words), MAX_CORRECTION_SENTENCES)
        if (old.correcting || sentences.isEmpty() || engine.state.value !is LlmEngineState.Ready) return
        mutable.update { it.copy(correcting = true, correctionTotal = sentences.size, corrections = emptyList()) }
        correctionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                CorrectionCoordinator(engine).correct(sentences, MAX_CORRECTION_SENTENCES) { result ->
                    currentCoroutineContext().ensureActive()
                    mutable.update { it.copy(corrections = it.corrections + result) }
                }
            } finally { mutable.update { it.copy(correcting = false) } }
        }
    }
    fun retryCorrection(index: Int) {
        val engine = mutableLlm.value ?: return
        val failed = state.value.corrections.getOrNull(index) as? CorrectionOutcome.Failed ?: return
        if (state.value.correcting) return
        mutable.update { it.copy(correcting = true) }
        correctionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val replacement = CorrectionCoordinator(engine).correctOne(failed.sentence)
                mutable.update { s -> if (s.corrections.getOrNull(index) != failed) s
                    else s.copy(corrections = s.corrections.toMutableList().also { it[index] = replacement }) }
            } finally { mutable.update { it.copy(correcting = false) } }
        }
    }
    /** 답변 전체를 유형별 템플릿 단계 기준으로 평가한다. 문법 교정과 같은 엔진·준비 상태를 쓴다. */
    fun startFeedback() {
        val engine = mutableLlm.value ?: return
        val old = state.value
        val question = old.question ?: return
        val answer = editedText(old.words)
        if (old.correcting || old.feedbackLoading || answer.isBlank() || engine.state.value !is LlmEngineState.Ready) return
        val steps = templates?.templates?.firstOrNull { it.type == question.type }?.steps.orEmpty().map { it.name }
        mutable.update { it.copy(feedbackLoading = true, feedback = null, feedbackFailed = false) }
        correctionJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val raw = engine.generate(buildAnswerFeedbackPrompt(question.en, steps, answer), timeoutMs = 180_000).getOrNull()
                val parsed = raw?.let { parseAnswerFeedback(it, steps) }
                mutable.update { it.copy(feedback = parsed, feedbackFailed = parsed == null) }
            } finally { mutable.update { it.copy(feedbackLoading = false) } }
        }
    }

    fun cancelCorrection() { correctionJob?.cancel(); mutable.update { it.copy(correcting = false, feedbackLoading = false) } }

    fun cancelTranscribe() { abort.set(true); transcribeJob?.cancel(); mutable.update { it.copy(transcribing = false, message = "취소됨") } }

    private fun cancelWork() {
        stopRecording(); recordingJob?.cancel(); if (state.value.transcribing) cancelTranscribe(); cancelCorrection()
    }

    override fun onCleared() { abort.set(true); stopRecording(); recordingJob?.cancel(); correctionJob?.cancel(); stopSpeaking(); super.onCleared() }
}
