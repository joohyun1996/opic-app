package com.jooh.opic.core.llm

import android.content.Context
import android.os.Build
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession

fun createOnDeviceLlmEngine(context: Context, config: LlmConfig,
                          store: ModelStore = HttpModelStore()): OnDeviceLlmEngine {
    val appContext = context.applicationContext
    return DefaultLlmEngine(config, store, RuntimeFactory { file ->
        if (!Build.SUPPORTED_ABIS.contains("arm64-v8a"))
            throw LlmException(LlmFailureReason.DEVICE_UNSUPPORTED, "arm64-v8a required")
        val inference = LlmInference.createFromOptions(appContext,
            LlmInference.LlmInferenceOptions.builder().setModelPath(file.absolutePath)
                .setMaxTokens(config.maxTokens).build())
        object : InferenceRuntime {
            override fun generate(prompt: String): String =
                LlmInferenceSession.createFromOptions(inference,
                    LlmInferenceSession.LlmInferenceSessionOptions.builder().build()).use { session ->
                    session.addQueryChunk(prompt)
                    session.generateResponse()
                }
            override fun close() = inference.close()
        }
    })
}
