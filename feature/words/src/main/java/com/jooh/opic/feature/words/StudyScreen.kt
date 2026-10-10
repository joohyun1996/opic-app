package com.jooh.opic.feature.words

import com.jooh.opic.core.ui.Opic
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jooh.opic.core.common.maskHint
import com.jooh.opic.core.common.StudyLanguage
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.WordEntity

/** ④ 영→한 / ⑤ 한→영 플래시카드와 결과 화면 */
@Composable
internal fun StudyScreen(
    database: OpicDatabase, language: StudyLanguage, source: StudySource, mode: StudyMode, speaker: Speaker,
    onRecorded: () -> Unit, onBack: () -> Unit,
) {
    val model: StudyViewModel = viewModel(key = "study-${language.code}-${source.key}-${mode.route}", factory = StudyViewModel.Factory(database, language, source, mode, onRecorded))
    val state by model.state.collectAsState()
    DisposableEffect(Unit) { onDispose { speaker.stop() } }

    Column(Modifier.fillMaxSize().imePadding()) {
        TextButton(onClick = onBack) { Text(source.backLabel) }
        if (state.saveFailed) Text("기록 저장 실패", color = WrongInk, style = MaterialTheme.typography.bodySmall)
        val word = state.current
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Ink) }
            state.finished -> ResultView(state, onRestart = model::restart, onBack = onBack)
            word != null -> CardView(state, word, mode, speaker, model)
        }
    }
}

@Composable
private fun ColumnScope.CardView(state: StudyState, word: WordEntity, mode: StudyMode, speaker: Speaker, model: StudyViewModel) {
    Text("${mode.title} · ${state.index + 1} / ${state.cards.size}", style = MaterialTheme.typography.labelLarge)
    LinearProgressIndicator(
        progress = { (state.index + 1).toFloat() / state.cards.size },
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), color = Opic.colors.accent, trackColor = Opic.colors.surfaceHigh, drawStopIndicator = {},
    )
    val border = when (state.checked) { true -> BorderStroke(2.dp, RightInk); false -> BorderStroke(2.dp, WrongInk); null -> BorderStroke(1.dp, Line) }
    // 카드는 내용 높이만 차지한다. 키보드가 올라와 공간이 줄면 카드 안에서 스크롤되고, 문제는 항상 보인다.
    Card(Modifier.fillMaxWidth().weight(3f, fill = false), border = border, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (mode) {
                StudyMode.EN_KO -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(word.word, style = MaterialTheme.typography.headlineMedium)
                        SpeakButton(speaker, word.word)
                    }
                    Text(word.phonetic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StudyMode.KO_EN -> {
                    Text(word.meaningKo.removePrefix("*"), style = MaterialTheme.typography.headlineSmall)
                    Text(word.partOfSpeech, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (state.hintShown) Text("힌트: ${maskHint(word.word)}", style = MaterialTheme.typography.titleMedium)
                    else if (state.checked == null) TextButton(onClick = model::showHint, contentPadding = PaddingValues(0.dp)) { Text("힌트 보기") }
                }
            }
            state.checked?.let { correct ->
                HorizontalDivider(color = Line)
                Text(if (correct) "✓ 정답" else "✗ 오답", color = if (correct) RightInk else WrongInk, style = MaterialTheme.typography.titleMedium)
                if (state.corrected) Text("맞음으로 바꿨습니다", color = RightInk, style = MaterialTheme.typography.bodySmall)
                when (mode) {
                    StudyMode.EN_KO -> if (!correct) Text("정답: ${word.meaningKo.removePrefix("*")}")
                    StudyMode.KO_EN -> Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (correct) word.word else "정답: ${word.word}", style = MaterialTheme.typography.titleMedium)
                        SpeakButton(speaker, word.word)
                        Text(word.phonetic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(word.example, style = MaterialTheme.typography.bodyMedium)
                Text(word.exampleKo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.weight(1f))
    val english = mode == StudyMode.KO_EN
    OutlinedTextField(
        value = state.input, onValueChange = model::setInput, enabled = state.checked == null, singleLine = true,
        label = { Text(if (english) "영어 단어" else "한국어 뜻") },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None, autoCorrectEnabled = !english,
            keyboardType = if (english) KeyboardType.Ascii else KeyboardType.Text, imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { if (state.checked == null) model.check() else model.next() }),
    )
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.checked == false && mode == StudyMode.EN_KO && !state.corrected) {
            OutlinedButton(onClick = model::markCorrect, modifier = Modifier.weight(1f)) { Text("맞았어요") }
        }
        if (state.checked == null) {
            Button(onClick = model::check, enabled = state.input.isNotBlank(), modifier = Modifier.weight(1f)) { Text("확인") }
        } else {
            Button(onClick = model::next, modifier = Modifier.weight(1f)) { Text("다음") }
        }
    }
}

@Composable
private fun ResultView(state: StudyState, onRestart: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Text("결과", style = MaterialTheme.typography.headlineSmall)
        Text("${state.correctCount} / ${state.cards.size} 정답", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 12.dp))
        if (state.wrongWords.isNotEmpty()) Text("틀린 단어", style = MaterialTheme.typography.titleMedium)
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(state.wrongWords, key = { it.id }) { word ->
                Column(Modifier.padding(vertical = 6.dp)) {
                    Text(word.word, style = MaterialTheme.typography.titleMedium)
                    Text(word.meaningKo.removePrefix("*"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider(color = Line)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) { Text("다시 하기") }
            Button(onClick = onBack, modifier = Modifier.weight(1f)) { Text("목록으로") }
        }
    }
}
