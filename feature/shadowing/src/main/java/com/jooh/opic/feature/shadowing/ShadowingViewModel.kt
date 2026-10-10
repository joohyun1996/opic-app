package com.jooh.opic.feature.shadowing

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jooh.opic.core.common.WordDiff
import com.jooh.opic.core.common.Cue
import com.jooh.opic.core.common.compareWords
import com.jooh.opic.core.common.wordErrorRate
import com.jooh.opic.core.stt.SttModels
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
    val wer: Double? = null, val message: String? = null,
    val captions: List<Cue> = emptyList(), val captionStatus: CaptionStatus = CaptionStatus.NONE,
)
enum class CaptionStatus { LOADING, READY, NONE, FAILED }
private object CaptionCache { val byVideo = ConcurrentHashMap<String, List<Cue>>() }
private object ShadowingSentences { val byVideo = mutableMapOf<String, String>() }

class ShadowingViewModel(app: Application, private val whisper: UserWhisper, private val beforeLoad: () -> Unit) : AndroidViewModel(app) {
    private val stateValue = MutableStateFlow(ShadowState())
    val state = stateValue.asStateFlow()
    private val spec = whisper.model
    private var recordingJob: Job? = null
    private var transcriptionJob: Job? = null
    private val abort = AtomicBoolean(false)
    private val captionClient = CaptionClient()
    private var captionJob: Job? = null
    private var captionRequest = 0L
    private var playerCaptionKind: String? = null
    val recordingFile = File(app.filesDir, "shadowing-last.pcm")
    init { stateValue.value = stateValue.value.copy(model = if (whisper.ready) "준비됨" else if (SttModels.isDownloaded(spec)) "불러오기 전" else "없음") }
    private fun update(block: (ShadowState) -> ShadowState) { stateValue.update(block) }
    fun link(value: String) = update { it.copy(link = value) }
    fun open(id: String) {
        cancelCaptions()
        playerCaptionKind = null
        update { it.copy(videoId = id, sentence = ShadowingSentences.byVideo[id].orEmpty(), result = null,
            diff = emptyList(), message = null, captions = emptyList(), captionStatus = CaptionStatus.NONE) }
        loadCaptions()
    }
    fun loadCaptions(retry: Boolean = false) {
        val id = state.value.videoId ?: return
        if (!retry && (captionJob?.isActive == true || state.value.captionStatus == CaptionStatus.FAILED)) return
        cancelCaptions()
        val request = captionRequest
        val cached = if (retry) null else CaptionCache.byVideo[id]
        if (cached != null) {
            update { it.copy(captions = cached, captionStatus = if (cached.isEmpty()) CaptionStatus.NONE else CaptionStatus.READY) }
            return
        }
        update { it.copy(captionStatus = CaptionStatus.LOADING) }
        captionJob = viewModelScope.launch {
            try {
                if (!retry) delay(8_000)
                if (state.value.captionStatus == CaptionStatus.READY) return@launch
                val cues = captionClient.fetch(id)
                ensureActive()
                if (request != captionRequest || state.value.videoId != id) return@launch
                if (state.value.captionStatus == CaptionStatus.READY) return@launch
                CaptionCache.byVideo[id] = cues
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
            CaptionCache.byVideo[id] = cues
            update { it.copy(captions = cues, captionStatus = CaptionStatus.READY) }
        }
    }
    fun cancelCaptions() { captionRequest++; captionJob?.cancel(); captionJob = null }

    fun sentence(value: String) {
        state.value.videoId?.let { ShadowingSentences.byVideo[it] = value }
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
        update { it.copy(recording = true, recordingLevel = 0f, result = null, diff = emptyList(), wer = null, message = null) }
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            var recorder: AudioRecord? = null
            try {
                ensureActive()
                val min = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                require(min > 0)
                val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16_000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 4)
                recorder = record
                require(record.state == AudioRecord.STATE_INITIALIZED)
                record.startRecording()
                val deadline = System.nanoTime() + 30_000_000_000L
                val buffer = ByteArray(min)
                var levelEnergy = 0.0
                var levelSamples = 0
                recordingFile.outputStream().use { out ->
                    while (isActive && state.value.recording && System.nanoTime() < deadline) {
                        val n = record.read(buffer, 0, buffer.size)
                        if (n > 0) {
                            out.write(buffer, 0, n)
                            for (index in 0 until n - 1 step 2) {
                                val sample = ((buffer[index + 1].toInt() shl 8) or (buffer[index].toInt() and 0xff)).toShort().toInt()
                                levelEnergy += sample.toDouble() * sample
                                levelSamples++
                                if (levelSamples >= 1_600) {
                                    val rms = kotlin.math.sqrt(levelEnergy / levelSamples)
                                    update { it.copy(recordingLevel = (rms / 6_000).toFloat().coerceIn(0f, 1f)) }
                                    levelEnergy = 0.0
                                    levelSamples = 0
                                }
                            }
                        }
                    }
                }
                ensureActive()
                update { it.copy(recording = false, recordingLevel = 0f) }
                if (recordingTooShort(recordingFile.length())) message("너무 짧아요 — 문장 전체를 말해 보세요")
                else transcribe()
            } catch (e: Exception) {
                if (e !is CancellationException) message("녹음 실패: ${e.message}")
                update { it.copy(recording = false, recordingLevel = 0f) }
            } finally {
                // AudioRecord는 생성한 IO 작업만 정리한다. 화면 이탈은 취소 신호만 보낸다.
                recorder?.let { record ->
                    try {
                        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
                    } finally { record.release() }
                }
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
                    update { it.copy(result = null, diff = emptyList(), wer = null,
                        message = "말소리가 잘 들리지 않았어요 — 폰을 입에 가까이") }
                    return@launch
                }
                val diff = if (reference.isBlank()) emptyList() else compareWords(reference, answer)
                val wer = if (reference.isBlank()) null else wordErrorRate(reference, answer)
                update { it.copy(result = answer, diff = diff, wer = wer, message = null) }
            } catch (e: Exception) {
                if (e !is CancellationException) message("받아 적기 실패: ${e.message}")
            } finally { update { it.copy(busy = false) } }
        }
    }
    fun cancel() { abort.set(true); transcriptionJob?.cancel(); update { it.copy(busy = false, message = "취소됨") } }
    override fun onCleared() { cancelCaptions(); abort.set(true); stopRecording(); recordingJob?.cancel(); super.onCleared() }
}
