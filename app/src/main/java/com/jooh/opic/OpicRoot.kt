package com.jooh.opic

import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.jooh.opic.feature.grammar.GrammarExplanationScreen
import com.jooh.opic.core.correction.GrammarLink
import com.jooh.opic.core.correction.LocalGrammarLink
import com.jooh.opic.feature.shadowing.ShadowingScreen
import com.jooh.opic.feature.shadowing.ShadowingViewModel
import com.jooh.opic.feature.speaking.SpeakingScreen
import com.jooh.opic.feature.speaking.SpeakingViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jooh.opic.feature.grammar.dueExercises
import com.jooh.opic.feature.grammar.GrammarLoadResult
import com.jooh.opic.feature.grammar.GrammarTracks
import com.jooh.opic.feature.grammar.displayTitle
import com.jooh.opic.feature.words.Speaker
import com.jooh.opic.feature.words.WordsApp
import com.jooh.opic.feature.words.dueReviewWords
import com.jooh.opic.feature.words.WordsPage
import android.app.Activity
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import com.jooh.opic.core.ui.Opic
import com.jooh.opic.core.ui.OpicTheme
import com.jooh.opic.core.ui.ThemeMode
import com.jooh.opic.core.ui.UiSettings
import com.jooh.opic.feature.words.WordsViewModel

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
                val coreUnits = remember(grammarResult) { (grammarResult as? GrammarLoadResult.Loaded)?.book?.units.orEmpty().filter { it.track == GrammarTracks.CORE }.associateBy { it.id } }
                val grammarLink = remember(grammarResult) { GrammarLink(title = { id -> coreUnits[id]?.displayTitle()?.removePrefix("실전 ") },
                    open = { id -> nav.navigate("grammarUnit/$id") }) }
                CompositionLocalProvider(LocalGrammarLink provides grammarLink) {
                NavHost(navController = nav, startDestination = Tab.HOME.route) {
                    composable("grammarUnit/{id}") { entry ->
                        val id = entry.arguments?.getString("id")
                        coreUnits[id]?.let { unit ->
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    GrammarExplanationScreen(unit, back, onStart = { tabRequest.value = TabRequest.GrammarUnit(unit.id); openTab(Tab.GRAMMAR) }, backLabel = "← 돌아가기")
                                }
                            }
                        } ?: Text("문법 장을 찾지 못했습니다")
                    }
                    navigation(startDestination = "home", route = Tab.HOME.route) {
                        composable("home") {
                            // 홈에 들어올 때마다 실제 데이터를 다시 센다
                            val words: WordsViewModel = viewModel(factory = remember(app) { WordsViewModel.Factory(app.database, app.currentLanguage) })
                            val wordState by words.state.collectAsState()
                            var grammarDue by remember { mutableStateOf(0) }
                            var recentVideo by remember { mutableStateOf<String?>(null) }
                            var wordDue by remember { mutableStateOf(0) }
                            // 단어 복습 수: 학습 기록이 바뀌면(Day 통계 갱신) 다시 센다
                            LaunchedEffect(wordState) { wordDue = runCatching { dueReviewWords(app.database, app.currentLanguage).size }.getOrDefault(0) }
                            LaunchedEffect(grammarResult) {
                                val book = (grammarResult as? GrammarLoadResult.Loaded)?.book ?: return@LaunchedEffect
                                grammarDue = runCatching { app.grammarReviews.dueExercises(book).size }.getOrDefault(0)
                            }
                            LaunchedEffect(Unit) {
                                val id = runCatching { app.database.shadowingAttemptDao().recentVideos(app.currentLanguage.code, 1).firstOrNull()?.videoId }.getOrNull()
                                recentVideo = app.shadowingVideos.firstOrNull { it.id == id }?.title
                            }
                            val next = wordState.days.firstOrNull { it.total > 0 && it.mastered < it.total }
                            HomeScreen(HomeData(grammarDue, wordDue, next?.day, next?.mastered ?: 0, next?.total ?: 0, wordState.mastered, wordState.total, recentVideo),
                                onMenu = openMenu, onGrammarReview = { tabRequest.value = TabRequest.GrammarReview; openTab(Tab.GRAMMAR) }, onWords = { openTab(Tab.WORDS) },
                                onDay = { day -> openTab(Tab.WORDS); nav.navigate("day/$day") },
                                onWordReview = { openTab(Tab.WORDS); nav.navigate("study/review/en-ko") }, onMock = { openTab(Tab.SPEAKING) },
                                onShadowing = { openTab(Tab.SHADOWING) })
                        }
                    }
                    navigation(startDestination = "days", route = Tab.WORDS.route) {
                        composable("days") { TabRoot(openMenu) { WordsApp(app.database, app.currentLanguage, importResult, speaker, WordsPage.DAYS, grammarCount, navigate = navigate, onBack = back) } }
                        composable("day/{day}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            WordsApp(app.database, app.currentLanguage, importResult, speaker, WordsPage.DAY, grammarCount,
                                day = entry.arguments?.getInt("day") ?: 1, navigate = navigate, onBack = back)
                        }
                        composable("study/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            WordsApp(app.database, app.currentLanguage, importResult, speaker, WordsPage.STUDY, grammarCount,
                                day = entry.arguments?.getInt("day") ?: 1, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
                        }
                        composable("study/review/{mode}") { entry ->
                            WordsApp(app.database, app.currentLanguage, importResult, speaker, WordsPage.REVIEW_STUDY, grammarCount,
                                mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
                        }
                        composable("wrong") { WordsApp(app.database, app.currentLanguage, importResult, speaker, WordsPage.WRONG, grammarCount, navigate = navigate, onBack = back) }
                        composable("study/wrong/{day}/{mode}", arguments = listOf(navArgument("day") { type = NavType.IntType })) { entry ->
                            WordsApp(app.database, app.currentLanguage, importResult, speaker, WordsPage.WRONG_STUDY, grammarCount,
                                day = entry.arguments?.getInt("day") ?: 0, mode = entry.arguments?.getString("mode"), navigate = navigate, onBack = back)
                        }
                    }
                    navigation(startDestination = "grammar", route = Tab.GRAMMAR.route) {
                        composable("grammar") {
                            val request by tabRequest.collectAsState()
                            LaunchedEffect(Unit) { app.whisper.close() }
                            TabRoot(openMenu) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    Column(Modifier.widthIn(max = 430.dp).fillMaxSize().padding(horizontal = 16.dp)) {
                                        GrammarFlow(grammarResult, app.llmEngine, app.hfTokenStore, { ModelCatalog.isDownloaded(app) },
                                            ModelCatalog.config(app).models.first().expectedBytes, back, reviews = app.grammarReviews,
                                            openUnitId = (request as? TabRequest.GrammarUnit)?.id, openReview = request == TabRequest.GrammarReview,
                                            onOpened = { tabRequest.value = null })
                                    }
                                }
                            }
                        }
                    }
                    navigation(startDestination = "shadowing", route = Tab.SHADOWING.route) {
                        composable("shadowing") {
                            val model: ShadowingViewModel = viewModel(factory = remember(app) { ShadowingViewModel.Factory(app, app.whisper, app::releaseGemmaBeforeWhisper,
                                app.currentLanguage, app.database.shadowingAttemptDao()) })
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
                                val model: SpeakingViewModel = viewModel(factory = remember(app, loaded) { SpeakingViewModel.Factory {
                                    SpeakingViewModel(app, app.currentLanguage, catalog, app.whisper, app::releaseGemmaBeforeWhisper, speaker::speak, speaker::stop,
                                        { app.llmEngine }, app.hfTokenStore, { ModelCatalog.isDownloaded(app) },
                                        ModelCatalog.config(app).models.first().expectedBytes, app.database.speakingDao(), loaded.templates)
                                } })
                                LaunchedEffect(Unit) { if (tabRequest.value == TabRequest.SpeakingHistory) { tabRequest.value = null; model.openHistory() } }
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
                            value = runCatching { loadStats(app.database, app.currentLanguage.code, java.time.LocalDate.now().toEpochDay()) }.getOrNull()
                        }
                        val titles = remember(grammarResult) { (grammarResult as? GrammarLoadResult.Loaded)?.book?.units.orEmpty().associate { u ->
                            u.id to u.displayTitle() }
                        }
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(Modifier.widthIn(max = 430.dp)) { StatsScreen(stats, { titles[it] ?: it }, back) }
                        }
                    }
                    composable("menu") {
                        MenuRoute(app, themeMode, speechRate, onClose = back, navigate = navigate,
                            onSpeakingHistory = { tabRequest.value = TabRequest.SpeakingHistory; openTab(Tab.SPEAKING) },
                            onModels = { modelsOpen = true }, onBackup = { backupOpen = true })
                    }
                }
                }
            }
        }
        if (backupOpen) BackupDialog(app) { backupOpen = false }
        if (modelsOpen) ModelsDialog(app) { modelsOpen = false }
    }
}

/** 다른 화면에서 탭을 열며 남기는 요청 하나 (TASK 38). 받은 탭이 처리한 뒤 null로 비운다. */
internal sealed interface TabRequest {
    data class GrammarUnit(val id: String) : TabRequest
    data object GrammarReview : TabRequest
    data object SpeakingHistory : TabRequest
}
private val tabRequest = kotlinx.coroutines.flow.MutableStateFlow<TabRequest?>(null)
