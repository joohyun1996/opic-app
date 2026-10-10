package com.jooh.opic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jooh.opic.core.stt.SttModels
import com.jooh.opic.core.ui.ThemeMode
import com.jooh.opic.core.ui.UiSettings
import com.jooh.opic.feature.words.WordsViewModel

/** 전체 메뉴 (TASK 24): 학습 · 설정 · 데이터 · 앱 정보. */
@Composable
fun MenuScreen(
    wrongCount: Int, gemmaStatus: String, whisperStatus: String, themeMode: ThemeMode, speechRate: Float, version: String,
    onClose: () -> Unit, onWrong: () -> Unit, onStats: () -> Unit, onSpeakingHistory: () -> Unit, onModels: () -> Unit,
    onTheme: (ThemeMode) -> Unit, onSpeechRate: (Float) -> Unit, onBackup: () -> Unit,
    reminderHour: Int? = null, onReminder: (Int?) -> Unit = {},
) {
    var picker by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) { Icon(Icons.Close, contentDescription = "닫기") }
            Text("전체 메뉴", style = MaterialTheme.typography.titleLarge)
        }
        MenuSection("학습") {
            MenuRow("학습 통계", "연속 학습·추이", onStats, divider = false)
            MenuRow("오답노트", "단어 ${wrongCount}개", onWrong)
            MenuRow("스피킹 기록", "답변·모의고사", onSpeakingHistory)
            MenuRow("중국어 (HSK)", "준비 중", null)
        }
        MenuSection("설정") {
            MenuRow("AI 모델", "Gemma $gemmaStatus · Whisper $whisperStatus", onModels, divider = false)
            MenuRow("발음 듣기 (TTS) 속도", SPEECH_RATES.first { it.second == speechRate }.first, onClick = { picker = "rate" })
            MenuRow("화면 테마", themeMode.label, onClick = { picker = "theme" })
            MenuRow("학습 알림", reminderHour?.let { "매일 %02d:00".format(it) } ?: "끔", onClick = { picker = "reminder" })
        }
        MenuSection("데이터") {
            MenuRow("학습 기록 백업·복원", "단어·문법·스피킹·섀도잉", onBackup, divider = false)
        }
        Text("앱 정보 · v$version", Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    when (picker) {
        "theme" -> ChoiceDialog("화면 테마", ThemeMode.entries.map { it.label to it }, themeMode, { onTheme(it); picker = null }) { picker = null }
        "reminder" -> ChoiceDialog("학습 알림 (오늘 공부했으면 안 울려요)", listOf<Pair<String, Int?>>("끔" to null) + ReminderSettings.HOURS.map { "매일 %02d:00".format(it) to it },
            reminderHour, { onReminder(it); picker = null }) { picker = null }
        "rate" -> ChoiceDialog("발음 듣기 속도", SPEECH_RATES, speechRate, { onSpeechRate(it); picker = null }) { picker = null }
    }
}

private val SPEECH_RATES = listOf("느리게" to 0.8f, "보통" to 1.0f, "빠르게" to 1.2f)

@Composable
private fun MenuSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column { content() }
        }
    }
}

@Composable
private fun MenuRow(title: String, value: String, onClick: (() -> Unit)?, divider: Boolean = true) {
    val modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
    if (divider) HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
    Surface(onClick = { onClick?.invoke() }, enabled = onClick != null, color = MaterialTheme.colorScheme.surfaceVariant, modifier = modifier) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun <T> ChoiceDialog(title: String, options: List<Pair<String, T>>, current: T, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column {
            options.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = value == current, onClick = { onPick(value) })
                    TextButton(onClick = { onPick(value) }) { Text(label) }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } })
}

/** 메뉴 화면 연결 (TASK 38: OpicRoot에서 분리). 학습 알림은 켤 때 권한을 묻고, 거부하면 꺼짐으로 둔다 (TASK 34). */
@Composable
internal fun MenuRoute(app: OpicApplication, themeMode: ThemeMode, speechRate: Float, onClose: () -> Unit, navigate: (String) -> Unit,
    onSpeakingHistory: () -> Unit, onModels: () -> Unit, onBackup: () -> Unit) {
                    var reminderHour by remember { mutableStateOf(ReminderSettings.hour(app)) }
            var pendingHour by remember { mutableStateOf<Int?>(null) }
            val permission = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { granted ->
                val hour = pendingHour.takeIf { granted }
                ReminderSettings.set(app, hour); reminderHour = hour
                if (!granted) Toast.makeText(app, "알림 권한이 없어 알림을 켜지 않았어요", Toast.LENGTH_LONG).show()
            }
            val onReminder: (Int?) -> Unit = { hour ->
                if (hour == null || ReminderSettings.canNotify(app)) { ReminderSettings.set(app, hour); reminderHour = hour }
                else { pendingHour = hour; permission.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
            }
            val words: WordsViewModel = viewModel(factory = remember(app) { WordsViewModel.Factory(app.database, app.currentLanguage) })
            val wordState by words.state.collectAsState()
            LaunchedEffect(Unit) { words.refresh() }
            MenuScreen(
                wrongCount = wordState.wrong,
                gemmaStatus = gemmaStatus(app), whisperStatus = if (SttModels.isDownloaded(app.whisper.model)) "받음" else "없음",
                themeMode = themeMode, speechRate = speechRate, version = BuildConfig.VERSION_NAME,
                onClose = onClose, onWrong = { navigate("wrong") }, onStats = { navigate("stats") },
                reminderHour = reminderHour, onReminder = onReminder,
                onSpeakingHistory = onSpeakingHistory,
                onModels = onModels,
                onTheme = { UiSettings.setTheme(app, it) }, onSpeechRate = { UiSettings.setSpeechRate(app, it) },
                onBackup = onBackup,
            )
}
