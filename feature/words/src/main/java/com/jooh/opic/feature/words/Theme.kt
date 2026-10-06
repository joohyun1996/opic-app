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

internal val Ink = Color(0xFF1A1A18)
internal val Paper = Color(0xFFF8F7F4)
internal val Line = Color(0xFFE8E6E0)
internal val New = Color(0xFFF1EFE8)
internal val Learned = Color(0xFFEAF3DE)
internal val Learning = Color(0xFFFAEEDA)
internal val WrongBg = Color(0xFFFCEBEB)
internal val WrongInk = Color(0xFF9B2626)
internal val RightInk = Color(0xFF2E6B1F)

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
