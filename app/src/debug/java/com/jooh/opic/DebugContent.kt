package com.jooh.opic

import androidx.compose.runtime.Composable
import com.jooh.opic.debug.LlmBenchScreen

fun debugContent(app: OpicApplication): (@Composable (() -> Unit) -> Unit)? =
    if (BuildConfig.DEBUG) { onBack -> LlmBenchScreen(app, onBack) } else null
