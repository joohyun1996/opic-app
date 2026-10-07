package com.jooh.opic.debug

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Debug
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jooh.opic.OpicApplication
import com.jooh.opic.core.common.wordErrorRate
import com.jooh.opic.core.llm.HttpModelStore
import com.jooh.opic.core.stt.SttModels
import com.jooh.opic.core.stt.WavDecoder
import com.jooh.opic.core.stt.WhisperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/** TASK 14: Whisper 측정용 대본 (각 약 30초, OPIc 답변 형식). 사용자가 읽고 녹음한다. */
private val SCRIPTS = listOf(
    "I live in a small apartment near a big park. Every morning, I usually walk my dog there before work. " +
        "These days, I'm also learning how to cook, so I go to the market on weekends and buy fresh vegetables. " +
        "My neighborhood is quiet, but there are a lot of nice cafés, and I really like it.",
    "Last summer, I went to Jeju Island with my family. We stayed at a hotel near the beach for four days. " +
        "On the second day, it rained a lot, so we visited a museum instead of going swimming. " +
        "Although the weather wasn't perfect, it was one of the best trips I have ever taken.",
    "When I was a child, I used to play outside with my friends every day after school. " +
        "We didn't have smartphones, so we would ride our bikes or play soccer until dinner. " +
        "Now, kids spend much more time on their phones, and I think that's a big difference between then and now.",
)

private data class Sample(val name: String, val reference: String?, val file: File)
private data class Measure(val model: String, val sample: String, val audioMs: Long, val elapsedMs: Long, val rtf: Double, val wer: Double?, val pssMb: Int, val text: String)

