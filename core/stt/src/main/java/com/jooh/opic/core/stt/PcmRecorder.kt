package com.jooh.opic.core.stt

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

/** 16kHz mono PCM16 녹음 (섀도잉·스피킹 공용). AudioRecord는 이 함수 안에서만 만들고 정리한다. */
object PcmRecorder {
    /** [keepGoing]이 false가 되거나 [maxMs]가 지나면 멈춘다. 100ms마다 입력 크기(0~1)를 [onLevel]로 알린다. 녹음된 바이트 수를 돌려준다. 호출 전에 권한을 확인한다. */
    @SuppressLint("MissingPermission") // 호출하는 쪽이 RECORD_AUDIO 권한을 먼저 확인한다
    suspend fun record(file: File, maxMs: Long, keepGoing: () -> Boolean, onLevel: (Float) -> Unit): Long = withContext(Dispatchers.IO) {
        val rate = WhisperEngine.SAMPLE_RATE
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        require(min > 0) { "녹음을 시작할 수 없습니다" }
        val record = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 4)
        try {
            require(record.state == AudioRecord.STATE_INITIALIZED) { "마이크를 열 수 없습니다" }
            record.startRecording()
            val deadline = System.nanoTime() + maxMs * 1_000_000
            val buffer = ByteArray(min)
            var energy = 0.0
            var samples = 0
            var written = 0L
            file.outputStream().use { out ->
                while (keepGoing() && System.nanoTime() < deadline) {
                    ensureActive()
                    val n = record.read(buffer, 0, buffer.size)
                    if (n <= 0) continue
                    out.write(buffer, 0, n)
                    written += n
                    for (i in 0 until n - 1 step 2) {
                        val sample = ((buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xff)).toShort().toInt()
                        energy += sample.toDouble() * sample
                        if (++samples >= rate / 10) {
                            onLevel((sqrt(energy / samples) / 6_000).toFloat().coerceIn(0f, 1f))
                            energy = 0.0; samples = 0
                        }
                    }
                }
            }
            written
        } finally {
            try { if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop() } finally { record.release() }
        }
    }
}
