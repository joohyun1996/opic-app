package com.jooh.opic.core.common

import kotlinx.serialization.json.*

data class CaptionTrack(val baseUrl: String, val languageCode: String, val kind: String? = null)
data class Cue(val startMs: Long, val endMs: Long, val text: String)

fun pickTrack(tracks: List<CaptionTrack>): CaptionTrack? =
    tracks.firstOrNull { it.kind != "asr" && it.languageCode == "en" }
        ?: tracks.firstOrNull { it.kind != "asr" && it.languageCode.startsWith("en-") }
        ?: tracks.firstOrNull { it.kind == "asr" && it.languageCode == "en" }

/** 문자열 안의 괄호·이스케이프를 건너뛰고 JSON 배열의 끝을 찾는다. */
fun extractCaptionTracks(html: String): List<CaptionTrack> = runCatching {
    val marker = Regex("\"captionTracks\"\\s*:\\s*\\[").find(html) ?: return emptyList()
    val start = marker.range.last
    var depth = 0
    var quoted = false
    var escaped = false
    var end = -1
    for (i in start until html.length) {
        val c = html[i]
        if (quoted) {
            if (escaped) escaped = false
            else if (c == '\\') escaped = true
            else if (c == '"') quoted = false
        } else when (c) {
            '"' -> quoted = true
            '[' -> depth++
            ']' -> { depth--; if (depth == 0) { end = i + 1; break } }
        }
    }
    if (end < 0) return emptyList()
    Json.parseToJsonElement(html.substring(start, end)).jsonArray.map { element ->
        val item = element.jsonObject
        CaptionTrack(decodeEntities(item.getValue("baseUrl").jsonPrimitive.content),
            item.getValue("languageCode").jsonPrimitive.content, item["kind"]?.jsonPrimitive?.contentOrNull)
    }
}.getOrDefault(emptyList())

fun parseJson3(json: String): List<Cue> = runCatching {
    Json.parseToJsonElement(json).jsonObject["events"]?.jsonArray.orEmpty().mapNotNull { element ->
        val event = element.jsonObject
        val start = event["tStartMs"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
        val duration = event["dDurationMs"]?.jsonPrimitive?.longOrNull ?: return@mapNotNull null
        if (start < 0 || duration <= 0 || start > Long.MAX_VALUE - duration) return@mapNotNull null
        val text = decodeEntities(event["segs"]?.jsonArray.orEmpty().joinToString("") {
            it.jsonObject["utf8"]?.jsonPrimitive?.contentOrNull.orEmpty()
        }).replace(Regex("\\s+"), " ").trim()
        if (text.isEmpty()) null else Cue(start, start + duration, text)
    }.sortedBy { it.startMs }
}.getOrDefault(emptyList())

private fun decodeEntities(text: String): String = text.replace("&#39;", "'").replace("&quot;", "\"")
    .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ").replace("&amp;", "&")

fun mergeSentences(cues: List<Cue>, maxMs: Long = 12_000): List<Cue> {
    require(maxMs > 0)
    val result = mutableListOf<Cue>()
    var pending: Cue? = null
    for (cue in cues) {
        val previous = pending
        if (previous != null && maxOf(previous.endMs, cue.endMs) - previous.startMs > maxMs) {
            result += previous
            pending = null
        }
        val part = pending
        pending = if (part == null) cue else Cue(part.startMs, maxOf(part.endMs, cue.endMs), "${part.text} ${cue.text}")
        if (pending.text.trimEnd().lastOrNull() in listOf('.', '?', '!') || pending.endMs - pending.startMs >= maxMs) {
            result += pending
            pending = null
        }
    }
    pending?.let(result::add)
    return result
}

/** 시작 포함, 끝 제외. 시간순 문장 목록을 입력한다. */
fun cueAt(cues: List<Cue>, positionMs: Long): Int? {
    var low = 0
    var high = cues.lastIndex
    var candidate = -1
    while (low <= high) {
        val mid = low + (high - low) / 2
        if (cues[mid].startMs <= positionMs) { candidate = mid; low = mid + 1 } else high = mid - 1
    }
    return candidate.takeIf { it >= 0 && positionMs < cues[it].endMs }
}
