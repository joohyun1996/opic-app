package com.jooh.opic.core.common

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

private val errorTypes = setOf("tense", "article", "preposition", "agreement", "word_choice", "word_order", "other")

data class CorrectionError(val type: String, val original: String, val fix: String, val explanationKo: String)
data class CorrectionResult(val correct: Boolean, val corrected: String, val errors: List<CorrectionError>)
sealed interface CorrectionParse {
    data class Ok(val result: CorrectionResult) : CorrectionParse
    data object InvalidJson : CorrectionParse
    data object Contradiction : CorrectionParse
}

fun buildCorrectionPrompt(sentence: String): String {
    require(sentence.isNotBlank())
    return """
        다음 영어 문장 하나를 교정하세요. 설명은 한국어 한 문장으로 쓰세요.
        JSON만 반환하세요. 마크다운이나 앞뒤 설명을 쓰지 마세요.
        형식: {"correct": Boolean, "corrected": String, "errors": [{"type": String, "original": String, "fix": String, "explanationKo": String}]}
        type은 tense | article | preposition | agreement | word_choice | word_order | other 중 하나만 사용하세요.
        문장이 맞으면 correct=true, corrected는 입력 문장 그대로, errors=[]로 쓰세요.
        문장이 틀리면 correct=false, corrected는 고친 전체 문장, errors에는 고친 항목을 넣으세요.
        입력 문장: ${sentence.trim()}
    """.trimIndent()
}

fun parseCorrection(raw: String, original: String): CorrectionParse {
    val start = raw.indexOf('{')
    if (start < 0) return CorrectionParse.InvalidJson
    var depth = 0
    var quoted = false
    var escaped = false
    var end = -1
    for (i in start until raw.length) {
        val char = raw[i]
        if (quoted) {
            if (escaped) escaped = false
            else if (char == '\\') escaped = true
            else if (char == '"') quoted = false
        } else {
            when (char) {
                '"' -> quoted = true
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) { end = i + 1; break } }
            }
        }
    }
    if (end < 0) return CorrectionParse.InvalidJson
    return try {
        val root = Json.parseToJsonElement(raw.substring(start, end)) as? JsonObject ?: return CorrectionParse.InvalidJson
        val correct = root["correct"]?.jsonPrimitive?.booleanOrNull ?: return CorrectionParse.InvalidJson
        val corrected = root["corrected"]?.jsonPrimitive?.content ?: return CorrectionParse.InvalidJson
        if (corrected.isBlank()) return CorrectionParse.InvalidJson
        val array = root["errors"] as? JsonArray ?: return CorrectionParse.InvalidJson
        val errors = array.map { item ->
            val obj = item as? JsonObject ?: return CorrectionParse.InvalidJson
            val type = obj["type"]?.jsonPrimitive?.content ?: return CorrectionParse.InvalidJson
            val part = obj["original"]?.jsonPrimitive?.content ?: return CorrectionParse.InvalidJson
            val fix = obj["fix"]?.jsonPrimitive?.content ?: return CorrectionParse.InvalidJson
            val explanation = obj["explanationKo"]?.jsonPrimitive?.content ?: return CorrectionParse.InvalidJson
            if (part.isBlank() || fix.isBlank() || explanation.isBlank()) return CorrectionParse.InvalidJson
            CorrectionError(type.takeIf { it in errorTypes } ?: "other", part, fix, explanation)
        }
        if ((correct && errors.isNotEmpty()) || (!correct && errors.isEmpty()) ||
            (errors.isNotEmpty() && corrected.trim() == original.trim())) CorrectionParse.Contradiction
        else CorrectionParse.Ok(CorrectionResult(correct, corrected, errors))
    } catch (_: Exception) {
        CorrectionParse.InvalidJson
    }
}
