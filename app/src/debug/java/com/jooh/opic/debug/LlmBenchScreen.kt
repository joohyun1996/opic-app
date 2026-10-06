package com.jooh.opic.debug

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.jooh.opic.OpicApplication
import com.jooh.opic.core.common.CorrectionParse
import com.jooh.opic.core.common.buildCorrectionPrompt
import com.jooh.opic.core.common.parseCorrection
import com.jooh.opic.core.llm.LlmEngineState
import com.jooh.opic.core.llm.ModelCatalog
import com.jooh.opic.core.llm.llmProgressLabel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.os.SystemClock

private data class BenchSentence(val sentence: String, val expectedCorrect: Boolean, val note: String)
private data class BenchRow(val index: Int, val status: String, val elapsedMs: Long, val matches: Boolean?, val sentence: String)

@Composable
fun LlmBenchScreen(app: OpicApplication, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val engineState by app.llmEngine.state.collectAsState()
    var tokenInput by remember { mutableStateOf("") }
    var tokenSaved by remember { mutableStateOf(app.hfTokenStore.getToken() != null) }
    var confirmDownload by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var firstLoadMs by remember { mutableStateOf<Long?>(null) }
    var rows by remember { mutableStateOf(emptyList<BenchRow>()) }

    fun prepare() {
        scope.launch {
            busy = true
            val start = SystemClock.elapsedRealtime()
            val result = app.llmEngine.ensureModelReady()
            firstLoadMs = SystemClock.elapsedRealtime() - start
            message = if (result.isSuccess) "모델 준비 완료" else "모델 준비 실패: ${result.exceptionOrNull()?.javaClass?.simpleName}"
            Log.i("LlmBench", "LOAD\t${firstLoadMs}\t${if (result.isSuccess) "Ok" else "Failed"}")
            busy = false
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row {
            TextButton(onClick = onBack) { Text("뒤로") }
            Text("LLM 검증")
        }
        OutlinedTextField(
            value = tokenInput,
            onValueChange = { tokenInput = it },
            label = { Text(if (tokenSaved) "HF 토큰 (저장됨)" else "HF 토큰") },
            visualTransformation = PasswordVisualTransformation(),
        )
        Button(onClick = {
            if (tokenInput.isNotBlank()) {
                app.hfTokenStore.setToken(tokenInput)
                tokenInput = ""
                tokenSaved = true
                message = "토큰 저장 완료"
            }
        }) { Text("토큰 저장") }
        Text(llmProgressLabel(engineState, engineState.javaClass.simpleName))
        Button(onClick = {
            if (ModelCatalog.isDownloaded(app)) prepare() else confirmDownload = true
        }, enabled = !busy) { Text("모델 준비") }
        Button(onClick = {
            scope.launch {
                busy = true
                rows = emptyList()
                val input = JSONArray(app.assets.open("llm-bench/sentences.json").bufferedReader().use { it.readText() })
                for (i in 0 until input.length()) {
                    val item = input.getJSONObject(i)
                    val sample = BenchSentence(item.getString("sentence"), item.getBoolean("expectedCorrect"), item.getString("note"))
                    val start = SystemClock.elapsedRealtime()
                    val result = app.llmEngine.generate(buildCorrectionPrompt(sample.sentence), timeoutMs = 120_000)
                    val elapsed = SystemClock.elapsedRealtime() - start
                    val raw = result.getOrNull()
                    val parsed = if (raw == null) null else parseCorrection(raw, sample.sentence)
                    val status = when {
                        raw == null -> if (result.exceptionOrNull()?.message?.contains("timeout", true) == true) "Timeout" else "Failed"
                        parsed is CorrectionParse.Ok -> "Ok"
                        parsed is CorrectionParse.InvalidJson -> "InvalidJson"
                        else -> "Contradiction"
                    }
                    val matches = (parsed as? CorrectionParse.Ok)?.result?.correct?.let { it == sample.expectedCorrect }
                    rows = rows + BenchRow(i + 1, status, elapsed, matches, sample.sentence)
                    val log = JSONObject().put("index", i + 1).put("status", status).put("elapsedMs", elapsed)
                        .put("matches", matches).put("sentence", sample.sentence).put("raw", raw ?: "")
                    Log.i("LlmBench", log.toString())
                }
                message = "20문장 측정 완료"
                busy = false
            }
        }, enabled = !busy && engineState is LlmEngineState.Ready) { Text("벤치 실행") }
        Text(message)
        Text("첫 로딩: ${firstLoadMs ?: "-"} ms")
        rows.forEach { Text("${it.index}. ${it.status} / ${it.elapsedMs} ms / ${it.matches ?: "-"} — ${it.sentence}") }
    }
    if (confirmDownload) AlertDialog(
        onDismissRequest = { confirmDownload = false },
        title = { Text("모델 다운로드") },
        text = { Text("약 4.4GB를 받습니다.") },
        confirmButton = { TextButton(onClick = { confirmDownload = false; prepare() }) { Text("받기") } },
        dismissButton = { TextButton(onClick = { confirmDownload = false }) { Text("취소") } },
    )
}
