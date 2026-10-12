package com.jooh.opic.feature.words

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.common.StudyLanguage
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.WordEntity

/** 학습 카드의 확인 뒤 분해 조각에서만 이 경로를 연다. */
val LocalAffixNavigator = staticCompositionLocalOf<(String, Long) -> Unit> { { _, _ -> } }

@Composable
fun AffixWordsScreen(database: OpicDatabase, language: StudyLanguage, kind: String, id: Long, onBack: () -> Unit) {
    var words by remember(language, kind, id) { mutableStateOf<List<WordEntity>?>(null) }
    var title by remember(language, kind, id) { mutableStateOf("") }
    LaunchedEffect(language, kind, id) {
        val dao = database.affixDao()
        if (kind == "affix") {
            title = dao.affixById(language.code, id)?.form.orEmpty()
            words = dao.wordsWithAffix(language.code, id)
        } else {
            title = dao.baseById(language.code, id)?.word.orEmpty()
            words = dao.wordsWithBase(language.code, id)
        }
    }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← 돌아가기") }
        Text("$title 관련 단어", style = MaterialTheme.typography.headlineSmall)
        val list = words
        if (list == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        else LazyColumn {
            items(list, key = { it.id }) { word ->
                Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(word.word, style = MaterialTheme.typography.titleMedium)
                    Text(word.meaningKo.removePrefix("*"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }
    }
}
