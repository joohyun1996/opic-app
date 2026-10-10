package com.jooh.opic.feature.speaking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.EditableWord
import com.jooh.opic.core.common.SpeakingCatalog
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

enum class SpeakingPage { TOPICS, QUESTION, RESULT }
enum class ModelState { MISSING, DOWNLOADING, NOT_LOADED, LOADING, READY, FAILED }

data class SpeakingState(
    val page: SpeakingPage = SpeakingPage.TOPICS,
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
    val message: String? = null,
)

class SpeakingViewModel(
    app: Application,
    val catalog: SpeakingCatalog?,
    private val whisper: UserWhisper,
    private val beforeLoad: () -> Unit,
    private val speak: (String) -> Unit,
    private val stopSpeaking: () -> Unit,
    private val llmEngine: () -> OnDeviceLlmEngine,
    private val tokenStore: HfTokenStore,
    val llmModelDownloaded: () -> Boolean,
    val llmModelBytes: Long,
) : AndroidViewModel(app) {
    private val mutableLlm = MutableStateFlow<OnDeviceLlmEngine?>(null)
    /** 교정을 열었을 때만 만든다 (Whisper와 동시 적재 금지). */
    val llm: StateFlow<OnDeviceLlmEngine?> = mutableLlm.asStateFlow()
    private var correctionJob: Job? = null
    private var preparation: Job? = null
    private val mutable = MutableStateFlow(SpeakingState())
    val state = mutable.asStateFlow()
    val recordingFile = File(app.filesDir, "speaking-last.pcm")
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
                val bytes = PcmRecorder.record(recordingFile, MAX_ANSWER_MS, { state.value.recording }) { level -> mutable.update { it.copy(level = level) } }
                ticker.cancel()
                mutable.update { it.copy(recording = false, level = 0f) }
                if (recordingTooShort(bytes)) mutable.update { it.copy(message = "너무 짧아요 — 질문에 답해 보세요") }
                else transcribe(bytes * 1000 / (WhisperEngine.SAMPLE_RATE * 2))
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutable.update { it.copy(message = "녹음 실패: ${e.message}") }
            } finally { ticker.cancel(); mutable.update { it.copy(recording = false, level = 0f) } }
        }
    }

    fun stopRecording() = mutable.update { it.copy(recording = false) }

    private fun transcribe(durationMs: Long) {
        abort.set(false)
        mutable.update { it.copy(transcribing = true) }
        transcribeJob = viewModelScope.launch {
            try {
                val audio = withContext(Dispatchers.IO) { WavDecoder.pcm16ToFloat(recordingFile.readBytes()) }
                val result = whisper.transcribe(audio, abort, FILLER_PROMPT, withWords = true)
                val text = cleanWhisperText(result.text)
                val spoken = result.words.filter { cleanWhisperText(it.text).isNotEmpty() }
                    .ifEmpty { text.split(Regex("\\s+")).filter { it.isNotEmpty() }.map { SpokenWord(it, 0, durationMs, 1f) } }
                if (text.isBlank()) mutable.update { it.copy(message = "말소리가 잘 들리지 않았어요 — 폰을 입에 가까이 대 주세요") }
                else mutable.update { it.copy(page = SpeakingPage.RESULT, transcript = text, durationMs = durationMs,
                    words = editable(spoken), metrics = speakingMetrics(text, durationMs)) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutable.update { it.copy(message = "받아 적기 실패: ${e.message}") }
            } finally { mutable.update { it.copy(transcribing = false) } }
        }
    }

    // ---- 받아 적은 글 직접 고치기 (TASK 18) ----
    private fun edit(block: (List<EditableWord>) -> List<EditableWord>) = mutable.update {
        val words = block(it.words)
        it.copy(words = words, metrics = speakingMetrics(editedText(words), it.durationMs))
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
    fun saveToken(token: String) { tokenStore.setToken(token); mutable.update { it.copy(hasToken = true) } }
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
    fun cancelCorrection() { correctionJob?.cancel(); mutable.update { it.copy(correcting = false) } }

    fun cancelTranscribe() { abort.set(true); transcribeJob?.cancel(); mutable.update { it.copy(transcribing = false, message = "취소됨") } }

    private fun cancelWork() {
        stopRecording(); recordingJob?.cancel(); if (state.value.transcribing) cancelTranscribe(); cancelCorrection()
    }

    override fun onCleared() { abort.set(true); stopRecording(); recordingJob?.cancel(); correctionJob?.cancel(); stopSpeaking(); super.onCleared() }
}