@SuppressLint("MissingPermission")
@Composable
fun SttBenchScreen(app: OpicApplication, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val models = remember { SttModels.all(app) }
    val store = remember { HttpModelStore() }
    val dir = remember { File(app.filesDir, "stt").apply { mkdirs() } }
    val jfk = remember {
        File(dir, "jfk.pcm").apply { if (!exists()) app.assets.open("stt/jfk.wav").use { writeBytes(it.readBytes()) } }
    }
    val samples = remember {
        listOf(Sample("jfk (원어민 11초)", "And so my fellow Americans, ask not what your country can do for you, ask what you can do for your country.", jfk)) +
            SCRIPTS.mapIndexed { i, text -> Sample("대본 ${i + 1}", text, File(dir, "script-${i + 1}.pcm")) }
    }
    var refresh by remember { mutableIntStateOf(0) }
    var engine by remember { mutableStateOf<WhisperEngine?>(null) }
    var status by remember { mutableStateOf("모델을 받아서 불러오세요") }
    var progress by remember { mutableStateOf<String?>(null) }
    var recording by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<Measure>() }
    var granted by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    DisposableEffect(Unit) { onDispose { engine?.close() } }

    fun audioOf(sample: Sample): FloatArray? = if (!sample.file.isFile) null
        else if (sample.file == jfk) WavDecoder.decode(sample.file.readBytes()) else WavDecoder.pcm16ToFloat(sample.file.readBytes())

    fun measure(list: List<Sample>) {
        val current = engine ?: return
        busy = true
        scope.launch {
            for (sample in list) {
                val audio = withContext(Dispatchers.IO) { audioOf(sample) } ?: continue
                status = "${current.modelId}: ${sample.name} 받아 적는 중…"
                val result = runCatching { current.transcribe(audio) }.getOrElse { status = "실패: ${it.message}"; null } ?: continue
                val pss = (Debug.getPss() / 1024).toInt()
                val wer = sample.reference?.let { wordErrorRate(it, result.text) }
                val row = Measure(current.modelId, sample.name, result.audioMs, result.elapsedMs, result.rtf, wer, pss, result.text)
                results.add(0, row)
                Log.i("SttBench", String.format(Locale.US, "model=%s sample=%s audioMs=%d elapsedMs=%d rtf=%.2f wer=%s pssMb=%d text=%s",
                    row.model, row.sample, row.audioMs, row.elapsedMs, row.rtf, wer?.let { String.format(Locale.US, "%.3f", it) } ?: "-", pss, row.text))
            }
            status = "완료"
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onBack) { Text("← 뒤로") }
        Text("STT 검증 (Whisper)", style = MaterialTheme.typography.headlineSmall)
        Text(remember { WhisperEngine.systemInfo() }, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(status)
        progress?.let { Text(it) }

        key(refresh) {
            for (spec in models) {
                val downloaded = SttModels.isDownloaded(spec)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${spec.id} (${spec.expectedBytes / 1_000_000}MB)" + if (engine?.modelId == spec.id) " ← 사용 중" else "", Modifier.weight(1f))
                    if (!downloaded) OutlinedButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            runCatching {
                                store.ensure(spec) { done, total -> progress = "${spec.id} 받는 중 ${done * 100 / total}% (${done / 1_000_000}MB)" }
                            }.onFailure { status = "다운로드 실패: ${it.message}" }
                            progress = null; busy = false; refresh++
                        }
                    }) { Text("받기") }
                    else Button(enabled = !busy && engine?.modelId != spec.id, onClick = {
                        busy = true
                        scope.launch {
                            engine?.close(); engine = null
                            status = "${spec.id} 불러오는 중…"
                            val start = System.nanoTime()
                            runCatching { WhisperEngine.load(spec) }
                                .onSuccess {
                                    engine = it
                                    val ms = (System.nanoTime() - start) / 1_000_000
                                    status = "${spec.id} 불러옴 ${ms}ms, PSS ${Debug.getPss() / 1024}MB"
                                    Log.i("SttBench", "load model=${spec.id} ms=$ms pssMb=${Debug.getPss() / 1024}")
                                }
                                .onFailure { status = "불러오기 실패: ${it.message}" }
                            busy = false
                        }
                    }) { Text("불러오기") }
                }
            }
        }

        HorizontalDivider()
        Button(enabled = engine != null && !busy, onClick = { measure(samples) }, modifier = Modifier.fillMaxWidth()) { Text("녹음된 것 전체 측정 (현재 모델)") }

        samples.forEachIndexed { index, sample ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    key(refresh) { Text(sample.name + if (sample.file.isFile) " · 녹음됨" else " · 녹음 없음", style = MaterialTheme.typography.titleSmall) }
                    sample.reference?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (index > 0) OutlinedButton(enabled = !busy && (recording == null || recording == index), onClick = {
                            if (recording == index) { recording = null; return@OutlinedButton }
                            if (!granted) { permission.launch(Manifest.permission.RECORD_AUDIO); return@OutlinedButton }
                            recording = index
                            scope.launch(Dispatchers.IO) {
                                val min = AudioRecord.getMinBufferSize(WhisperEngine.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                                val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, WhisperEngine.SAMPLE_RATE,
                                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 4)
                                val buffer = ByteArray(min)
                                FileOutputStream(sample.file).use { out ->
                                    recorder.startRecording()
                                    while (isActive && recording == index) {
                                        val read = recorder.read(buffer, 0, buffer.size)
                                        if (read > 0) out.write(buffer, 0, read)
                                    }
                                    recorder.stop()
                                }
                                recorder.release()
                                refresh++
                            }
                        }) { Text(if (recording == index) "■ 녹음 정지" else "● 녹음") }
                        Button(enabled = engine != null && !busy && sample.file.isFile, onClick = { measure(listOf(sample)) }) { Text("받아 적기") }
                    }
                }
            }
        }

        HorizontalDivider()
        Text("결과 (logcat 태그 SttBench)", style = MaterialTheme.typography.titleSmall)
        for (row in results) {
            Text(String.format(Locale.US, "%s · %s · 음성 %.1f초 · 처리 %.1f초 · RTF %.2f · WER %s · PSS %dMB",
                row.model, row.sample, row.audioMs / 1000.0, row.elapsedMs / 1000.0, row.rtf,
                row.wer?.let { String.format(Locale.US, "%.1f%%", it * 100) } ?: "-", row.pssMb), style = MaterialTheme.typography.bodySmall)
            Text(row.text, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}
