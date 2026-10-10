package com.jooh.opic.core.stt

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** [PcmRecorder]가 남긴 16kHz mono PCM16 파일을 끝까지 재생한다 (섀도잉·스피킹 "내 목소리 듣기"). */
object PcmPlayer {
    /** [fromMs]~[toMs] 구간만 재생한다 (기본: 전체). */
    suspend fun play(file: File, fromMs: Long = 0, toMs: Long = Long.MAX_VALUE) = withContext(Dispatchers.IO) {
        val all = file.readBytes()
        fun offset(ms: Long) = (ms.coerceAtLeast(0).coerceAtMost(all.size * 1000L / (WhisperEngine.SAMPLE_RATE * 2)) * WhisperEngine.SAMPLE_RATE * 2 / 1000).toInt() and 1.inv()
        val audio = all.copyOfRange(offset(fromMs), offset(toMs).coerceAtLeast(offset(fromMs)))
        if (audio.isEmpty()) return@withContext
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(WhisperEngine.SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(audio.size.coerceAtLeast(4096)).setTransferMode(AudioTrack.MODE_STATIC).build()
        try {
            track.write(audio, 0, audio.size)
            track.play()
            delay(audio.size * 1000L / (WhisperEngine.SAMPLE_RATE * 2) + 200)
        } finally { track.release() }
    }
}
