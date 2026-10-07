package com.jooh.opic.core.common

import org.junit.Assert.*
import org.junit.Test

class WritingTest {
    @Test fun keepsAbbreviationsAndDropsEmptySentences() {
        val input = "  Mr. Smith met Dr. Lee at 9 a.m. in the U.S. He was happy!  .  Was Ms. Kim there?  "
        assertEquals(listOf("Mr. Smith met Dr. Lee at 9 a.m. in the U.S. He was happy!", "Was Ms. Kim there?"), splitSentences(input))
        assertEquals(listOf("Mrs. A arrived at 3 p.m. e.g. after lunch.", "I.e. she was late."),
            splitSentences("Mrs. A arrived at 3 p.m. e.g. after lunch. I.e. she was late."))
    }

    @Test fun takesOnlyFirstFive() {
        val input = "One. Two! Three? Four. Five. Six."
        assertEquals(6, splitSentences(input).size)
        assertEquals(listOf("One.", "Two!", "Three?", "Four.", "Five."), sentencesToCorrect(input))
    }

    @Test fun guidesCoverKnownAndUnknownTypes() {
        listOf("tense", "article", "preposition", "agreement", "word_choice", "word_order", "other").forEach {
            assertTrue(errorTypeGuide(it).name.isNotBlank())
            assertTrue(errorTypeGuide(it).explanation.isNotBlank())
        }
        assertEquals(errorTypeGuide("other"), errorTypeGuide("unrecognized"))
    }
}
