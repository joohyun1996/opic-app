package com.jooh.opic.core.llm

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Owns native work independently of callers: cancelling an await never frees an active runtime. */
class DefaultLlmEngine(
    private val config: LlmConfig,
    private val store: ModelStore,
    private val factory: RuntimeFactory,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : OnDeviceLlmEngine {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val guard = Any()
    private val mutableState = MutableStateFlow<LlmEngineState>(LlmEngineState.NotDownloaded)
    override val state = mutableState.asStateFlow()
    private var runtime: InferenceRuntime? = null
    private var selected: ModelSpec? = null
    private var busy = false
    private var closed = false
    private var abandoned: LlmFailureReason? = null
    private var attempts = emptyList<ModelAttempt>()

    override suspend fun ensureModelReady(): Result<Unit> = execute(null) {
        if (runtime == null) {
            val failures = mutableListOf<ModelAttempt>()
            for (model in config.models) {
                try {
                    synchronized(guard) {
                        if (closed || abandoned != null) throw CancellationException("Preparation abandoned")
                    }
                    val file = store.ensure(model) { bytes, total ->
                        publish(LlmEngineState.Downloading(model.id, bytes, total))
                    }
                    publish(LlmEngineState.Loading(model.id))
                    runtime = factory.load(file)
                    selected = model
                    attempts = failures.toList()
                    break
                } catch (e: CancellationException) { throw e
                } catch (e: Exception) {
                    failures += ModelAttempt(model.id, classify(e, LlmFailureReason.MODEL_LOAD_FAILED).reason)
                } catch (e: OutOfMemoryError) {
                    failures += ModelAttempt(model.id, LlmFailureReason.INSUFFICIENT_MEMORY)
                } catch (e: UnsatisfiedLinkError) {
                    failures += ModelAttempt(model.id, LlmFailureReason.DEVICE_UNSUPPORTED)
                }
            }
            attempts = failures.toList()
            if (runtime == null) throw LlmException(failures.last().reason, "All model candidates failed")
        }
    }

    override suspend fun generate(prompt: String, timeoutMs: Long): Result<String> {
        require(timeoutMs > 0)
        return execute(timeoutMs) {
            val active = runtime ?: throw LlmException(LlmFailureReason.NOT_DOWNLOADED, "Call ensureModelReady first")
            publish(LlmEngineState.Generating(selected!!.id))
            active.generate(prompt)
        }
    }

    private suspend fun <T> execute(timeoutMs: Long?, block: suspend () -> T): Result<T> {
        val result = CompletableDeferred<Result<T>>()
        synchronized(guard) {
            if (closed) return Result.failure(LlmException(LlmFailureReason.CLOSED, "Engine closed"))
            if (busy) return Result.failure(LlmException(LlmFailureReason.BUSY, "Native work still running"))
            busy = true
            abandoned = null
            scope.launch {
                val outcome = try { Result.success(block()) }
                catch (e: Exception) { Result.failure(classify(e, LlmFailureReason.INFERENCE_FAILED)) }
                catch (e: OutOfMemoryError) { Result.failure(LlmException(LlmFailureReason.INSUFFICIENT_MEMORY, "Not enough memory", e)) }
                catch (e: LinkageError) { Result.failure(LlmException(LlmFailureReason.DEVICE_UNSUPPORTED, "Native library unavailable", e)) }
                val failure = outcome.exceptionOrNull() as? LlmException
                while (true) {
                    val needsCleanup = synchronized(guard) {
                        if ((failure != null || abandoned != null || closed) && runtime != null) {
                            true
                        } else {
                            busy = false
                            mutableState.value = when {
                                closed -> LlmEngineState.Closed
                                abandoned != null -> LlmEngineState.Failed(abandoned!!, attempts = attempts)
                                failure != null -> LlmEngineState.Failed(failure.reason, attempts = attempts)
                                else -> LlmEngineState.Ready(selected!!.id, attempts)
                            }
                            result.complete(outcome)
                            if (closed) scope.cancel()
                            false
                        }
                    }
                    if (!needsCleanup) break
                    // Keep busy while cleaning up, but never hold the UI-facing monitor in JNI.
                    releaseRuntime()
                }
            }
        }
        return try {
            if (timeoutMs == null) result.await() else withTimeout(timeoutMs) { result.await() }
        } catch (e: TimeoutCancellationException) {
            if (!currentCoroutineContext().isActive) {
                abandon(LlmFailureReason.UNKNOWN)
                throw e
            }
            abandon(LlmFailureReason.TIMEOUT)
            Result.failure(LlmException(LlmFailureReason.TIMEOUT, "Inference exceeded ${timeoutMs}ms", e))
        } catch (e: CancellationException) {
            abandon(LlmFailureReason.UNKNOWN)
            throw e
        }
    }
    private fun abandon(reason: LlmFailureReason) = synchronized(guard) {
        if (!closed) {
            abandoned = reason
            mutableState.value = LlmEngineState.Failed(reason, draining = busy, attempts = attempts)
        }
    }
    private fun publish(value: LlmEngineState) = synchronized(guard) {
        if (!closed && abandoned == null) mutableState.value = value
    }
    private fun releaseRuntime() {
        try { runtime?.close() } catch (_: Exception) { /* Preserve original failure. */ }
        runtime = null
        selected = null
    }
    override fun close() = synchronized(guard) {
        if (!closed) {
            closed = true
            mutableState.value = LlmEngineState.Closed
            if (!busy) {
                busy = true
                scope.launch {
                    releaseRuntime()
                    synchronized(guard) { busy = false }
                    scope.cancel()
                }
            }
        }
    }
    private fun classify(e: Exception, fallback: LlmFailureReason) =
        e as? LlmException ?: LlmException(fallback, "LLM operation failed", e)
}
