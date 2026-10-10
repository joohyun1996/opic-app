package com.jooh.opic.core.common

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 섀도잉 추천 영상 (TASK 21). 링크만 담고 영상은 내려받지 않는다. */
@Serializable data class ShadowingVideo(val id: String, val title: String, val channel: String, val category: String, val level: String, val minutes: Int)
@Serializable data class ShadowingLibrary(val dataVersion: Int, val checkedAt: String, val videos: List<ShadowingVideo>)

/** 화면 순서대로: category → 이름. */
val SHADOWING_CATEGORIES = listOf(
    "learner" to "학습자용", "conversation" to "일상 대화", "interview" to "길거리 인터뷰",
    "pronunciation" to "발음", "teded" to "TED-Ed", "ted" to "TED 강연",
)
private val libraryJson = Json { ignoreUnknownKeys = false }
private val VIDEO_ID = Regex("[A-Za-z0-9_-]{11}")

/** 형식·ID·중복·분류·등급이 틀리면 null. */
fun parseShadowingLibrary(json: String): ShadowingLibrary? = runCatching {
    val library = libraryJson.decodeFromString<ShadowingLibrary>(json)
    val categories = SHADOWING_CATEGORIES.map { it.first }.toSet()
    val ids = mutableSetOf<String>()
    require(library.dataVersion > 0 && library.videos.isNotEmpty())
    library.videos.forEach { v ->
        require(VIDEO_ID.matches(v.id) && ids.add(v.id) && v.title.isNotBlank() && v.category in categories && v.level in SPEAKING_LEVELS && v.minutes > 0)
    }
    library
}.getOrNull()
