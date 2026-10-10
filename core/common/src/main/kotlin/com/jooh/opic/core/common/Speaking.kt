package com.jooh.opic.core.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable data class SpeakingCatalog(val dataVersion: Int, val topics: List<SpeakingTopic>)
@Serializable data class SpeakingTopic(val id: String, val titleKo: String, val questions: List<SpeakingQuestion>, val category: String = "survey")
@Serializable data class SpeakingQuestion(val id: String, val type: String, val en: String, val ko: String, val tip: String, val level: String = "IM")

val SPEAKING_TYPES = setOf("describe", "routine", "experience", "compare", "roleplay_ask", "roleplay_solve", "issue")
val SPEAKING_CATEGORIES = setOf("intro", "survey", "unexpected", "roleplay")
val SPEAKING_LEVELS = setOf("IM", "IH", "AL")
private val catalogJson = Json { ignoreUnknownKeys = false }

/** 형식이 틀리거나 id가 겹치거나 type이 목록 밖이면 null. */
fun parseSpeakingCatalog(json: String): SpeakingCatalog? = runCatching {
    val catalog = catalogJson.decodeFromString<SpeakingCatalog>(json)
    require(catalog.dataVersion > 0 && catalog.topics.isNotEmpty())
    val ids = mutableSetOf<String>()
    catalog.topics.forEach { topic ->
        require(topic.id.isNotBlank() && topic.titleKo.isNotBlank() && ids.add(topic.id) && topic.questions.isNotEmpty() && topic.category in SPEAKING_CATEGORIES)
        topic.questions.forEach { q ->
            require(q.id.isNotBlank() && ids.add(q.id) && q.type in SPEAKING_TYPES && q.level in SPEAKING_LEVELS && q.en.isNotBlank() && q.ko.isNotBlank())
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

/** 모의고사 한 문항. [part]는 화면 표시용 ("설문 주제 1" 등). */
data class MockItem(val number: Int, val part: String, val topicId: String, val question: SpeakingQuestion)

private val ROLEPLAY_SET = listOf("roleplay_ask", "roleplay_solve", "experience")

/** 실제 OPIc 순서 15문항 (TASK 20). 조건에 맞는 주제가 모자라면 null. */
fun buildMockExam(catalog: SpeakingCatalog, random: kotlin.random.Random): List<MockItem>? {
    val topics = catalog.topics
    val intro = topics.filter { it.category == "intro" }.flatMap { t -> t.questions.map { t to it } }.randomOrNull(random) ?: return null
    val surveys = topics.filter { it.category == "survey" && it.questions.size >= 3 }.shuffled(random).take(2)
    if (surveys.size < 2) return null
    val unexpected = topics.filter { it.category == "unexpected" && it.questions.size >= 3 }.randomOrNull(random) ?: return null
    val roleplay = topics.filter { t -> t.category == "roleplay" && t.questions.map { it.type }.take(3) == ROLEPLAY_SET }.randomOrNull(random) ?: return null
    val used = (surveys + unexpected + roleplay).map { it.id }.toMutableSet()
    fun pick(type: String) = topics.filter { it.id !in used && it.category in setOf("survey", "unexpected") }
        .flatMap { t -> t.questions.filter { it.type == type }.map { t to it } }.randomOrNull(random)?.also { used += it.first.id }
    val compare = pick("compare") ?: return null
    val issue = pick("issue") ?: return null
    val items = mutableListOf<MockItem>()
    fun add(part: String, topic: SpeakingTopic, q: SpeakingQuestion) { items += MockItem(items.size + 1, part, topic.id, q) }
    add("자기소개", intro.first, intro.second)
    surveys.forEachIndexed { i, t -> t.questions.take(3).forEach { add("설문 주제 ${i + 1} · ${t.titleKo}", t, it) } }
    unexpected.questions.take(3).forEach { add("돌발 · ${unexpected.titleKo}", unexpected, it) }
    roleplay.questions.take(3).forEach { add("롤플레이 · ${roleplay.titleKo.removePrefix("롤플레이 — ")}", roleplay, it) }
    add("고난도 · 비교", compare.first, compare.second)
    add("고난도 · 이슈", issue.first, issue.second)
    return items
}
