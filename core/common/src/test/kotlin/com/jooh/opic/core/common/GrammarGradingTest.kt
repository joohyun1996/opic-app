package com.jooh.opic.core.common

import org.junit.Assert.*
import org.junit.Test

class GrammarGradingTest {
    @Test fun normalizationAndTextGrading() {
        assertEquals("i'm taking", normalizeAnswer("  I’m   taking. "))
        assertTrue(gradeText("I’m taking!", listOf("I'm taking")))
        assertTrue(gradeText("  am   taking. ", listOf("am taking")))
        assertFalse(gradeText("I take", listOf("I'm taking")))
        assertFalse(gradeText("  ", listOf("")))
    }

    @Test fun choiceAndTwoAttempts() {
        assertTrue(gradeChoice(1, 1))
        assertFalse(gradeChoice(0, 1))
        val hint = GrammarAttempt().submit(false)
        assertEquals(GrammarFeedback.HINT, hint.feedback)
        assertFalse(hint.isFinished())
        val second = hint.submit(true)
        assertEquals(GrammarFeedback.CORRECT_SECOND, second.feedback)
        assertEquals(GrammarScore(secondTry = 1), GrammarScore().record(second))
        val reveal = hint.submit(false)
        assertEquals(GrammarFeedback.REVEAL_ANSWER, reveal.feedback)
        assertEquals(GrammarScore(wrong = 1), GrammarScore().record(reveal))
        assertEquals(GrammarFeedback.CORRECT_FIRST, GrammarAttempt().submit(true).feedback)
        assertEquals(GrammarScore(firstTry = 1), GrammarScore().record(GrammarAttempt().submit(true)))
        assertEquals(reveal, reveal.submit(true))
    }
}
