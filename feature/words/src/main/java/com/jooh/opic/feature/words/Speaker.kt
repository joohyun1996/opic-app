package com.jooh.opic.feature.words

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/** 앱 안에서 하나만 만들어 쓰는 영어 TTS. 엔진이나 영어 음성이 없으면 available = false. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {
    private val mutableAvailable = MutableStateFlow<Boolean?>(null)
    val available = mutableAvailable.asStateFlow()
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        mutableAvailable.value = status == TextToSpeech.SUCCESS && try {
            tts.setLanguage(Locale.US) !in setOf(TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED)
        } catch (_: Exception) {
            false
        }
    }

    fun speak(text: String) {
        if (mutableAvailable.value == true) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}

@Composable
internal fun SpeakButton(speaker: Speaker, text: String) {
    val available by speaker.available.collectAsState()
    TextButton(onClick = { speaker.speak(text) }, enabled = available == true) { Text("♪") }
}
