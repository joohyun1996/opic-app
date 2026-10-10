package com.jooh.opic.core.common

private val abbreviations = listOf("mr.", "mrs.", "ms.", "dr.", "a.m.", "p.m.", "e.g.", "i.e.", "u.s.")

fun splitSentences(input: String): List<String> {
    val sentences = mutableListOf<String>()
    var start = 0
    input.forEachIndexed { index, char ->
        if (char !in ".!?" || index + 1 >= input.length || !input[index + 1].isWhitespace()) return@forEachIndexed
        val prefix = input.substring(start, index + 1).trimEnd().lowercase()
        if (char == '.' && abbreviations.any { prefix.endsWith(it) }) return@forEachIndexed
        val part = input.substring(start, index + 1).trim()
        if (part.any(Char::isLetterOrDigit)) sentences += part
        start = index + 1
    }
    val tail = input.substring(start).trim()
    if (tail.any(Char::isLetterOrDigit)) sentences += tail
    return sentences
}

/** 교정할 문장 (문법 탭 5, 스피킹 15). */
fun sentencesToCorrect(input: String, max: Int = 5): List<String> = splitSentences(input).take(max)

data class ErrorGuide(val name: String, val explanation: String)

fun errorTypeGuide(type: String): ErrorGuide = when (type) {
    "tense" -> ErrorGuide("시제", "언제 일어난 일인지에 맞춰 동사 형태를 바꿔야 해요. 끝난 과거는 과거형, 늘 하는 일은 현재형, 지금 하는 일은 be + -ing를 써요. 이야기 중간에 시제가 바뀌지 않게 주의하세요.")
    "article" -> ErrorGuide("관사", "셀 수 있는 명사 하나에는 a/an, 서로 알고 있는 특정한 것에는 the를 써요. 복수나 셀 수 없는 명사를 일반적으로 말할 때는 관사를 쓰지 않아요.")
    "preposition" -> ErrorGuide("전치사", "시간은 at(시각)·on(요일·날짜)·in(월·연도), 장소는 at(지점)·in(안)·on(표면)이 기본이에요. 동사마다 함께 쓰는 전치사가 정해진 경우도 많아요.")
    "agreement" -> ErrorGuide("수 일치", "주어가 3인칭 단수면 현재형 동사에 -s를 붙이고, 복수면 붙이지 않아요. be동사도 주어에 맞춰 is/are, was/were를 골라요.")
    "word_choice" -> ErrorGuide("단어 선택", "뜻은 비슷해도 이 문맥에 어울리지 않는 단어예요. 영어에서 자연스럽게 함께 쓰는 단어 조합을 확인해 보세요.")
    "word_order" -> ErrorGuide("어순", "영어는 주어 + 동사 + 목적어 순서가 기본이에요. 의문문은 조동사가 주어 앞으로 오고, 간접의문문은 평서문 순서로 돌아가요.")
    else -> ErrorGuide("기타", "위 유형에 딱 맞지 않는 오류예요. 고친 문장과 원문을 비교해 보세요.")
}

/**
 * 교정 오류 종류 → 관련 실전 영문법 장 id (TASK 29). 언어마다 표를 따로 둔다 — 새 언어는 표만 추가한다.
 * 모르는 종류·언어는 빈 목록 (버튼 숨김).
 */
private val ERROR_CHAPTERS: Map<String, Map<String, List<String>>> = mapOf(
    "en" to mapOf(
        "tense" to listOf("c4", "c5"),
        "article" to listOf("c18"),
        "preposition" to listOf("c18"),
        "agreement" to listOf("c3", "c18"),
        "word_order" to listOf("c14", "c2"),
    ),
)

fun errorTypeChapters(type: String, language: String = StudyLanguages.EN.code): List<String> =
    ERROR_CHAPTERS[language]?.get(type).orEmpty()

/** 매핑에 쓰인 모든 장 id (데이터 검증용). */
fun errorChapterIds(language: String = StudyLanguages.EN.code): Set<String> =
    ERROR_CHAPTERS[language].orEmpty().values.flatten().toSet()
