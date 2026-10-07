package com.jooh.opic

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jooh.opic.debug.SttBenchScreen

/** debug 빌드 전용 검증 화면 모음 (release에는 null). */
fun debugContent(app: OpicApplication): (@Composable (() -> Unit) -> Unit)? =
    if (BuildConfig.DEBUG) { onBack -> DebugMenu(app, onBack) } else null

@Composable
private fun DebugMenu(app: OpicApplication, onBack: () -> Unit) {
    var page by remember { mutableStateOf<String?>(null) }
    when (page) {
        "stt" -> SttBenchScreen(app) { page = null }
        else -> Column(Modifier.padding(16.dp)) {
            TextButton(onClick = onBack) { Text("← 홈") }
            Button(onClick = { page = "stt" }) { Text("STT 검증 (Whisper)") }
        }
    }
}
