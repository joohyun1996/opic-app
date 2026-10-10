package com.jooh.opic.feature.grammar

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object GrammarTracks {
    const val OPIC = "opic"
    const val CORE = "core"
}

@Serializable data class GrammarBook(val dataVersion: Int, val units: List<GrammarUnit>)
@Serializable data class GrammarUnit(
    val id: String,
    val order: Int,
    val title: String,
    val opicUse: String,
    val errorType: String,
    val explanation: GrammarExplanation,
    val exercises: List<GrammarExercise>,
    val writingTask: GrammarWritingTask? = null,
    /** "opic" = OPIc 문법, "core" = 실전 영문법 (TASK 26). */
    val track: String = GrammarTracks.OPIC,
    /** 실전 영문법의 부 이름 (예: "1부 문장의 뼈대"). */
    val part: String? = null,
)
@Serializable data class GrammarExplanation(
    val summary: String,
    val points: List<String>,
    val examples: List<GrammarExample>,
    val commonMistakes: List<GrammarMistake>,
    // 실전 영문법 (TASK 26) — 모두 선택
    val concept: String? = null,
    val table: List<List<String>>? = null,
    val koreanNote: String? = null,
    val breakdowns: List<GrammarBreakdown> = emptyList(),
    /** 자세히 보기 — 소주제별 깊은 설명 (TASK 27, 사용자 요청 "설명이 더 자세했으면"). */
    val details: List<GrammarDetail> = emptyList(),
)
@Serializable data class GrammarDetail(val title: String, val body: String, val examples: List<GrammarExample> = emptyList())
/** 문장 구조 분해. role: S(주어) V(동사) O(목적어) C(보어) M(수식어) 등. */
@Serializable data class GrammarBreakdown(val sentence: String, val parts: List<GrammarPart>, val note: String? = null)
@Serializable data class GrammarPart(val text: String, val role: String)
@Serializable data class GrammarExample(val en: String, val ko: String)
@Serializable data class GrammarMistake(val wrong: String, val right: String, val note: String)
@Serializable data class GrammarWritingTask(val promptKo: String, val promptEn: String, val minSentences: Int, val sample: String? = null)
@Serializable data class GrammarExercise(
    val id: String,
    val kind: String,
    val prompt: String,
    val sentence: String,
    val hint: String,
    val explanation: String,
    val answers: List<String>? = null,
    val choices: List<String>? = null,
    val answer: Int? = null,
) {
    fun displayAnswer(): String = if (kind in TAP_KINDS) choices!![answer!!] else answers!!.first()
}

/** 고르기로 채점하는 유형. */
val TAP_KINDS = setOf("choice", "spot", "structure")

/** OPIc 문법 + 실전 영문법을 한 책으로 합친다 (복습·검색은 하나로, 화면은 track으로 나눔). 하나라도 실패하면 실패. */
fun mergeBooks(vararg results: GrammarLoadResult): GrammarLoadResult {
    val books = results.map { (it as? GrammarLoadResult.Loaded)?.book ?: return GrammarLoadResult.Failed }
    val units = books.flatMap { it.units }
    val ids = units.flatMap { u -> listOf(u.id) + u.exercises.map { it.id } }
    if (ids.size != ids.toSet().size) return GrammarLoadResult.Failed
    return GrammarLoadResult.Loaded(GrammarBook(books.maxOf { it.dataVersion }, units))
}

sealed interface GrammarLoadResult {
    data class Loaded(val book: GrammarBook) : GrammarLoadResult
    data object Failed : GrammarLoadResult
}

object GrammarCatalog {
    private val json = Json { ignoreUnknownKeys = false }

    fun parse(raw: String): GrammarLoadResult = try {
        val book = json.decodeFromString<GrammarBook>(raw)
        val ids = mutableSetOf<String>()
        require(book.dataVersion > 0 && book.units.isNotEmpty())
        book.units.forEach { unit ->
            require(unit.id.isNotBlank() && ids.add(unit.id))
            require(unit.exercises.isNotEmpty())
            unit.exercises.forEach { exercise ->
                require(exercise.id.isNotBlank() && ids.add(exercise.id))
                when (exercise.kind) {
                    "fix", "blank" -> require(exercise.answers?.isNotEmpty() == true &&
                        exercise.answers.all { it.isNotBlank() } && exercise.choices == null && exercise.answer == null)
                    // spot(틀린 곳 찾기)·structure(구조 찾기): choices = 문장의 단어들, answer = 고를 단어 위치 (TASK 26)
                    "choice", "spot", "structure" -> require(exercise.choices?.isNotEmpty() == true &&
                        exercise.answer != null && exercise.answer in exercise.choices.indices && exercise.answers == null)
                    else -> error("Unknown exercise kind")
                }
            }
        }
        GrammarLoadResult.Loaded(book)
    } catch (_: Exception) {
        GrammarLoadResult.Failed
    }
}

/** 문제 id → 출처 ("실전 4장 시제 12개 한눈에", "OPIc 3단원 …") — 복습 화면에 표시 (TASK 31). */
fun GrammarUnit.displayTitle(): String = if (track == GrammarTracks.CORE)
    "실전 ${order}장 ${title.substringBefore(" — ")}" else "OPIc ${order}단원 $title"

fun GrammarBook.sourceLabels(): Map<String, String> = units.flatMap { unit ->
    unit.exercises.map { it.id to unit.displayTitle() }
}.toMap()
