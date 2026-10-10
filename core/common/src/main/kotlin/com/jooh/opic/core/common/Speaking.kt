package com.jooh.opic.core.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class SpeakingCatalog(val dataVersion: Int, val topics: List<SpeakingTopic>)
@Serializable data class SpeakingTopic(val id: String, val titleKo: String, val questions: List<SpeakingQuestion>)
@Serializable data class SpeakingQuestion(val id: String, val type: String, val en: String, val ko: String, val tip: String)

val SPEAKING_TYPES = setOf("describe", "routine", "experience", "compare", "roleplay_ask", "roleplay_solve")
private val catalogJson = Json { ignoreUnknownKeys = false }

/** 형식이 틀리거나 id가 겹치거나 type이 목록 밖이면 null. */
fun parseSpeakingCatalog(json: String): SpeakingCatalog? = runCatching {
    val catalog = catalogJson.decodeFromString<SpeakingCatalog>(json)
    require(catalog.dataVersion > 0 && catalog.topics.isNotEmpty())
    val ids = mutableSetOf<String>()
    catalog.topics.forEach { topic ->
        require(topic.id.isNotBlank() && topic.titleKo.isNotBlank() && ids.add(topic.id) && topic.questions.isNotEmpty())
        topic.questions.forEach { q ->
            require(q.id.isNotBlank() && ids.add(q.id) && q.type in SPEAKING_TYPES && q.en.isNotBlank() && q.ko.isNotBlank())
        }
    }
    catalog
}.getOrNull()

data class SpeakingMetrics(
    val durationMs: Long,
    val wordCount: Int,
    val wordsPerMinute: Int,
    val fillerCount: Int,
    val sentenceCount: Int,
    val repeatedWords: List<Pair<String, Int>>,
)

private val FILLER_WORDS = setOf("um", "uh", "er", "erm", "hmm", "mm")
private val FILLER_PHRASES = listOf(listOf("you", "know"), listOf("i", "mean"))
private val FUNCTION_WORDS = setOf(
    "the", "a", "an", "and", "or", "but", "so", "i", "you", "he", "she", "it", "we", "they", "me", "my",
    "to", "of", "in", "on", "at", "for", "with", "is", "am", "are", "was", "were", "be", "that", "this", "there",
    "do", "did", "have", "has", "had", "it's", "i'm", "very", "really", "because",
)

/** 단어 목록에서 머뭇거림 위치. 구(you know, i mean)는 두 단어 모두 표시한다. */
fun fillerMask(words: List<String>): BooleanArray {
    val mask = BooleanArray(words.size) { words[it] in FILLER_WORDS }
    for (i in 0 until words.size - 1) {
        if (FILLER_PHRASES.any { it[0] == words[i] && it[1] == words[i + 1] }) { mask[i] = true; mask[i + 1] = true }
    }
    return mask
}

fun speakingMetrics(transcript: String, durationMs: Long): SpeakingMetrics {
    val words = werWords(transcript)
    val mask = fillerMask(words)
    var fillers = words.count { it in FILLER_WORDS }
    for (i in 0 until words.size - 1) if (FILLER_PHRASES.any { it[0] == words[i] && it[1] == words[i + 1] }) fillers++
    val content = words.filterIndexed { i, _ -> !mask[i] }
    val wpm = if (durationMs < 1_000) 0 else (content.size * 60_000.0 / durationMs).toInt()
    val sentences = transcript.split(Regex("[.?!]+")).count { werWords(it).isNotEmpty() }
    val repeated = content.filter { it !in FUNCTION_WORDS }.groupingBy { it }.eachCount()
        .filter { it.value >= 3 }.toList()
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }).take(3)
    return SpeakingMetrics(durationMs, content.size, wpm, fillers, sentences, repeated)
}

/** 화면 안내 문구 (TASK 17 고정 기준). */
fun paceAdvice(wpm: Int): String = when {
    wpm < 90 -> "조금 더 빠르게"
    wpm <= 150 -> "적당한 속도"
    else -> "조금 천천히"
}
fun durationAdvice(durationMs: Long): String? = if (durationMs < 60_000) "1분 이상 말해 보세요" else null
