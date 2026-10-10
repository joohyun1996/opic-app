package com.jooh.opic.feature.analysis

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.RecordKind
import com.jooh.opic.core.database.deleteRecords
import com.jooh.opic.core.ui.Opic
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 목록 한 줄. key는 삭제할 때 쓰는 값 (단어 id, 문제 id, 답변 id, 연습 id). */
data class RecordItem(val key: String, val title: String, val subtitle: String)

/** 화면에 보일 이름을 앱이 넘긴다 (콘텐츠는 앱이 들고 있다). */
class RecordLabels(val unitTitle: (String) -> String, val questionText: (String) -> String, val videoTitle: (String) -> String)

private val DATE = DateTimeFormatter.ofPattern("M/d HH:mm")
private fun date(ms: Long?) = ms?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DATE) } ?: "-"

suspend fun loadRecords(db: OpicDatabase, language: String, kind: RecordKind, labels: RecordLabels): List<RecordItem> = when (kind) {
    RecordKind.WORDS -> db.userWordDao().records(language).map {
        RecordItem(it.id.toString(), "${it.word} · ${it.meaningKo.removePrefix("*")}", "맞음 ${it.correctCount} · 틀림 ${it.wrongCount} · ${date(it.lastStudiedAt)}")
    }
    RecordKind.GRAMMAR -> db.grammarReviewDao().all(language).sortedByDescending { it.lastStudiedAt }.map {
        RecordItem(it.exerciseId, "${labels.unitTitle(it.unitId)} · ${it.exerciseId}",
            "틀림 ${it.wrongCount} · 다음 복습 ${LocalDate.ofEpochDay(it.dueEpochDay).format(DateTimeFormatter.ofPattern("M/d"))} · ${date(it.lastStudiedAt)}")
    }
    RecordKind.SPEAKING -> db.speakingDao().all(language).sortedByDescending { it.createdAt }.map {
        RecordItem(it.id.toString(), labels.questionText(it.questionId),
            "${date(it.createdAt)} · ${it.durationMs / 1000}초 · ${it.editedText.take(40).ifBlank { "(내용 없음)" }}" + if (it.mockId != null) " · 모의고사" else "")
    }
    RecordKind.SHADOWING -> db.shadowingAttemptDao().all(language).sortedByDescending { it.createdAt }.map {
        RecordItem(it.id.toString(), labels.videoTitle(it.videoId), "${date(it.createdAt)} · 일치율 ${(it.matchRate * 100).toInt()}% · ${it.sentence.take(40)}")
    }
}

/** 학습 기록 관리: 종류별 목록에서 골라 지운다. 지우기 전에 확인한다. 단어 데이터 자체는 지우지 않는다. */
@Composable
fun RecordsScreen(db: OpicDatabase, language: String, labels: RecordLabels, onBack: () -> Unit) {
    var kind by remember { mutableStateOf(RecordKind.WORDS) }
    var items by remember { mutableStateOf<List<RecordItem>?>(null) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(kind, reload) {
        items = null; selected = emptySet()
        items = runCatching { loadRecords(db, language, kind, labels) }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← 닫기") }
            Text("학습 기록 관리", style = MaterialTheme.typography.titleLarge)
        }
        TabRow(selectedTabIndex = kind.ordinal) {
            RecordKind.entries.forEach { k -> Tab(selected = k == kind, onClick = { kind = k }, text = { Text(k.label) }) }
        }
        message?.let { Text(it, color = Opic.colors.success) }
        val list = items
        if (list == null) { Text("불러오는 중…"); return@Column }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${list.size}개 · ${selected.size}개 선택", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { selected = if (selected.size == list.size) emptySet() else list.map { it.key }.toSet() }, enabled = list.isNotEmpty()) {
                Text(if (list.isNotEmpty() && selected.size == list.size) "선택 해제" else "전체 선택")
            }
            Button(onClick = { confirm = true }, enabled = selected.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("삭제") }
        }
        if (list.isEmpty()) Text("${kind.label} 기록이 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(list, key = { it.key }) { item ->
                val checked = item.key in selected
                Row(Modifier.fillMaxWidth().clickable { selected = if (checked) selected - item.key else selected + item.key }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + item.key else selected - item.key })
                    Column(Modifier.weight(1f)) {
                        Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("${kind.label} 기록 ${selected.size}개를 지울까요?") },
        text = { Text(buildString {
            append("지운 기록은 되돌릴 수 없어요. 필요하면 먼저 메뉴의 \"학습 기록 백업\"으로 저장하세요.")
            if (kind == RecordKind.WORDS) append("\n단어장의 단어는 그대로 있고, 맞음·틀림 기록만 지워져 새 단어처럼 돌아가요.")
        }) },
        confirmButton = { TextButton(onClick = {
            val keys = selected.toList(); confirm = false
            scope.launch {
                val n = runCatching { deleteRecords(db, language, kind, keys) }.getOrNull()
                message = if (n == null) "지우지 못했어요" else "${kind.label} 기록 ${n}개를 지웠어요"
                reload++
            }
        }) { Text("삭제", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("취소") } })
}
