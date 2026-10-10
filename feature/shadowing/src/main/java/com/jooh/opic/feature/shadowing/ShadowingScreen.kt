package com.jooh.opic.feature.shadowing

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jooh.opic.core.common.WordDiff
import com.jooh.opic.core.common.flapWords
import com.jooh.opic.core.common.linkingPairs
import com.jooh.opic.core.common.pronunciationTips
import com.jooh.opic.core.common.reductions
import com.jooh.opic.core.common.werWords
import com.jooh.opic.core.ui.PronunciationHintsCard
import com.jooh.opic.core.ui.UnclearHint
import com.jooh.opic.core.stt.PcmPlayer
import com.jooh.opic.core.common.cueAt
import com.jooh.opic.core.common.youtubeVideoId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShadowingScreen(model: ShadowingViewModel = viewModel(), onBack: () -> Unit, speak: (String) -> Unit = {}) {
    val state by model.state.collectAsState()
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    var current by remember { mutableDoubleStateOf(0.0) }
    var playerStatus by remember(state.videoId) { mutableStateOf("플레이어 준비 중") }
    var a by remember { mutableStateOf<Double?>(null) }
    var b by remember { mutableStateOf<Double?>(null) }
    var repeat by remember { mutableStateOf(false) }
    var selectedCaption by remember(state.videoId, state.captions) { mutableStateOf<Int?>(null) }
    LaunchedEffect(state.videoId) { if (state.videoId != null) model.loadCaptions() }
    DisposableEffect(model) { onDispose { model.cancelCaptions(); player = null } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { player?.pause(); model.startRecording() } else model.message("마이크 권한이 필요합니다")
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(repeat, a, b, state.videoId) {
        while (repeat) {
            val start = a; val end = b
            if (start != null && end != null && current >= end) player?.seek(start)
            kotlinx.coroutines.delay(150)
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TextButton(onClick = onBack) { Text("← 홈") }
        Text("섀도잉", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(state.link, model::link, label = { Text("YouTube 링크") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val id = youtubeVideoId(state.link)
            if (id == null) model.message("YouTube 링크를 확인하세요") else { model.open(id); a = null; b = null; repeat = false }
        }) { Text("열기") }
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.videoId?.let { id ->
            key(id) { PlayerView(id, Modifier.fillMaxWidth().height(220.dp), { player = it }, { current = it },
                { playerStatus = "플레이어 오류 $it"; model.message("영상 오류 코드 $it") },
                { playerStatus = "플레이어 준비됨" },
                { if (it == 1) playerStatus = "플레이어 준비됨" },
                { cues, kind -> model.playerCaptions(id, cues, kind) }) }
            Text(playerStatus)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { player?.play() }) { Text("재생") }
                OutlinedButton(onClick = { player?.pause() }) { Text("일시정지") }
            }
            Text("현재 ${"%.1f".format(current)}초")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { a = current; if (b != null && a!! >= b!!) { b = null; repeat = false } }) { Text("A 지정") }
                OutlinedButton(onClick = { if (a != null && current > a!!) b = current else model.message("B는 A보다 뒤여야 합니다") }) { Text("B 지정") }
                Button(enabled = a != null && b != null && a!! < b!!, onClick = { repeat = !repeat }) { Text(if (repeat) "반복 끔" else "반복 켬") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { a = (a ?: current).minus(0.5).coerceAtLeast(0.0) }) { Text("A −0.5") }
                OutlinedButton(onClick = { val next = (a ?: current) + 0.5; if (b == null || next < b!!) a = next }) { Text("A +0.5") }
                OutlinedButton(onClick = { val next = (b ?: current) - 0.5; if (a == null || next > a!!) b = next }) { Text("B −0.5") }
                OutlinedButton(onClick = { b = (b ?: current) + 0.5 }) { Text("B +0.5") }
            }
            Text("A ${a?.let { "%.1f".format(it) } ?: "—"} / B ${b?.let { "%.1f".format(it) } ?: "—"}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0.5, 0.75, 1.0).forEach { speed -> OutlinedButton(onClick = { player?.speed(speed) }) { Text("${speed}×") } }
            }
            CaptionList(state.captions, state.captionStatus, cueAt(state.captions, (current * 1000).toLong()), selectedCaption,
                onSelect = { index ->
                    state.captions.getOrNull(index)?.let { cue ->
                        selectedCaption = index
                        a = (cue.startMs / 1000.0 - 0.3).coerceAtLeast(0.0)
                        b = cue.endMs / 1000.0 + 0.3
                        model.sentence(cue.text)
                        player?.seek(a!!)
                        repeat = true
                    }
                }, onRetry = { model.loadCaptions(retry = true) })
            OutlinedTextField(state.sentence, model::sentence, label = { Text("지금 구간의 원문 문장") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Text("음성 인식: ${state.model}" + if (state.model == "받는 중") " ${state.progress}%" else "")
            if (state.model != "준비됨") Button(enabled = !state.busy, onClick = { model.prepare(true) }) {
                Text(if (state.model == "없음") "음성 인식 모델 받기 (190MB)" else if (state.model == "실패") "다시 시도" else "모델 불러오기")
            }
            if (state.recording) Button(onClick = model::stopRecording) { Text("■ 정지") }
            else Button(enabled = state.model == "준비됨" && !state.busy, onClick = { player?.pause(); permission.launch(Manifest.permission.RECORD_AUDIO) }) { Text("● 녹음") }
            if (state.recording) {
                Text("입력 소리 크기")
                LinearProgressIndicator(progress = { state.recordingLevel }, modifier = Modifier.fillMaxWidth())
            }
            if (state.message?.startsWith("너무 짧아요") == true || state.message?.startsWith("말소리가 잘") == true) {
                Text(state.message.orEmpty(), color = MaterialTheme.colorScheme.error)
            }
            if (state.busy && state.model == "준비됨") TextButton(onClick = model::cancel) { Text("받아 적기 취소") }
            if (model.recordingFile.isFile) OutlinedButton(onClick = {
                scope.launch { PcmPlayer.play(model.recordingFile) }
            }) { Text("내 목소리 듣기") }
            state.result?.let { result ->
                Text("받아 적은 말: $result")
                state.matchRate?.let { Text("일치율 ${"%.1f".format(it * 100)}%") }
                if (state.matchRate != null) Text(
                    "더 말한 단어 ${state.diff.count { it is WordDiff.Insert }}개 · " +
                        "빠뜨린 단어 ${state.diff.count { it is WordDiff.Delete }}개 · " +
                        "틀린 단어 ${state.diff.count { it is WordDiff.Substitute }}개"
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.diff.forEach { item -> when (item) {
                        is WordDiff.Match -> Text(item.word)
                        is WordDiff.Substitute -> Text("${item.ref} → ${item.hyp}", color = Color.Red)
                        is WordDiff.Delete -> Text(item.ref, color = Color.Gray, textDecoration = TextDecoration.LineThrough)
                        is WordDiff.Insert -> Text(item.hyp, color = Color(0xFFBA6500))
                    } }
                }
                if (state.sentence.isNotBlank()) {
                    val unclear = state.diff.mapNotNull { item -> when (item) {
                        is WordDiff.Substitute -> UnclearHint("${item.ref} → ${item.hyp}", item.ref)
                        is WordDiff.Delete -> UnclearHint("${item.ref} (빠뜨림)", item.ref)
                        else -> null
                    } }.distinctBy { it.word }.take(5)
                    PronunciationHintsCard(unclear, "다르게 들린 단어", pronunciationTips(werWords(state.sentence)),
                        linkingPairs(state.sentence), reductions(state.sentence), flapWords(state.sentence), speak)
                }
            }
        }
    }
}
