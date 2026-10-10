package com.jooh.opic.feature.speaking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.SpeakingCatalog
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** 머뭇거림을 지우지 않게 하는 Whisper 앞 문맥. */
internal const val FILLER_PROMPT = "Um, uh, so, like, you know, I mean."
internal const val MAX_ANSWER_MS = 120_000L

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
    val message: String? = null,
)

class SpeakingViewModel(
    app: Application,
    val catalog: SpeakingCatalog?,
    private val whisper: UserWhisper,
    private val beforeLoad: () -> Unit,
    private val speak: (String) -> Unit,
    private val stopSpeaking: () -> Unit,
) : AndroidViewModel(app) {
    private val mutable = MutableStateFlow(SpeakingState())
    val state = mutable.asStateFlow()
    val recordingFile = File(app.filesDir, "speaking-last.pcm")
    private val abort = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private var transcribeJob: Job? = null

    init { refreshModel() }

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
        mutable.update { SpeakingState(page = SpeakingPage.QUESTION, topicId = topicId, question = question, model = it.model, progress = it.progress) }
        speak(question.en)
    }

    fun replay() {
        val s = state.value
        if (s.replaysLeft <= 0 || s.recording || s.question == null) return
        mutable.update { it.copy(replaysLeft = it.replaysLeft - 1) }
        speak(s.question.en)
    }

    fun toggleText() = mutable.update { it.copy(showText = !it.showText) }

    fun backToTopics() { cancelWork(); stopSpeaking(); mutable.update { SpeakingState(model = it.model) } }

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
        if (!whisper.ready) { refreshModel(); return }
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
                val text = cleanWhisperText(whisper.transcribe(audio, abort, FILLER_PROMPT).text)
                if (text.isBlank()) mutable.update { it.copy(message = "말소리가 잘 들리지 않았어요 — 폰을 입에 가까이 대 주세요") }
                else mutable.update { it.copy(page = SpeakingPage.RESULT, transcript = text, metrics = speakingMetrics(text, durationMs)) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) { mutable.update { it.copy(message = "받아 적기 실패: ${e.message}") }
            } finally { mutable.update { it.copy(transcribing = false) } }
        }
    }

    fun cancelTranscribe() { abort.set(true); transcribeJob?.cancel(); mutable.update { it.copy(transcribing = false, message = "취소됨") } }

    private fun cancelWork() { stopRecording(); recordingJob?.cancel(); if (state.value.transcribing) cancelTranscribe() }

    override fun onCleared() { abort.set(true); stopRecording(); recordingJob?.cancel(); stopSpeaking(); super.onCleared() }
}
