package com.jooh.opic.core.llm

import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request

/** One shared store per model directory. Only model downloads use HTTP; prompts never enter here. */
class HttpModelStore(
    private val headers: (ModelSpec) -> Map<String, String> = { emptyMap() },
    private val freeBytes: (File) -> Long = { it.usableSpace },
    private val reserveBytes: Long = 128L * 1024 * 1024,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).build(),
) : ModelStore {
    private val mutex = Mutex()
    override suspend fun ensure(model: ModelSpec, progress: (Long, Long) -> Unit): File =
        withContext(Dispatchers.IO) { mutex.withLock {
            val target = model.file
            if (valid(target, model)) return@withLock target
            val url = model.downloadUrl ?: throw LlmException(
                if (target.exists()) LlmFailureReason.MODEL_CORRUPTED else LlmFailureReason.NOT_DOWNLOADED,
                "No valid local model: ${model.id}")
            require(url.startsWith("https://")) { "Model downloads require HTTPS" }
            val parent = target.absoluteFile.parentFile!!
            if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Cannot create model directory")
            if (freeBytes(parent) - reserveBytes < model.expectedBytes)
                throw LlmException(LlmFailureReason.INSUFFICIENT_STORAGE, "Not enough space for ${model.id}")
            val part = File(parent, target.name + ".part")
            val request = Request.Builder().url(url).apply {
                headers(model).forEach { (key, value) -> header(key, value) }
            }.build()
            val call = client.newCall(request)
            // Cancellation closes blocking socket reads too, not just the coroutine's next suspension.
            val watcher = CoroutineScope(currentCoroutineContext()).launch(start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { call.cancel() }
            }
            try {
                call.execute().use { response ->
                    if (response.code == 401 || response.code == 403)
                        throw LlmException(LlmFailureReason.AUTH_REQUIRED, "Accept model license and supply credentials")
                    if (!response.isSuccessful) throw LlmException(LlmFailureReason.NETWORK, "HTTP ${response.code}")
                    val body = response.body ?: throw IOException("Empty response")
                    if (body.contentLength() >= 0 && body.contentLength() != model.expectedBytes)
                        throw LlmException(LlmFailureReason.MODEL_CORRUPTED, "Unexpected content length")
                    var bytes = 0L
                    progress(0, model.expectedBytes)
                    body.byteStream().use { input -> part.outputStream().use { output ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            bytes += count
                            if (bytes > model.expectedBytes) throw LlmException(LlmFailureReason.MODEL_CORRUPTED, "Oversized model")
                            output.write(buffer, 0, count)
                            progress(bytes, model.expectedBytes)
                        }
                        output.fd.sync()
                    } }
                }
                if (!valid(part, model)) throw LlmException(LlmFailureReason.MODEL_CORRUPTED, "Size/SHA-256 mismatch")
                if (!part.renameTo(target)) throw IOException("Cannot commit model")
                target
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                val reason = if (freeBytes(parent) < reserveBytes || generateSequence<Throwable>(e) { it.cause }
                    .any { it.message?.contains("ENOSPC") == true }) LlmFailureReason.INSUFFICIENT_STORAGE else LlmFailureReason.NETWORK
                throw LlmException(reason, "Model download failed", e)
            } finally { watcher.cancel(); part.delete() }
        } }

    private suspend fun valid(file: File, model: ModelSpec): Boolean {
        if (!file.isFile || file.length() != model.expectedBytes) return false
        val expected = model.sha256 ?: return true
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }.equals(expected, true)
    }
}
