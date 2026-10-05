package com.jooh.opic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jooh.opic.debug.LlmBenchScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var showBench by remember { mutableStateOf(false) }
            if (BuildConfig.DEBUG && showBench) {
                LlmBenchScreen(application as OpicApplication, onBack = { showBench = false })
            } else {
                Column {
                    Text("OPIc 학습")
                    if (BuildConfig.DEBUG) Button(onClick = { showBench = true }) { Text("LLM 검증") }
                }
            }
        }
    }
}
