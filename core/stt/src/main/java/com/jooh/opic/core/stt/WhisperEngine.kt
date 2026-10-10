package com.jooh.opic.core.stt

import android.content.Context
import com.jooh.opic.core.llm.ModelSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

internal object WhisperNative {
    init { System.loadLibrary("opic_whisper") }
    external fun initContext(modelPath: String): Long
    external fun freeContext(context: Long)
    external fun transcribe(context: Long, threads: Int, audio: FloatArray, cancelled: AtomicBoolean?, prompt: String?): ByteArray?
    external fun systemInfo(): String
}

/** 사용자용 영어 음성 인식 모델. 크기·SHA-256은 Hugging Face ggerganov/whisper.cpp 기준 (2026-10-07). */
object SttModels {
    private const val BASE = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
    fun all(context: Context): List<ModelSpec> {
        val dir = File(context.noBackupFilesDir, "stt")
        fun spec(id: String, name: String, bytes: Long, sha: String) = ModelSpec(id, File(dir, name), BASE + name, bytes, sha)
        return listOf(
            spec("small.en", "ggml-small.en-q5_1.bin", 190_098_681, "bfdff4894dcb76bbf647d56263ea2a96645423f1669176f4844a1bf8e478ad30"),
        )
    }
    fun userModel(context: Context): ModelSpec = all(context).first { it.id == "small.en" }
    fun isDownloaded(spec: ModelSpec) = spec.file.isFile && spec.file.length() == spec.expectedBytes
}

data class Transcription(val text: String, val elapsedMs: Long, val audioMs: Long) {
    val rtf: Double get() = if (audioMs == 0L) 0.0 else elapsedMs.toDouble() / audioMs
}

/** whisper.cpp 컨텍스트 하나. 한 번에 하나의 받아 적기만 실행한다. */
class WhisperEngine private constructor(private var context: Long, val modelId: String) : AutoCloseable {
    private val lock = Mutex()

    /** 16kHz mono float(-1..1) 오디오를 영어로 받아 적는다. */
    suspend fun transcribe(audio: FloatArray, threads: Int = DEFAULT_THREADS, cancelled: AtomicBoolean? = null, prompt: String? = null): Transcription = lock.withLock {
        withContext(Dispatchers.Default) {
            check(context != 0L) { "닫힌 엔진" }
            val start = System.nanoTime()
            val bytes = WhisperNative.transcribe(context, threads, audio, cancelled, prompt) ?: if (cancelled?.get() == true) throw CancellationException("받아 적기 취소") else error("받아 적기 실패")
            currentCoroutineContext().ensureActive()
            Transcription(bytes.toString(Charsets.UTF_8).trim(), (System.nanoTime() - start) / 1_000_000, audio.size * 1000L / SAMPLE_RATE)
        }
    }

    suspend fun closeWhenIdle() = lock.withLock { close() }

    override fun close() {
        if (context != 0L) WhisperNative.freeContext(context)
        context = 0L
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val DEFAULT_THREADS = 4
        fun systemInfo(): String = WhisperNative.systemInfo()

        suspend fun load(spec: ModelSpec): WhisperEngine = withContext(Dispatchers.Default) {
            require(SttModels.isDownloaded(spec)) { "모델 파일 없음: ${spec.id}" }
            val pointer = WhisperNative.initContext(spec.file.absolutePath)
            check(pointer != 0L) { "모델을 불러오지 못했습니다: ${spec.id}" }
            WhisperEngine(pointer, spec.id)
        }
    }
}

/** 16-bit PCM mono 16kHz WAV → float. TASK 14 샘플(jfk.wav)과 녹음 파일용. */
object WavDecoder {
    fun pcm16ToFloat(bytes: ByteArray, offset: Int = 0): FloatArray {
        val count = (bytes.size - offset) / 2
        return FloatArray(count) { i ->
            val lo = bytes[offset + i * 2].toInt() and 0xFF
            val hi = bytes[offset + i * 2 + 1].toInt()
            ((hi shl 8) or lo).toShort() / 32768f
        }
    }

    /** 표준 44바이트 헤더가 아닐 수 있어 "data" 청크를 찾는다. */
    fun decode(wav: ByteArray): FloatArray {
        var i = 12
        while (i + 8 <= wav.size) {
            val id = String(wav, i, 4, Charsets.US_ASCII)
            val size = (wav[i + 4].toInt() and 0xFF) or ((wav[i + 5].toInt() and 0xFF) shl 8) or
                ((wav[i + 6].toInt() and 0xFF) shl 16) or ((wav[i + 7].toInt() and 0xFF) shl 24)
            if (id == "data") return pcm16ToFloat(wav.copyOfRange(i + 8, minOf(wav.size, i + 8 + size)))
            i += 8 + size
        }
        error("WAV data 청크 없음")
    }
}
