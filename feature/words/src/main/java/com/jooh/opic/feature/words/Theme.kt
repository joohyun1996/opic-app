package com.jooh.opic.feature.words

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jooh.opic.core.model.WordStatus
import com.jooh.opic.core.ui.Opic

// TASK 24: 이름은 그대로 두고 공용 테마(밝게·어둡게) 값을 쓴다
internal val Ink: Color @Composable get() = MaterialTheme.colorScheme.onBackground
internal val Paper: Color @Composable get() = MaterialTheme.colorScheme.background
internal val Line: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
internal val New: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
internal val Learned: Color @Composable get() = Opic.colors.successBg
internal val Learning: Color @Composable get() = Opic.colors.learning
internal val WrongBg: Color @Composable get() = Opic.colors.errorBg
internal val WrongInk: Color @Composable get() = Opic.colors.error
internal val RightInk: Color @Composable get() = Opic.colors.success

@Composable
internal fun StatusBadge(status: WordStatus) {
    val (label, color) = when (status) {
        WordStatus.NEW -> "신규" to New
        WordStatus.LEARNING -> "학습중" to Learning
        WordStatus.MASTERED -> "습득" to Learned
    }
    Surface(color = color, shape = MaterialTheme.shapes.small) {
        Text(label, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun WrongBadge(count: Int) {
    Surface(color = WrongBg, shape = MaterialTheme.shapes.small) {
        Text("오답 $count", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = WrongInk, style = MaterialTheme.typography.labelSmall)
    }
}
