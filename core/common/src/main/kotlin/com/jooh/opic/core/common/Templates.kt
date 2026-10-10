package com.jooh.opic.core.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** OPIc 유형별 답변 템플릿 (TASK 33). type은 speaking.json 질문의 type과 같다 — 새 유형은 데이터만 추가. */
@Serializable data class AnswerTemplates(val dataVersion: Int, val templates: List<AnswerTemplate>)
@Serializable data class AnswerTemplate(
    val type: String, val titleKo: String, val summary: String, val chapters: List<String> = emptyList(),
    val steps: List<TemplateStep>, val sample: String, val sampleKo: String,
)
@Serializable data class TemplateStep(val name: String, val tip: String, val expressions: List<String>)

private val templateJson = Json { ignoreUnknownKeys = false }

/** 형식이 틀리거나, 유형이 겹치거나 목록 밖이거나, 단계가 3개 미만이거나 표현이 비면 null. */
fun parseAnswerTemplates(json: String): AnswerTemplates? = runCatching {
    val data = templateJson.decodeFromString<AnswerTemplates>(json)
    require(data.dataVersion > 0 && data.templates.isNotEmpty())
    require(data.templates.map { it.type }.toSet().size == data.templates.size)
    data.templates.forEach { t ->
        require(t.type in SPEAKING_TYPES && t.steps.size >= 3 && t.sample.isNotBlank())
        require(t.steps.all { it.expressions.isNotEmpty() })
    }
    data
}.getOrNull()
