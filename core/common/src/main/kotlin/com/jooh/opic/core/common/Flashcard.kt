package com.jooh.opic.core.common

private val PARENTHESES = Regex("\\([^)]*\\)")
private val IGNORED = Regex("[\\s.,!?~·]")

/** 뜻 문자열을 채점 후보로 나눈다: `*` 제거 → `;`·`,`로 분리 → 괄호 내용 제거 → 공백 정리. */
fun meaningCandidates(meaningKo: String): List<String> =
    meaningKo.removePrefix("*").split(';', ',')
        .map { it.replace(PARENTHESES, "").trim() }
        .filter { it.isNotEmpty() }

private fun normalize(text: String) = text.replace(IGNORED, "")

/** 영→한: 입력이 뜻 후보 중 하나와 공백·문장부호를 무시하고 같으면 정답. */
fun gradeMeaning(input: String, meaningKo: String): Boolean {
    val answer = normalize(input)
    return answer.isNotEmpty() && meaningCandidates(meaningKo).any { normalize(it) == answer }
}

/** 한→영: 앞뒤 공백을 지우고 대소문자를 무시해 완전히 같으면 정답. */
fun gradeWord(input: String, word: String): Boolean = input.trim().lowercase() == word.lowercase()

/** 한→영 힌트: 첫 글자와 마지막 글자만 보이고 나머지 글자는 `_`. 하이픈·공백은 그대로. */
fun maskHint(word: String): String {
    val letters = word.count { it.isLetterOrDigit() }
    if (letters <= 1) return word
    if (letters == 2) return "${word.first()} _"
    val first = word.indexOfFirst { it.isLetterOrDigit() }
    val last = word.indexOfLast { it.isLetterOrDigit() }
    return word.mapIndexed { i, c ->
        when {
            i == first || i == last || !c.isLetterOrDigit() -> c.toString()
            else -> "_"
        }
    }.joinToString(" ")
}
