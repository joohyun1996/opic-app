package com.jooh.opic.feature.shadowing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.WordDiff
import com.jooh.opic.core.common.Cue
import com.jooh.opic.core.common.StudyLanguage
import com.jooh.opic.core.common.compareWords
import com.jooh.opic.core.common.matchRate
import com.jooh.opic.core.common.werWords
import com.jooh.opic.core.stt.SttModels
import com.jooh.opic.core.database.ShadowingAttemptDao
import com.jooh.opic.core.database.ShadowingAttemptEntity
import com.jooh.opic.core.database.VideoPracticeRow
import com.jooh.opic.core.stt.PcmRecorder
import com.jooh.opic.core.common.cleanWhisperText
import com.jooh.opic.core.common.recordingTooShort
import com.jooh.opic.core.stt.UserWhisper
import com.jooh.opic.core.stt.WavDecoder
import com.jooh.opic.core.stt.WhisperEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** 영상별 원문은 ViewModel 생존 동안만 보관한다. 파일에는 마지막 녹음 하나만 남긴다. */
data class ShadowState(
    val videoId: String? = null, val link: String = "", val sentence: String = "",
    val model: String = "없음", val progress: Int = 0, val busy: Boolean = false,
    val recording: Boolean = false, val recordingLevel: Float = 0f,
    val result: String? = null, val diff: List<WordDiff> = emptyList(),
    val matchRate: Double? = null, val message: String? = null,
    val captions: List<Cue> = emptyList(), val captionStatus: CaptionStatus = CaptionStatus.NONE,
    /** 영상별 연습 기록 (TASK 22). */
    val practice: Map<String, VideoPracticeRow> = emptyMap(),
)
enum class CaptionStatus { LOADING, READY, NONE, FAILED }
private object CaptionCache { val byVideo = ConcurrentHashMap<String, List<Cue>>() }
private object ShadowingSentences { val byVideo = mutableMapOf<String, String>() }

