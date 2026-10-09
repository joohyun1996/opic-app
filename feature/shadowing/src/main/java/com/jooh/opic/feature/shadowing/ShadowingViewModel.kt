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
import com.jooh.opic.core.common.compareWords
import com.jooh.opic.core.common.wordErrorRate
import com.jooh.opic.core.stt.SttModels
import com.jooh.opic.core.stt.UserWhisper
import com.jooh.opic.core.stt.WavDecoder
import com.jooh.opic.core.stt.WhisperEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** 영상별 원문은 ViewModel 생존 동안만 보관한다. 파일에는 마지막 녹음 하나만 남긴다. */
data class ShadowState(
    val videoId: String? = null, val link: String = "", val sentence: String = "",
    val model: String = "없음", val progress: Int = 0, val busy: Boolean = false,
    val recording: Boolean = false, val result: String? = null, val diff: List<WordDiff> = emptyList(),
    val wer: Double? = null, val message: String? = null,
)
private object ShadowingSentences { val byVideo = mutableMapOf<String, String>() }

class ShadowingViewModel(app: Application, private val whisper: UserWhisper, private val beforeLoad: () -> Unit) : AndroidViewModel(app) {
    private val stateValue = MutableStateFlow(ShadowState())
    val state = stateValue.asStateFlow()
    private val spec = whisper.model
    private var recordingJob: Job? = null
    private var transcriptionJob: Job? = null
    private var recorder: AudioRecord? = null
    private val abort = AtomicBoolean(false)
    val recordingFile = File(app.filesDir, "shadowing-last.pcm")
    init { stateValue.value = stateValue.value.copy(model = if (whisper.ready) "준비됨" else if (SttModels.isDownloaded(spec)) "불러오기 전" else "없음") }
    private fun update(block: (ShadowState) -> ShadowState) { stateValue.value = block(stateValue.value) }
    fun link(value: String) = update { it.copy(link = value) }
    fun open(id: String) = update { it.copy(videoId = id, sentence = ShadowingSentences.byVideo[id].orEmpty(), result = null, diff = emptyList(), message = null) }
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
        val app = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            message("마이크 권한이 필요합니다"); return
        }
        if (!whisper.ready || state.value.busy || state.value.recording) return
        recordingJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val min = AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                require(min > 0)
                val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16_000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 4)
                recorder = record
                require(record.state == AudioRecord.STATE_INITIALIZED)
                record.startRecording()
                update { it.copy(recording = true, result = null, diff = emptyList(), message = null) }
                val deadline = System.nanoTime() + 30_000_000_000L
                val buffer = ByteArray(min)
                recordingFile.outputStream().use { out ->
                    while (isActive && state.value.recording && System.nanoTime() < deadline) {
                        val n = record.read(buffer, 0, buffer.size)
                        if (n > 0) out.write(buffer, 0, n)
                    }
                }
                record.stop()
                update { it.copy(recording = false) }
                if (recordingFile.length() > 0) transcribe()
            } catch (e: Exception) {
                if (e !is CancellationException) message("녹음 실패: ${e.message}")
                update { it.copy(recording = false) }
            } finally { recorder?.release(); recorder = null }
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
                val answer = whisper.transcribe(audio, abort).text
                val diff = if (reference.isBlank()) emptyList() else compareWords(reference, answer)
                val wer = if (reference.isBlank()) null else wordErrorRate(reference, answer)
                update { it.copy(result = answer, diff = diff, wer = wer, message = null) }
            } catch (e: Exception) {
                if (e !is CancellationException) message("받아 적기 실패: ${e.message}")
            } finally { update { it.copy(busy = false) } }
        }
    }
    fun cancel() { abort.set(true); transcriptionJob?.cancel(); update { it.copy(busy = false, message = "취소됨") } }
    override fun onCleared() { abort.set(true); recordingJob?.cancel(); recorder?.stop(); super.onCleared() }
}
