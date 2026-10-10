package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PronunciationTest {
    private fun w(text: String, c: Float) = SpokenWord(text, 0, 100, c)

    @Test fun unclearWordsLowestFirstAndFiltered() {
        val words = listOf(w("I", 0.1f), w("um,", 0.1f), w("world", 0.3f), w("really", 0.2f), w("2020", 0.1f),
            w("park", 0.9f), w("World.", 0.25f), w("a", 0.05f))
        assertEquals(listOf(3, 6), unclearWords(words))
        assertEquals(listOf(3), unclearWords(words, max = 1))
        assertEquals(emptyList<Int>(), unclearWords(words, threshold = 0.1f))
    }

    @Test fun matchesKoreanSpeakerRules() {
        fun ids(word: String) = pronunciationTips(listOf(word), max = 8).map { it.rule.id }.toSet()
        assertTrue("r_l" in ids("really")); assertTrue("f_p" in ids("coffee")); assertTrue("v_b" in ids("very"))
        assertTrue("th" in ids("think")); assertTrue("z_j" in ids("zoo")); assertTrue("final" in ids("milk"))
        assertTrue("w" in ids("work")); assertTrue("ee_i" in ids("sheep"))
        assertEquals(setOf<String>(), ids("um"))
    }

    @Test fun tipsSortedByHitsWithDistinctExamples() {
        val tips = pronunciationTips(listOf("very", "very", "very", "think", "really", "lovely", "level", "rule", "real"))
        assertEquals("r_l", tips.first().rule.id)
        assertEquals(listOf("very", "really", "lovely", "level"), tips.first().examples) // very에도 r
        assertEquals(listOf("very", "lovely", "level"), tips.first { it.rule.id == "v_b" }.examples)
    }

    @Test fun linkingReductionsAndFlaps() {
        assertEquals(listOf("pick" to "it", "it" to "up"), linkingPairs("Pick it up."))
        assertEquals(emptyList<Pair<String, String>>(), linkingPairs("I go to school"))
        assertEquals(listOf("want to" to "wanna"), reductions("I want to go home."))
        assertEquals(listOf("going to" to "gonna", "a lot of" to "a lotta"), reductions("I'm going to eat a lot of food"))
        assertEquals(listOf("water", "city", "getting", "better"), flapWords("Water in the city is getting better at times on top"))
        assertEquals(emptyList<String>(), flapWords("time top"))
    }
}
