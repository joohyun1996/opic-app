package com.jooh.opic

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.jooh.opic.core.database.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jooh.opic.core.llm.ModelCatalog
import com.jooh.opic.feature.grammar.GrammarFlow
import com.jooh.opic.feature.shadowing.ShadowingScreen
import com.jooh.opic.feature.shadowing.ShadowingViewModel
import com.jooh.opic.feature.speaking.SpeakingScreen
import com.jooh.opic.feature.speaking.SpeakingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.jooh.opic.feature.grammar.dueExercises
import com.jooh.opic.feature.grammar.GrammarLoadResult
import com.jooh.opic.feature.words.Speaker
import com.jooh.opic.feature.words.WordsApp
import com.jooh.opic.feature.words.WordsPage
import com.jooh.opic.feature.words.WordsTheme

@Composable
fun OpicRoot(app: OpicApplication) {
    val importResult by app.importResult.collectAsState()
    val grammarResult by app.grammarResult.collectAsState()
    val grammarCount = (grammarResult as? GrammarLoadResult.Loaded)?.book?.units?.size
    val nav = rememberNavController()
    val speaker = remember(app) { Speaker(app) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    val ttsAvailable by speaker.available.collectAsState()
    var ttsNoticeShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ttsAvailable) {
        if (ttsAvailable == false && !ttsNoticeShown) {
            ttsNoticeShown = true
            Toast.makeText(app, "기기 설정에서 영어 음성을 설치하세요", Toast.LENGTH_LONG).show()
        }
    }
    val navigate: (String) -> Unit = { nav.navigate(it) }
    val back: () -> Unit = { nav.safeBack() }
    WordsTheme {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                // 홈에 들어올 때마다 오늘의 복습 수를 다시 센다
                var grammarDue by remember { mutableStateOf(0) }
                LaunchedEffect(grammarResult) {
                    val book = (grammarResult as? GrammarLoadResult.Loaded)?.book ?: return@LaunchedEffect
                    grammarDue = runCatching { app.grammarReviews.dueExercises(book).size }.getOrDefault(0)
                }
                var backupOpen by remember { mutableStateOf(false) }
                WordsApp(app.database, importResult, speaker, WordsPage.HOME, grammarCount,
                    grammarDue = grammarDue, navigate = navigate, onBack = back, onBackup = { backupOpen = true })
                if (backupOpen) BackupDialog(app) { backupOpen = false }
            }
            composable("days") { WordsApp(app.database, importResult, speaker, WordsPage.DAYS, grammarCount,
                navigate = navigate, onBack = back) }
            composable("day/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                WordsApp(app.database, importResult, speaker, WordsPage.DAY, grammarCount,
                    day = entry.arguments?.getInt("day") ?: 1, navigate = navigate, onBack = back)
            }
            composable("study/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                WordsApp(app.database, importResult, speaker, WordsPage.STUDY, grammarCount,
                    day = entry.arguments?.getInt("day") ?: 1, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
            }
            composable("wrong") { WordsApp(app.database, importResult, speaker, WordsPage.WRONG, grammarCount,
                navigate = navigate, onBack = back) }
            composable("study/wrong/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                WordsApp(app.database, importResult, speaker, WordsPage.WRONG_STUDY, grammarCount,
                    day = entry.arguments?.getInt("day") ?: 0, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
            }
            composable("grammar") {
                LaunchedEffect(Unit) { app.whisper.close() }
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
                    Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 16.dp)) {
                        GrammarFlow(grammarResult, app.llmEngine, app.hfTokenStore, { ModelCatalog.isDownloaded(app) },
                            ModelCatalog.config(app).models.first().expectedBytes, back, reviews = app.grammarReviews)
                    }
                }
            }
            composable("shadowing") {
                val model: ShadowingViewModel = viewModel(factory = remember(app) { object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                        ShadowingViewModel(app, app.whisper, app::releaseGemmaBeforeWhisper, app.database.shadowingAttemptDao()) as T
                } })
                Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
                    Box(Modifier.widthIn(max = 430.dp).fillMaxSize()) {
                        ShadowingScreen(model, back, speaker::speak, app.shadowingVideos)
                    }
                }
            }
            composable("speaking") {
                val load by app.speakingCatalog.collectAsState()
                // 파싱이 끝나기 전에 들어오면 ViewModel을 만들지 않고 기다린다
                load?.let { loaded ->
                    val catalog = loaded.catalog
                    val model: SpeakingViewModel = viewModel(factory = remember(app, loaded) { object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            SpeakingViewModel(app, catalog, app.whisper, app::releaseGemmaBeforeWhisper, speaker::speak, speaker::stop,
                                { app.llmEngine }, app.hfTokenStore, { ModelCatalog.isDownloaded(app) },
                                ModelCatalog.config(app).models.first().expectedBytes, app.database.speakingDao()) as T
                    } })
                    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
                        Box(Modifier.widthIn(max = 430.dp).fillMaxSize()) { SpeakingScreen(model, back) }
                    }
                }
            }
        }
    }
}

/** 학습 기록 백업·복원 (TASK 22). 파일 위치는 시스템 선택 창으로 사용자가 고른다 (새 권한 없음). */
@Composable
private fun BackupDialog(app: OpicApplication, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    val manager = remember(app) { BackupManager(app.database) }
    val create = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                val raw = manager.export(System.currentTimeMillis())
                withContext(Dispatchers.IO) { app.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(raw.toByteArray()) } }
                "백업 파일을 만들었어요 (${raw.length / 1024}KB)"
            }.getOrElse { "백업 실패: ${it.message}" }
        }
    }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            status = runCatching {
                val raw = withContext(Dispatchers.IO) { app.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                val r = manager.restore(raw) ?: return@runCatching "백업 파일 형식이 아니에요. 아무것도 바꾸지 않았어요"
                "복원했어요 — 단어 ${r.userWords}개, 문법 복습 ${r.grammarReviews}개, 스피킹 답변 ${r.speakingAnswers}개, 섀도잉 ${r.shadowingAttempts}개" +
                    if (r.skippedWords > 0) " (없는 단어 ${r.skippedWords}개 건너뜀)" else ""
            }.getOrElse { "복원 실패: ${it.message}" }
        }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("학습 기록 백업·복원") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("단어 학습 기록, 문법 복습, 스피킹 답변, 섀도잉 연습을 파일 하나로 저장해요. 녹음 파일은 포함하지 않아요.")
            Text("복원은 기존 기록과 합쳐요: 더 최근 기록을 남기고, 같은 답변은 두 번 들어가지 않아요.")
            Button(onClick = { create.launch("opic-backup-${java.time.LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) { Text("백업 파일 만들기") }
            OutlinedButton(onClick = { open.launch(arrayOf("application/json", "application/octet-stream", "*/*")) }, modifier = Modifier.fillMaxWidth()) { Text("백업에서 복원") }
            status?.let { Text(it) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
}

private fun NavController.safeBack() {
    val current = currentBackStackEntry ?: return
    if (previousBackStackEntry != null && current.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}
