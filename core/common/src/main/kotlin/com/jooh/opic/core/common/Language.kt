package com.jooh.opic.core.common

/**
 * 학습 언어 목록 (2026-10-10). 언어를 추가할 때는 [StudyLanguages.all]에 한 줄만 더한다.
 * DB·백업·통계는 모두 [StudyLanguage.code]로 구분하므로 기존 코드를 고치지 않는다.
 */
data class StudyLanguage(val code: String, val label: String, val exam: String, val requiresRichWordFields: Boolean = false)

object StudyLanguages {
    val EN = StudyLanguage("en", "영어", "OPIc", requiresRichWordFields = true)
    /** 기존 중국어 단어 파일 호환용. 학습 화면에는 아직 노출하지 않는다. */
    val ZH = StudyLanguage("zh", "중국어", "HSK")
    val all: List<StudyLanguage> = listOf(EN)
    val codes: List<String> get() = all.map { it.code }
    fun byCode(code: String): StudyLanguage? = all.firstOrNull { it.code == code }
}
