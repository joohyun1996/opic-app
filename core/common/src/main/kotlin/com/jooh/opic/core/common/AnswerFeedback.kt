package com.jooh.opic.core.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 스피킹 답변 전체에 대한 내용·구조 피드백 (2026-10-10). 문장 단위 문법 교정과 달리
 * 질문에 맞게 답했는지, 유형별 템플릿 단계를 갖췄는지, 길이가 충분한지를 본다.
 */
data class AnswerFeedback(
    val score: Int,                 // 1~5
    val onTopic: Boolean,
    val missingSteps: List<String>, // 템플릿 단계 중 빠진 것 (단계 이름 그대로)
    val strengths: List<String>,
    val improvements: List<String>,
    val betterOpening: String?,     // 더 나은 첫 문장 (영어)
)

fun buildAnswerFeedbackPrompt(question: String, steps: List<String>, answer: String): String {
    require(question.isNotBlank() && answer.isNotBlank())
    val stepLine = if (steps.isEmpty()) "없음" else steps.joinToString(" → ")
    return """
        당신은 OPIc 영어 말하기 시험 코치입니다. 아래 질문에 대한 학습자의 말하기 답변(음성 인식 결과)을 평가하세요.
        문법 오류 하나하나가 아니라 내용과 구조를 봅니다: 질문에 맞게 답했는지, 권장 순서의 단계를 갖췄는지, 구체적인 예시와 길이가 충분한지.
        JSON만 반환하세요. 마크다운이나 앞뒤 설명을 쓰지 마세요.
        형식: {"score": 1~5 정수, "onTopic": Boolean, "missingSteps": [String], "strengths": [String], "improvements": [String], "betterOpening": String}
        - missingSteps에는 권장 단계 이름 중 답변에 없는 것만 그대로 쓰세요. 다 있으면 []
        - strengths, improvements는 각각 1~3개, 한국어 한 문장씩
        - betterOpening은 이 질문에 어울리는 더 좋은 첫 문장 하나 (영어)
        질문: ${question.trim()}
        권장 단계: $stepLine
        답변: ${answer.trim()}
    """.trimIndent()
}

/** 형식이 틀리거나 점수가 범위 밖이거나, 모르는 단계 이름이 오면 null. */
fun parseAnswerFeedback(raw: String, steps: List<String>): AnswerFeedback? = runCatching {
    val root = Json.parseToJsonElement(extractJsonObject(raw) ?: return null) as? JsonObject ?: return null
    fun strings(key: String): List<String>? = (root[key] as? JsonArray)?.map { it.jsonPrimitive.content.trim() }?.filter { it.isNotEmpty() }
    val score = root["score"]?.jsonPrimitive?.intOrNull?.takeIf { it in 1..5 } ?: return null
    val onTopic = root["onTopic"]?.jsonPrimitive?.booleanOrNull ?: return null
    val missing = strings("missingSteps") ?: return null
    if (!steps.containsAll(missing)) return null
    val strengths = strings("strengths")?.take(3) ?: return null
    val improvements = strings("improvements")?.take(3) ?: return null
    if (strengths.isEmpty() && improvements.isEmpty()) return null
    val opening = root["betterOpening"]?.jsonPrimitive?.content?.trim()?.takeIf { it.isNotEmpty() }
    AnswerFeedback(score, onTopic, missing, strengths, improvements, opening)
}.getOrNull()

/** 모델 출력에서 첫 JSON 객체만 잘라 낸다 (따옴표 안의 중괄호는 무시). 없으면 null. */
internal fun extractJsonObject(raw: String): String? {
    val start = raw.indexOf('{')
    if (start < 0) return null
    var depth = 0; var quoted = false; var escaped = false
    for (i in start until raw.length) {
        val c = raw[i]
        if (quoted) { if (escaped) escaped = false else if (c == '\\') escaped = true else if (c == '"') quoted = false }
        else when (c) { '"' -> quoted = true; '{' -> depth++; '}' -> { depth--; if (depth == 0) return raw.substring(start, i + 1) } }
    }
    return null
}
