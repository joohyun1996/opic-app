package com.jooh.opic.feature.analysis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.DailyCount
import com.jooh.opic.core.ui.Opic
import java.time.LocalDate

/** 학습 통계 (TASK 30). [unitTitle]은 문법 장 id → 화면에 보일 이름. */
@Composable
fun StatsScreen(data: StatsData?, unitTitle: (String) -> String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 닫기") }
            Text("학습 통계", style = MaterialTheme.typography.titleLarge)
        }
        if (data == null) { Text("불러오는 중…"); return@Column }
        StatCard("이번 주") {
            Text("연속 학습 ${data.streak}일", style = MaterialTheme.typography.headlineSmall, color = Opic.colors.accent)
            WeekBars(data.week)
        }
        StatCard("단어") {
            Text("습득 ${data.wordsMastered} / ${data.wordsTotal}")
            LinearProgressIndicator(progress = { if (data.wordsTotal == 0) 0f else data.wordsMastered / data.wordsTotal.toFloat() },
                Modifier.fillMaxWidth(), color = Opic.colors.accent, trackColor = Opic.colors.surfaceHigh)
            Ranking("자주 틀리는 단어", data.hardWords.map { it.first to "${it.second}회" })
        }
        StatCard("문법") {
            Text("복습 대기 ${data.grammarDue}문제")
            Ranking("자주 틀리는 장", data.grammarWrong.map { unitTitle(it.first) to "${it.second}회" })
        }
        StatCard("스피킹") {
            if (data.speakingCount == 0) Empty() else {
                Text("답변 ${data.speakingCount}개 · 최근 ${data.recentWpm.size}개 평균 ${data.recentWpm.average().toInt()} WPM · 필러 평균 ${"%.1f".format(data.recentFillers.average())}")
                Text("말하기 속도(WPM) 추이", style = MaterialTheme.typography.labelMedium, color = Opic.colors.muted)
                LineChart(data.recentWpm)
            }
        }
        StatCard("섀도잉") {
            if (data.shadowingCount == 0) Empty() else Text("연습 ${data.shadowingCount}회 · 평균 일치율 ${((data.shadowingAvg ?: 0.0) * 100).toInt()}%")
        }
    }
}

@Composable private fun Empty() = Text("아직 기록이 없어요", color = Opic.colors.muted)

@Composable
private fun StatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun Ranking(title: String, rows: List<Pair<String, String>>) {
    Text(title, style = MaterialTheme.typography.labelMedium, color = Opic.colors.muted)
    if (rows.isEmpty()) Empty()
    rows.forEachIndexed { i, (name, value) ->
        Row { Text("${i + 1}. $name", Modifier.weight(1f)); Text(value, color = Opic.colors.error) }
    }
}

@Composable
private fun WeekBars(week: List<DailyCount>) {
    val max = (week.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    val bar = Opic.colors.accent; val track = Opic.colors.surfaceHigh
    Canvas(Modifier.fillMaxWidth().height(96.dp)) {
        val slot = size.width / week.size; val w = slot * 0.5f
        week.forEachIndexed { i, d ->
            val x = slot * i + (slot - w) / 2
            drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(8f))
            val h = size.height * d.count / max
            if (h > 0) drawRoundRect(bar, Offset(x, size.height - h), Size(w, h), CornerRadius(8f))
        }
    }
    Row(Modifier.fillMaxWidth()) {
        week.forEach { d ->
            val label = LocalDate.ofEpochDay(d.epochDay).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.KOREAN)
            Text("$label\n${d.count}", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = Opic.colors.muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun LineChart(values: List<Int>) {
    if (values.size < 2) return
    val color = Opic.colors.accent
    val min = values.min(); val span = (values.max() - min).coerceAtLeast(1)
    Canvas(Modifier.fillMaxWidth().height(80.dp)) {
        val step = size.width / (values.size - 1)
        val points = values.mapIndexed { i, v -> Offset(step * i, size.height - size.height * (v - min) / span) }
        points.zipWithNext { a, b -> drawLine(color, a, b, strokeWidth = 5f) }
        points.forEach { drawCircle(color, 7f, it) }
    }
}
