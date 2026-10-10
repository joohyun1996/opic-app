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
import com.jooh.opic.feature.analysis.StatsData
import com.jooh.opic.feature.analysis.StatsScreen
import com.jooh.opic.feature.analysis.loadStats
import com.jooh.opic.core.common.StudyLanguages
import com.jooh.opic.feature.grammar.GrammarExplanationScreen
import com.jooh.opic.core.correction.GrammarLink
import com.jooh.opic.core.correction.LocalGrammarLink
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
import android.app.Activity
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import com.jooh.opic.core.llm.SharedModel
import com.jooh.opic.core.stt.SttModels
import com.jooh.opic.core.ui.Opic
import com.jooh.opic.core.ui.OpicTheme
import com.jooh.opic.core.ui.ThemeMode
import com.jooh.opic.core.ui.UiSettings
import com.jooh.opic.feature.words.WordsViewModel

/** 하단 바 탭 (TASK 24). route = 탭 그래프, start = 탭 첫 화면. */
private enum class Tab(val route: String, val start: String, val label: String, val icon: ImageVector) {
    HOME("tab/home", "home", "홈", Icons.Home),
    WORDS("tab/words", "days", "단어", Icons.Cards),
    GRAMMAR("tab/grammar", "grammar", "문법", Icons.Book),
    SHADOWING("tab/shadowing", "shadowing", "섀도잉", Icons.Play),
    SPEAKING("tab/speaking", "speaking", "스피킹", Icons.Mic),
}

