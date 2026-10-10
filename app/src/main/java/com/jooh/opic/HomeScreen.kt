package com.jooh.opic

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.ui.Opic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 홈에 보여 줄 실제 데이터 (TASK 24). */
data class HomeData(
    val grammarDue: Int = 0,
    /** 오늘 복습할 단어 수 (간격 복습) */
    val wordDue: Int = 0,
    val nextDay: Int? = null,
    val dayMastered: Int = 0,
    val dayTotal: Int = 0,
    val wordsMastered: Int = 0,
    val wordsTotal: Int = 0,
    val recentVideoTitle: String? = null,
)

private val DATE_FORMAT = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)

@Composable
fun HomeScreen(data: HomeData, onMenu: () -> Unit, onGrammarReview: () -> Unit, onWords: () -> Unit,
               onDay: (Int) -> Unit, onMock: () -> Unit, onShadowing: () -> Unit, onWordReview: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TopBar(LocalDate.now().format(DATE_FORMAT), "오늘의 학습", onMenu)
        // 오늘 할 일
        Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("오늘 할 일", style = MaterialTheme.typography.labelLarge, color = Opic.colors.accent)
                if (data.grammarDue > 0 || data.wordDue > 0) {
                    Text(listOfNotNull(data.wordDue.takeIf { it > 0 }?.let { "단어 ${it}개" }, data.grammarDue.takeIf { it > 0 }?.let { "문법 ${it}문제" })
                        .joinToString(" · ") + " 복습", style = MaterialTheme.typography.titleLarge)
                    Text("전에 틀린 것을 잊기 전에 다시 봐요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (data.wordDue > 0) Button(onClick = onWordReview, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("단어 복습 시작") }
                    if (data.grammarDue > 0) Button(onClick = onGrammarReview, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("문법 복습 시작") }
                } else {
                    Text("오늘 복습 끝!", style = MaterialTheme.typography.titleLarge)
                    Text(data.nextDay?.let { "영단어 Day $it 을 이어서 해 볼까요?" } ?: "영단어를 이어서 해 볼까요?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { data.nextDay?.let(onDay) ?: onWords() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("영단어 이어서") }
                }
            }
        }
        Text("이어서 하기", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        ContinueRow(Icons.Mic, "스피킹 모의고사", "실제 시험 순서 15문항", null, onMock)
        ContinueRow(Icons.Cards, data.nextDay?.let { "영단어 Day $it" } ?: "영단어",
            "습득 ${data.wordsMastered} / ${"%,d".format(data.wordsTotal)}",
            if (data.dayTotal > 0) data.dayMastered.toFloat() / data.dayTotal else 0f) { data.nextDay?.let(onDay) ?: onWords() }
        ContinueRow(Icons.Play, data.recentVideoTitle?.let { "섀도잉 · 최근 영상" } ?: "섀도잉 추천 영상",
            data.recentVideoTitle ?: "학습자용부터 100개", null, onShadowing)
    }
}

/** 탭 첫 화면 공용 머리글: 작은 글씨 + 제목 + ≡ 전체 메뉴. */
@Composable
fun TopBar(caption: String?, title: String, onMenu: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            caption?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text(title, style = MaterialTheme.typography.headlineSmall)
        }
        FilledTonalIconButton(onClick = onMenu, modifier = Modifier.size(48.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Icon(Icons.Menu, contentDescription = "전체 메뉴")
        }
    }
}

@Composable
private fun ContinueRow(icon: ImageVector, title: String, subtitle: String, progress: Float?, onClick: () -> Unit) {
    Card(onClick = onClick, shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(40.dp).background(Opic.colors.surfaceHigh, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Opic.colors.accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                progress?.let {
                    LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        color = Opic.colors.accent, trackColor = Opic.colors.surfaceHigh, drawStopIndicator = {})
                }
            }
        }
    }
}
