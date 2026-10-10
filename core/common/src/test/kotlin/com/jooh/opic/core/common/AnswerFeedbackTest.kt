package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnswerFeedbackTest {
    private val steps = listOf("도입", "특징", "느낌")

    @Test fun promptIncludesQuestionStepsAndAnswer() {
        val prompt = buildAnswerFeedbackPrompt("Describe your home.", steps, "I live in Seoul.")
        assertTrue(prompt.contains("Describe your home.") && prompt.contains("도입 → 특징 → 느낌") && prompt.contains("I live in Seoul."))
        assertTrue(prompt.contains("JSON만"))
    }

    @Test fun parsesValidFeedbackWrappedInText() {
        val raw = """결과: {"score": 3, "onTopic": true, "missingSteps": ["느낌"], "strengths": ["질문에 맞게 답했어요 {좋아요}"], "improvements": ["예시를 하나 더"], "betterOpening": "Let me tell you about my home."} 끝"""
        val f = parseAnswerFeedback(raw, steps)!!
        assertEquals(3, f.score); assertEquals(listOf("느낌"), f.missingSteps)
        assertEquals("질문에 맞게 답했어요 {좋아요}", f.strengths.single())
        assertEquals("Let me tell you about my home.", f.betterOpening)
    }

    @Test fun rejectsBadScoreUnknownStepOrEmptyAdvice() {
        assertNull(parseAnswerFeedback("""{"score": 7, "onTopic": true, "missingSteps": [], "strengths": ["a"], "improvements": [], "betterOpening": ""}""", steps))
        assertNull(parseAnswerFeedback("""{"score": 3, "onTopic": true, "missingSteps": ["결론"], "strengths": ["a"], "improvements": [], "betterOpening": ""}""", steps))
        assertNull(parseAnswerFeedback("""{"score": 3, "onTopic": true, "missingSteps": [], "strengths": [], "improvements": [], "betterOpening": ""}""", steps))
        assertNull(parseAnswerFeedback("no json", steps))
    }
}
