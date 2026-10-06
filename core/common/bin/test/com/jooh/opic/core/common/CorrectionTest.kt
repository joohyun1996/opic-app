package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectionTest {
    private val ok = """{"correct":true,"corrected":"I went home.","errors":[]}"""
    private val wrong = """{"correct":false,"corrected":"I went home.","errors":[{"type":"tense","original":"go","fix":"went","explanationKo":"과거 시제를 써야 합니다."}]}"""

    @Test fun promptContainsContract() {
        val prompt = buildCorrectionPrompt("I go home.")
        assertTrue(prompt.contains("I go home."))
        assertTrue(prompt.contains("JSON만 반환"))
        listOf("tense", "article", "preposition", "agreement", "word_choice", "word_order", "other").forEach { assertTrue(prompt.contains(it)) }
    }

    @Test fun validAndWrappedJson() {
        assertTrue(parseCorrection(ok, "I went home.") is CorrectionParse.Ok)
        assertTrue(parseCorrection("앞말```json\n$ok\n```뒷말", "I went home.") is CorrectionParse.Ok)
    }

    @Test fun brokenJsonIsInvalid() {
        assertEquals(CorrectionParse.InvalidJson, parseCorrection(ok.dropLast(2), "I went home."))
    }

    @Test fun contradictoryResults() {
        val hasError = wrong.substring(0, wrong.length)
        assertEquals(CorrectionParse.Contradiction, parseCorrection(hasError.replace("\"correct\":false", "\"correct\":true"), "I go home."))
        assertEquals(CorrectionParse.Contradiction, parseCorrection(ok.replace("\"correct\":true", "\"correct\":false"), "I went home."))
        assertEquals(CorrectionParse.Contradiction, parseCorrection(wrong, "I went home."))
    }

    @Test fun unknownTypeBecomesOther() {
        val parsed = parseCorrection(wrong.replace("\"tense\"", "\"unknown\""), "I go home.")
        assertEquals("other", (parsed as CorrectionParse.Ok).result.errors.single().type)
    }
}
