package com.jooh.opic.feature.shadowing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.Cue

@Composable
fun CaptionList(cues: List<Cue>, status: CaptionStatus, current: Int?, selected: Int?, onSelect: (Int) -> Unit, onRetry: () -> Unit) {
    when (status) {
        CaptionStatus.LOADING -> Text("자막 받는 중…")
        CaptionStatus.NONE, CaptionStatus.FAILED -> Row {
            Text(if (status == CaptionStatus.FAILED) "자막 요청 실패 — 문장을 붙여 넣으세요" else "자막 없음 — 문장을 붙여 넣으세요",
                Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("다시 시도") }
        }
        CaptionStatus.READY -> {
            Text("자막 · ${cues.size}문장")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = selected != null && selected > 0, onClick = { selected?.let { onSelect(it - 1) } }) { Text("이전 문장") }
                OutlinedButton(enabled = selected != null && selected < cues.lastIndex, onClick = { selected?.let { onSelect(it + 1) } }) { Text("다음 문장") }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 240.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(cues) { index, cue ->
                    Surface(onClick = { onSelect(index) }, color = if (index == current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
                        val seconds = cue.startMs / 1000
                        Text("${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}  ${cue.text}", Modifier.padding(10.dp))
                    }
                }
            }
        }
    }
}
