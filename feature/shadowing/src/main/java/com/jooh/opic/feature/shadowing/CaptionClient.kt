package com.jooh.opic.feature.shadowing

import com.jooh.opic.core.common.Cue
import com.jooh.opic.core.common.extractCaptionTracks
import com.jooh.opic.core.common.mergeSentences
import com.jooh.opic.core.common.parseJson3
import com.jooh.opic.core.common.pickTrack
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.io.ByteArrayOutputStream
import java.net.URLDecoder

internal data class CaptionRequest(val jsonUrl: String, val kind: String?)

/** 플레이어가 실제로 요청한 영어 자막 주소만 허용한다. */
internal fun captionRequest(address: String): CaptionRequest? = runCatching {
    val uri = URI(address)
    if (uri.scheme != "https" || uri.host !in setOf("www.youtube.com", "youtube.com") ||
        uri.userInfo != null || uri.port !in listOf(-1, 443) || uri.path != "/api/timedtext") return null
    val query = uri.rawQuery?.split('&')?.map { part ->
        val pieces = part.split('=', limit = 2)
        URLDecoder.decode(pieces[0], "UTF-8") to URLDecoder.decode(pieces.getOrElse(1) { "" }, "UTF-8")
    }.orEmpty()
    if (query.firstOrNull { it.first == "lang" }?.second != "en") return null
    val kind = query.firstOrNull { it.first == "kind" }?.second
    if (kind != null && kind != "asr") return null
    val withoutFmt = uri.rawQuery?.split('&')?.filterNot { it.substringBefore('=') == "fmt" }.orEmpty()
    val next = uri.scheme + "://" + uri.host + uri.path + "?" + (withoutFmt + "fmt=json3").joinToString("&")
    CaptionRequest(next, kind)
}.getOrNull()

internal data class InterceptedCaption(val body: ByteArray, val cues: List<Cue>, val kind: String?)

/** 쿠키·계정·학습 정보 없이 공개 페이지와 선택한 자막만 요청한다. */
class CaptionClient {
    internal fun intercept(address: String): InterceptedCaption? {
        val request = captionRequest(address) ?: return null
        return runCatching {
            val body = runBlocking(Dispatchers.IO) { get(request.jsonUrl, captionsOnly = true) }
            if (!JSONObject(body).has("events")) return null
            val cues = mergeSentences(parseJson3(body))
            if (cues.isEmpty()) return null
            InterceptedCaption(body.toByteArray(Charsets.UTF_8), cues, request.kind)
        }.getOrNull()
    }
    suspend fun fetch(videoId: String): List<Cue> = withContext(Dispatchers.IO) {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{11}")))
        val html = get("https://www.youtube.com/watch?v=$videoId", captionsOnly = false)
        val track = pickTrack(extractCaptionTracks(html)) ?: return@withContext emptyList()
        val uri = URI(track.baseUrl)
        require(uri.path == "/api/timedtext") { "자막 주소가 아닙니다" }
        val url = track.baseUrl.substringBefore('#') + if (uri.rawQuery == null) "?fmt=json3" else "&fmt=json3"
        val body = get(url, captionsOnly = true)
        if (body.isBlank()) return@withContext emptyList()
        // Android 네트워크 경계에서도 JSON 형식만 허용한다. 실제 cue 파싱은 순수 Kotlin 함수가 담당한다.
        if (!runCatching { JSONObject(body).has("events") }.getOrDefault(false)) return@withContext emptyList()
        mergeSentences(parseJson3(body))
    }

    private suspend fun get(address: String, captionsOnly: Boolean): String {
        var next = address
        repeat(4) {
            val uri = URI(next)
            require(uri.scheme == "https" && uri.host in setOf("www.youtube.com", "youtube.com") && uri.userInfo == null && uri.port in listOf(-1, 443))
            require(uri.path == if (captionsOnly) "/api/timedtext" else "/watch")
            currentCoroutineContext().ensureActive()
            val connection = URL(next).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.setRequestProperty("Accept", if (captionsOnly) "application/json" else "text/html")
            val watcher = CoroutineScope(currentCoroutineContext()).launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                try { awaitCancellation() } finally { connection.disconnect() }
            }
            try {
                val code = connection.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    next = uri.resolve(connection.getHeaderField("Location") ?: error("이동 주소 없음")).toString()
                } else {
                    check(code in 200..299) { "자막 요청 실패 ($code)" }
                    check(connection.contentLengthLong <= MAX_BYTES) { "자막 응답 크기 초과" }
                    val output = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            check(output.size().toLong() + count <= MAX_BYTES) { "자막 응답 크기 초과" }
                            output.write(buffer, 0, count)
                        }
                    }
                    return output.toString(Charsets.UTF_8.name())
                }
            } finally { watcher.cancel(); connection.disconnect() }
        }
        error("자막 요청 이동 횟수 초과")
    }
    private companion object { const val MAX_BYTES = 5L * 1024 * 1024 }
}
