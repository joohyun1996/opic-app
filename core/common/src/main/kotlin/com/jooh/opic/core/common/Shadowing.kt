package com.jooh.opic.core.common

import java.net.URI

fun youtubeVideoId(url: String): String? = runCatching {
    val uri = URI(url.trim())
    if (uri.scheme != "https" && uri.scheme != "http") return null
    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
    val path = uri.path.trim('/').split('/')
    val id = when (host) {
        "youtu.be" -> path.singleOrNull()
        "youtube.com", "m.youtube.com" -> when (path.firstOrNull()) {
            "watch" -> uri.rawQuery?.split('&')?.firstOrNull { it.startsWith("v=") }?.substring(2)
            "shorts", "embed" -> path.getOrNull(1)
            else -> null
        }
        else -> null
    }
    id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
}.getOrNull()

sealed interface WordDiff {
    data class Match(val word: String) : WordDiff
    data class Substitute(val ref: String, val hyp: String) : WordDiff
    data class Delete(val ref: String) : WordDiff
    data class Insert(val hyp: String) : WordDiff
}

fun compareWords(reference: String, hypothesis: String): List<WordDiff> {
    val a = werWords(reference)
    val b = werWords(hypothesis)
    val dp = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in 0..a.size) dp[i][0] = i
    for (j in 0..b.size) dp[0][j] = j
    for (i in 1..a.size) for (j in 1..b.size) dp[i][j] = minOf(
        dp[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1,
        dp[i - 1][j] + 1, dp[i][j - 1] + 1,
    )
    val result = mutableListOf<WordDiff>()
    var i = a.size
    var j = b.size
    while (i > 0 || j > 0) {
        when {
            i > 0 && j > 0 && a[i - 1] == b[j - 1] && dp[i][j] == dp[i - 1][j - 1] -> {
                result += WordDiff.Match(a[i - 1]); i--; j--
            }
            i > 0 && j > 0 && dp[i][j] == dp[i - 1][j - 1] + 1 -> {
                result += WordDiff.Substitute(a[i - 1], b[j - 1]); i--; j--
            }
            i > 0 && dp[i][j] == dp[i - 1][j] + 1 -> { result += WordDiff.Delete(a[i - 1]); i-- }
            else -> { result += WordDiff.Insert(b[j - 1]); j-- }
        }
    }
    return result.asReversed()
}