@Composable
fun OpicRoot(app: OpicApplication) {
    val importResult by app.importResult.collectAsState()
    val grammarResult by app.grammarResult.collectAsState()
    val grammarCount = (grammarResult as? GrammarLoadResult.Loaded)?.book?.units?.size
    val nav = rememberNavController()
    val speaker = remember(app) { Speaker(app) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    val ttsAvailable by speaker.available.collectAsState()
    val themeMode by UiSettings.themeMode.collectAsState()
    val speechRate by UiSettings.speechRate.collectAsState()
    LaunchedEffect(ttsAvailable, speechRate) { if (ttsAvailable == true) speaker.setRate(speechRate) }
    var ttsNoticeShown by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ttsAvailable) {
        if (ttsAvailable == false && !ttsNoticeShown) {
            ttsNoticeShown = true
            Toast.makeText(app, "기기 설정에서 영어 음성을 설치하세요", Toast.LENGTH_LONG).show()
        }
    }
    val navigate: (String) -> Unit = { nav.navigate(it) }
    val back: () -> Unit = { nav.safeBack() }
    val dark = when (themeMode) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
        }
    }
    fun openTab(tab: Tab) = nav.navigate(tab.route) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    val openMenu: () -> Unit = { nav.navigate("menu") { launchSingleTop = true } }
    var backupOpen by remember { mutableStateOf(false) }
    var modelsOpen by remember { mutableStateOf(false) }
    OpicTheme(themeMode) {
        val entry by nav.currentBackStackEntryAsState()
        val destination = entry?.destination
        Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
            if (destination?.route != "menu" && destination?.route != "stats") NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                Tab.entries.forEach { tab ->
                    val selected = destination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(selected = selected, onClick = { openTab(tab) }, icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.label) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Opic.colors.accent,
                            selectedTextColor = Opic.colors.accent, indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant))
                }
            }
        }) { padding ->
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                // 교정 카드 → 실전 영문법 장 설명 (TASK 29). 뒤로 가면 원래 화면으로 돌아온다
                val coreUnits = (grammarResult as? GrammarLoadResult.Loaded)?.book?.units.orEmpty().filter { it.track == "core" }.associateBy { it.id }
                val grammarLink = remember(coreUnits) { GrammarLink(title = { id -> coreUnits[id]?.let { "${it.order}장 ${it.title.substringBefore(" — ")}" } },
                    open = { id -> nav.navigate("grammarUnit/$id") }) }
                CompositionLocalProvider(LocalGrammarLink provides grammarLink) {
                NavHost(navController = nav, startDestination = Tab.HOME.route) {
                    composable("grammarUnit/{id}") { entry ->
                        val id = entry.arguments?.getString("id")
                        coreUnits[id]?.let { unit ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    GrammarExplanationScreen(unit, back, onStart = { pendingGrammarUnit.value = unit.id; openTab(Tab.GRAMMAR) }, backLabel = "← 돌아가기")
                                }
                            }
                        } ?: Text("문법 장을 찾지 못했습니다")
                    }
                    navigation(startDestination = "home", route = Tab.HOME.route) {
                        composable("home") {
                            // 홈에 들어올 때마다 실제 데이터를 다시 센다
                            val words: WordsViewModel = viewModel(factory = remember(app) { WordsViewModel.Factory(app.database) })
                            val wordState by words.state.collectAsState()
                            var grammarDue by remember { mutableStateOf(0) }
                            var recentVideo by remember { mutableStateOf<String?>(null) }
                            LaunchedEffect(importResult) { words.refresh() }
                            LaunchedEffect(grammarResult) {
                                val book = (grammarResult as? GrammarLoadResult.Loaded)?.book ?: return@LaunchedEffect
                                grammarDue = runCatching { app.grammarReviews.dueExercises(book).size }.getOrDefault(0)
                            }
                            LaunchedEffect(Unit) {
                                val id = runCatching { app.database.shadowingAttemptDao().recentVideos("en", 1).firstOrNull()?.videoId }.getOrNull()
                                recentVideo = app.shadowingVideos.firstOrNull { it.id == id }?.title
                            }
                            val next = wordState.days.firstOrNull { it.total > 0 && it.mastered < it.total }
                            HomeScreen(HomeData(grammarDue, next?.day, next?.mastered ?: 0, next?.total ?: 0, wordState.mastered, wordState.total, recentVideo),
                                onMenu = openMenu, onGrammarReview = { pendingGrammarReview.value = true; openTab(Tab.GRAMMAR) }, onWords = { openTab(Tab.WORDS) },
                                onDay = { day -> openTab(Tab.WORDS); nav.navigate("day/$day") }, onMock = { openTab(Tab.SPEAKING) },
                                onShadowing = { openTab(Tab.SHADOWING) })
                        }
                    }
                    navigation(startDestination = "days", route = Tab.WORDS.route) {
                        composable("days") { TabRoot(openMenu) { WordsApp(app.database, importResult, speaker, WordsPage.DAYS, grammarCount, navigate = navigate, onBack = back) } }
                        composable("day/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            WordsApp(app.database, importResult, speaker, WordsPage.DAY, grammarCount,
                                day = entry.arguments?.getInt("day") ?: 1, navigate = navigate, onBack = back)
                        }
                        composable("study/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            WordsApp(app.database, importResult, speaker, WordsPage.STUDY, grammarCount,
                                day = entry.arguments?.getInt("day") ?: 1, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
                        }
                        composable("wrong") { WordsApp(app.database, importResult, speaker, WordsPage.WRONG, grammarCount, navigate = navigate, onBack = back) }
                        composable("study/wrong/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            WordsApp(app.database, importResult, speaker, WordsPage.WRONG_STUDY, grammarCount,
                                day = entry.arguments?.getInt("day") ?: 0, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
                        }
                    }
                    navigation(startDestination = "grammar", route = Tab.GRAMMAR.route) {
                        composable("grammar") {
                            val pendingGrammar by pendingGrammarUnit.collectAsState()
                            val pendingReview by pendingGrammarReview.collectAsState()
                            LaunchedEffect(Unit) { app.whisper.close() }
                            TabRoot(openMenu) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 16.dp)) {
                                        GrammarFlow(grammarResult, app.llmEngine, app.hfTokenStore, { ModelCatalog.isDownloaded(app) },
                                            ModelCatalog.config(app).models.first().expectedBytes, back, reviews = app.grammarReviews,
                                            openUnitId = pendingGrammar, openReview = pendingReview == true,
                                            onOpened = { pendingGrammarUnit.value = null; pendingGrammarReview.value = null })
                                    }
                                }
                            }
                        }
                    }
                    navigation(startDestination = "shadowing", route = Tab.SHADOWING.route) {
                        composable("shadowing") {
                            val model: ShadowingViewModel = viewModel(factory = remember(app) { object : ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                                    ShadowingViewModel(app, app.whisper, app::releaseGemmaBeforeWhisper, app.database.shadowingAttemptDao()) as T
                            } })
                            TabRoot(openMenu) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    Box(Modifier.widthIn(max = 430.dp).fillMaxSize()) { ShadowingScreen(model, back, speaker::speak, app.shadowingVideos) }
                                }
                            }
                        }
                    }
                    navigation(startDestination = "speaking", route = Tab.SPEAKING.route) {
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
                                            ModelCatalog.config(app).models.first().expectedBytes, app.database.speakingDao(), loaded.templates) as T
                                } })
                                LaunchedEffect(Unit) { pendingSpeakingHistory.value?.let { pendingSpeakingHistory.value = null; model.openHistory() } }
                                TabRoot(openMenu) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                        Box(Modifier.widthIn(max = 430.dp).fillMaxSize()) { SpeakingScreen(model, back) }
                                    }
                                }
                            }
                        }
                    }
                    composable("stats") {
                        val stats by produceState<StatsData?>(null) {
                            value = runCatching { loadStats(app.database, StudyLanguages.EN.code, java.time.LocalDate.now().toEpochDay()) }.getOrNull()
                        }
                        val titles = (grammarResult as? GrammarLoadResult.Loaded)?.book?.units.orEmpty().associate { u ->
                            u.id to if (u.track == "core") "실전 ${u.order}장 ${u.title.substringBefore(" — ")}" else "OPIc ${u.order}단원 ${u.title}" }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = 430.dp)) { StatsScreen(stats, { titles[it] ?: it }, back) }
                        }
                    }
                    composable("menu") {
                        val words: WordsViewModel = viewModel(factory = remember(app) { WordsViewModel.Factory(app.database) })
                        val wordState by words.state.collectAsState()
                        LaunchedEffect(Unit) { words.refresh() }
                        MenuScreen(
                            wrongCount = wordState.wrong,
                            gemmaStatus = gemmaStatus(app), whisperStatus = if (SttModels.isDownloaded(app.whisper.model)) "받음" else "없음",
                            themeMode = themeMode, speechRate = speechRate, version = BuildConfig.VERSION_NAME,
                            onClose = back, onWrong = { nav.navigate("wrong") }, onStats = { nav.navigate("stats") },
                            onSpeakingHistory = { pendingSpeakingHistory.value = true; openTab(Tab.SPEAKING) },
                            onModels = { modelsOpen = true },
                            onTheme = { UiSettings.setTheme(app, it) }, onSpeechRate = { UiSettings.setSpeechRate(app, it) },
                            onBackup = { backupOpen = true },
                        )
                    }
                }
                }
            }
        }
        if (backupOpen) BackupDialog(app) { backupOpen = false }
        if (modelsOpen) ModelsDialog(app) { modelsOpen = false }
    }
}

