package com.jooh.opic.core.stt

import android.content.Context
import com.jooh.opic.core.llm.HttpModelStore
import java.util.concurrent.atomic.AtomicBoolean

/** 사용자 기능은 small.en 하나만 사용한다. Application이 인스턴스 하나를 지연 생성한다. */
class UserWhisper(context: Context) {
    val model = SttModels.userModel(context)
    private val store = HttpModelStore()
    private var engine: WhisperEngine? = null
    val ready: Boolean get() = engine != null
    suspend fun prepare(download: Boolean, beforeLoad: () -> Unit, progress: (Long, Long) -> Unit) {
        if (!SttModels.isDownloaded(model)) {
            check(download) { "모델이 없습니다" }
            store.ensure(model, progress)
        }
        beforeLoad()
        engine?.close()
        engine = WhisperEngine.load(model)
    }
    suspend fun transcribe(audio: FloatArray, cancelled: AtomicBoolean): Transcription =
        checkNotNull(engine) { "모델을 먼저 불러오세요" }.transcribe(audio, cancelled = cancelled)
    suspend fun close() { engine?.closeWhenIdle(); engine = null }
}
