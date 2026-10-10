package com.jooh.opic.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.PronunciationTip
import com.jooh.opic.core.common.RHYTHM_TIP

/** 불명확하게 들린 단어 하나. [label]은 화면 표시("world" 또는 "walk → work"), [word]는 TTS로 들려줄 원어민 단어. */
data class UnclearHint(val label: String, val word: String, val playMine: (() -> Unit)? = null)

/** 발음 힌트 카드 (TASK 19): A 불명확 단어, B 한국인 발음 팁, C 연음·약화·t 약화 + 리듬. 단어를 누르면 [onSpeak]로 원어민 발음. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PronunciationHintsCard(
    unclear: List<UnclearHint>,
    unclearTitle: String,
    tips: List<PronunciationTip>,
    linking: List<Pair<String, String>>,
    reductions: List<Pair<String, String>>,
    flaps: List<String>,
    onSpeak: (String) -> Unit,
) {
    var open by remember { mutableStateOf(true) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Text("발음 힌트", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { open = !open }) { Text(if (open) "접기" else "펼치기") }
            }
            if (!open) return@Column
            Text("단어를 누르면 원어민 발음을 들려줘요", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            if (unclear.isNotEmpty()) {
                Text(unclearTitle, style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    unclear.forEach { hint ->
                        AssistChip(onClick = { onSpeak(hint.word) }, label = { Text("♪ ${hint.label}") })
                        hint.playMine?.let { AssistChip(onClick = it, label = { Text("내 발음") }) }
                    }
                }
            }
            tips.forEach { tip ->
                HorizontalDivider()
                Text(tip.rule.title, style = MaterialTheme.typography.titleSmall)
                Text(tip.rule.explanation, style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tip.examples.forEach { word -> AssistChip(onClick = { onSpeak(word) }, label = { Text("♪ $word") }) }
                }
            }
            if (linking.isNotEmpty() || reductions.isNotEmpty() || flaps.isNotEmpty()) {
                HorizontalDivider()
                Text("이어 읽기 · 약하게 읽기", style = MaterialTheme.typography.titleSmall)
                if (linking.isNotEmpty()) {
                    Text("앞 단어 끝 자음을 다음 모음에 붙여 읽어요", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        linking.forEach { (a, b) -> AssistChip(onClick = { onSpeak("$a $b") }, label = { Text("♪ $a‿$b") }) }
                    }
                }
                reductions.forEach { (phrase, sound) ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("$phrase → 말할 때는 '$sound'처럼", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { onSpeak(phrase) }) { Text("♪") }
                    }
                }
                if (flaps.isNotEmpty()) {
                    Text("모음 사이 t는 'ㄹ'처럼 부드럽게 (water → 워러)", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        flaps.forEach { word -> AssistChip(onClick = { onSpeak(word) }, label = { Text("♪ $word") }) }
                    }
                }
            }
            HorizontalDivider()
            Text("리듬: $RHYTHM_TIP", style = MaterialTheme.typography.bodySmall)
        }
    }
}
