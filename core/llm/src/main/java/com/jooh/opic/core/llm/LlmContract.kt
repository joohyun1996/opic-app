package com.jooh.opic.core.llm

import java.io.File
import kotlinx.coroutines.flow.StateFlow

/** Ordered candidates: first usable model wins. Paths and download locations belong to callers. */
data class ModelSpec(
    val id: String,
    val file: File,
    val downloadUrl: String?,
    val expectedBytes: Long,
    val sha256: String? = null,
) {
    init {
        require(id.isNotBlank() && expectedBytes > 0)
        require(sha256 == null || sha256.matches(Regex("[a-fA-F0-9]{64}")))
    }
}

data class LlmConfig(val models: List<ModelSpec>, val maxTokens: Int = 1024) {
    init {
        require(models.isNotEmpty() && maxTokens > 0)
        require(models.map { it.id }.distinct().size == models.size)
        require(models.map { it.file.canonicalPath }.distinct().size == models.size)
    }
}

enum class LlmFailureReason {
    NOT_DOWNLOADED, INSUFFICIENT_STORAGE, INSUFFICIENT_MEMORY, DEVICE_UNSUPPORTED, MODEL_CORRUPTED,
    TIMEOUT, NETWORK, AUTH_REQUIRED, MODEL_LOAD_FAILED, INFERENCE_FAILED, BUSY, CLOSED, UNKNOWN
}
class LlmException(val reason: LlmFailureReason, message: String, cause: Throwable? = null) :
    Exception(message, cause)

data class ModelAttempt(val modelId: String, val reason: LlmFailureReason)
sealed interface LlmEngineState {
    data object NotDownloaded : LlmEngineState
    data class Downloading(val modelId: String, val bytes: Long, val total: Long) : LlmEngineState {
        val progress: Float get() = (bytes.toDouble() / total).toFloat().coerceIn(0f, 1f)
    }
    data class Loading(val modelId: String) : LlmEngineState
    data class Ready(val modelId: String, val fallbackAttempts: List<ModelAttempt> = emptyList()) : LlmEngineState
    data class Generating(val modelId: String) : LlmEngineState
    /** draining=true: timed-out/cancelled native work still owns the runtime. Retry after it ends. */
    data class Failed(val reason: LlmFailureReason, val draining: Boolean = false,
                      val attempts: List<ModelAttempt> = emptyList()) : LlmEngineState
    data object Closed : LlmEngineState
}
interface OnDeviceLlmEngine : AutoCloseable {
    val state: StateFlow<LlmEngineState>
    suspend fun ensureModelReady(): Result<Unit>
    /** Requires ensureModelReady first. Does not access the network or format the prompt. */
    suspend fun generate(prompt: String, timeoutMs: Long = 90_000): Result<String>
}

/** Injectable seams: tests need neither Android nor native MediaPipe. */
fun interface ModelStore {
    suspend fun ensure(model: ModelSpec, progress: (Long, Long) -> Unit): File
}
interface InferenceRuntime : AutoCloseable { fun generate(prompt: String): String }
fun interface RuntimeFactory { fun load(file: File): InferenceRuntime }