class ShadowingViewModel(app: Application, private val whisper: UserWhisper, private val beforeLoad: () -> Unit,
                         val language: StudyLanguage, private val history: ShadowingAttemptDao? = null) : AndroidViewModel(app) {
    private val stateValue = MutableStateFlow(ShadowState())
    val state = stateValue.asStateFlow()
    private val spec = whisper.model
    private var recordingJob: Job? = null
    private var transcriptionJob: Job? = null
    private val abort = AtomicBoolean(false)
    private val captionClient = CaptionClient(language.code)
    private var captionJob: Job? = null
    private var captionRequest = 0L
    private var playerCaptionKind: String? = null
    val recordingFile = File(app.filesDir, "shadowing-last.pcm")
    init {
        stateValue.value = stateValue.value.copy(model = if (whisper.ready) "준비됨" else if (SttModels.isDownloaded(spec)) "불러오기 전" else "없음")
        refreshPractice()
    }

    private fun refreshPractice() {
        val dao = history ?: return
        viewModelScope.launch {
            val rows = runCatching { dao.recentVideos(language.code) }.getOrDefault(emptyList())
            update { it.copy(practice = rows.associateBy { r -> r.videoId }) }
        }
    }
    private fun update(block: (ShadowState) -> ShadowState) { stateValue.update(block) }
    fun link(value: String) = update { it.copy(link = value) }
    /** 영상을 닫고 추천 목록으로 (TASK 21). */
    fun close() {
        cancelCaptions()
        update { it.copy(videoId = null, result = null, diff = emptyList(), message = null, captions = emptyList(), captionStatus = CaptionStatus.NONE) }
    }

    fun open(id: String) {
        cancelCaptions()
        playerCaptionKind = null
        update { it.copy(videoId = id, sentence = ShadowingSentences.byVideo["${language.code}:$id"].orEmpty(), result = null,
            diff = emptyList(), message = null, captions = emptyList(), captionStatus = CaptionStatus.NONE) }
        loadCaptions()
    }
    fun loadCaptions(retry: Boolean = false) {
        val id = state.value.videoId ?: return
        if (!retry && (captionJob?.isActive == true || state.value.captionStatus == CaptionStatus.FAILED)) return
        cancelCaptions()
        val request = captionRequest
        val cacheKey = "${language.code}:$id"
        val cached = if (retry) null else CaptionCache.byVideo[cacheKey]
        if (cached != null) {
            update { it.copy(captions = cached, captionStatus = if (cached.isEmpty()) CaptionStatus.NONE else CaptionStatus.READY) }
            return
        }
        update { it.copy(captionStatus = CaptionStatus.LOADING) }
        captionJob = viewModelScope.launch {
            try {
                // 플레이어가 자막을 요청할 시간 (열 때 잠깐 재생시켜 요청을 일으킨다, TASK 23)
                if (!retry) delay(10_000)
                if (state.value.captionStatus == CaptionStatus.READY) return@launch
                val cues = captionClient.fetch(id)
                ensureActive()
                if (request != captionRequest || state.value.videoId != id) return@launch
                if (state.value.captionStatus == CaptionStatus.READY) return@launch
                CaptionCache.byVideo[cacheKey] = cues
                update { it.copy(captions = cues, captionStatus = if (cues.isEmpty()) CaptionStatus.NONE else CaptionStatus.READY) }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (request == captionRequest && state.value.videoId == id) {
                    update { it.copy(captions = emptyList(), captionStatus = CaptionStatus.FAILED) }
                }
            }
        }
    }
    fun playerCaptions(id: String, cues: List<Cue>, kind: String?) {
        if (state.value.videoId != id || cues.isEmpty()) return
        if (playerCaptionKind == null || playerCaptionKind == "asr" || kind != "asr") {
            playerCaptionKind = kind ?: "human"
            CaptionCache.byVideo["${language.code}:$id"] = cues
            update { it.copy(captions = cues, captionStatus = CaptionStatus.READY) }
        }
    }
    fun cancelCaptions() { captionRequest++; captionJob?.cancel(); captionJob = null }

    fun sentence(value: String) {
        state.value.videoId?.let { ShadowingSentences.byVideo["${language.code}:$it"] = value }
        update { it.copy(sentence = value) }
    }
    fun message(value: String) = update { it.copy(message = value) }
    fun prepare(download: Boolean) {
        if (state.value.busy || state.value.recording) return
        if (!download && !SttModels.isDownloaded(spec)) return
        update { it.copy(busy = true, model = if (SttModels.isDownloaded(spec)) "불러오는 중" else "받는 중", message = null) }
        viewModelScope.launch {
            try {
                whisper.prepare(download, { beforeLoad(); update { it.copy(model = "불러오는 중") } }) { done, total ->
                    update { it.copy(progress = (done * 100 / total).toInt()) }
                }
                update { it.copy(model = "준비됨") }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                update { it.copy(model = "실패", message = e.message) }
            } finally { update { it.copy(busy = false) } }
        }
    }
    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (recordingJob?.isActive == true) return
        val app = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            message("마이크 권한이 필요합니다"); return
        }
        if (!whisper.ready || state.value.busy || state.value.recording) return
        update { it.copy(recording = true, recordingLevel = 0f, result = null, diff = emptyList(), matchRate = null, message = null) }
        recordingJob = viewModelScope.launch {
            try {
                val bytes = PcmRecorder.record(recordingFile, 30_000, { state.value.recording }) { level ->
                    update { it.copy(recordingLevel = level) }
                }
                ensureActive()
                update { it.copy(recording = false, recordingLevel = 0f) }
                if (recordingTooShort(bytes)) message("너무 짧아요 — 문장 전체를 말해 보세요")
                else transcribe()
            } catch (e: Exception) {
                if (e !is CancellationException) message("녹음 실패: ${e.message}")
            } finally {
                update { it.copy(recording = false, recordingLevel = 0f) }
            }
        }
    }
    fun stopRecording() = update { it.copy(recording = false) }
    private fun transcribe() {
        if (!whisper.ready) return
        val reference = state.value.sentence
        abort.set(false)
        update { it.copy(busy = true, message = "받아 적는 중") }
        transcriptionJob = viewModelScope.launch {
            try {
                val audio = withContext(Dispatchers.IO) { WavDecoder.pcm16ToFloat(recordingFile.readBytes()) }
                val answer = cleanWhisperText(whisper.transcribe(audio, abort).text)
                if (answer.isBlank()) {
                    update { it.copy(result = null, diff = emptyList(), matchRate = null,
                        message = "말소리가 잘 들리지 않았어요 — 폰을 입에 가까이") }
                    return@launch
                }
                val diff = if (reference.isBlank()) emptyList() else compareWords(reference, answer)
                val rate = if (reference.isBlank()) null else matchRate(diff, werWords(reference).size)
                update { it.copy(result = answer, diff = diff, matchRate = rate, message = null) }
                val videoId = state.value.videoId
                if (rate != null && videoId != null) {
                    runCatching { history?.insert(ShadowingAttemptEntity(language = language.code, videoId = videoId, sentence = reference, heard = answer, matchRate = rate, createdAt = System.currentTimeMillis())) }
                    refreshPractice()
                }
            } catch (e: Exception) {
                if (e !is CancellationException) message("받아 적기 실패: ${e.message}")
            } finally { update { it.copy(busy = false) } }
        }
    }
    fun cancel() { abort.set(true); transcriptionJob?.cancel(); update { it.copy(busy = false, message = "취소됨") } }
    override fun onCleared() { cancelCaptions(); abort.set(true); stopRecording(); recordingJob?.cancel(); super.onCleared() }
}
