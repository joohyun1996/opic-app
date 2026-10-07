package com.jooh.opic.core.common

/** 비교용 단어 목록: 소문자, 문장부호 제거(아포스트로피는 단어 안에서 유지), 공백 기준 분리. */
fun werWords(text: String): List<String> = text.lowercase()
    .replace('’', '\'')
    .replace(Regex("[^a-z0-9'\\s]"), " ")
    .split(Regex("\\s+"))
    .map { it.trim('\'') }
    .filter { it.isNotEmpty() }

/**
 * 단어 오류율(WER) = (치환 + 삽입 + 삭제) / 기준(reference) 단어 수.
 * 기준이 비어 있으면 가설도 비었을 때 0, 아니면 1.
 */
fun wordErrorRate(reference: String, hypothesis: String): Double {
    val ref = werWords(reference)
    val hyp = werWords(hypothesis)
    if (ref.isEmpty()) return if (hyp.isEmpty()) 0.0 else 1.0
    var previous = IntArray(hyp.size + 1) { it }
    for (i in 1..ref.size) {
        val current = IntArray(hyp.size + 1)
        current[0] = i
        for (j in 1..hyp.size) {
            val substitution = previous[j - 1] + if (ref[i - 1] == hyp[j - 1]) 0 else 1
            current[j] = minOf(substitution, previous[j] + 1, current[j - 1] + 1)
        }
        previous = current
    }
    return previous[hyp.size].toDouble() / ref.size
}