/** 메뉴 "스피킹 기록" → 스피킹 탭을 열면서 기록 화면으로. */
private val pendingGrammarReview = kotlinx.coroutines.flow.MutableStateFlow<Boolean?>(null)
private val pendingGrammarUnit = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
private val pendingSpeakingHistory = kotlinx.coroutines.flow.MutableStateFlow<Boolean?>(null)

/** 탭 첫 화면 오른쪽 위에 ≡ 전체 메뉴 버튼을 얹는다. */
@Composable
private fun TabRoot(onMenu: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        content()
        IconButton(onClick = onMenu, modifier = Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).size(48.dp)) {
            Icon(Icons.Menu, contentDescription = "전체 메뉴")
        }
    }
}

private fun gemmaStatus(app: OpicApplication): String {
    val bytes = ModelCatalog.config(app).models.first().expectedBytes
    return when {
        SharedModel.isValid(bytes) -> "공용 모델"
        ModelCatalog.isDownloaded(app) -> "앱 안"
        else -> "없음"
    }
}

/** AI 모델 상태 (TASK 24). 공용 Gemma(ADR 002)는 모든 파일 접근이 필요하다. */
@Composable
private fun ModelsDialog(app: OpicApplication, onDismiss: () -> Unit) {
    val bytes = ModelCatalog.config(app).models.first().expectedBytes
    AlertDialog(onDismissRequest = onDismiss, title = { Text("AI 모델") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("문법 교정 (Gemma 3n E4B, 4.4GB): ${gemmaStatus(app)}")
            Text("공용 위치: 내장 저장공간/${SharedModel.FOLDER_HINT}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!SharedModel.hasAccess()) OutlinedButton(onClick = { app.startActivity(SharedModel.accessSettingsIntent(app).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }) {
                Text("모든 파일 접근 허용 (공용 모델 읽기)")
            } else if (!SharedModel.isValid(bytes)) Text("공용 위치에 모델 파일이 없어요", color = Opic.colors.warning)
            Text("음성 인식 (Whisper small.en, 190MB): " + if (SttModels.isDownloaded(app.whisper.model)) "받음" else "없음 — 섀도잉·스피킹에서 받기")
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
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
