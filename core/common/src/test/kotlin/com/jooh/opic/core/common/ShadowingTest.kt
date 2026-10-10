package com.jooh.opic.core.common

import org.junit.Assert.*
import org.junit.Test

class ShadowingTest {
    private val id = "AbC_123-xYz"
    @Test fun youtubeLinks() {
        listOf("https://youtu.be/$id", "https://youtu.be/$id?si=abc", "https://youtube.com/watch?v=$id&t=3",
            "https://youtube.com/shorts/$id", "https://youtube.com/embed/$id", "https://m.youtube.com/watch?v=$id")
            .forEach { assertEquals(it, id, youtubeVideoId(it)) }
        listOf("https://evil.com/watch?v=$id", "https://youtu.be/short", "https://youtube.com/watch?v=${id}x", "bad")
            .forEach { assertNull(it, youtubeVideoId(it)) }
    }
    @Test fun diffsAndWer() {
        assertEquals(listOf(WordDiff.Match("i"), WordDiff.Substitute("walk", "work"), WordDiff.Match("my"), WordDiff.Match("dog")),
            compareWords("I walk my dog", "I work my dog"))
        for ((a, b) in listOf("I walk my dog" to "I my dog", "I walk" to "I quickly walk", "Hello!" to "hello", "" to "", "" to "hi", "a b c" to "c")) {
            val diff = compareWords(a, b)
            val errors = diff.count { it !is WordDiff.Match }
            val words = werWords(a).size
            assertEquals(wordErrorRate(a, b), if (words == 0) if (errors == 0) 0.0 else 1.0 else errors.toDouble() / words, 0.0)
        }
        assertTrue(compareWords("", "").isEmpty())
        assertEquals(listOf(WordDiff.Insert("hi")), compareWords("", "hi"))
    }

    @Test fun matchRateStaysInRangeWithExtraWords() {
        val diff = compareWords("I walk", "I walk with my dog today")
        assertEquals(1.0, matchRate(diff, 2), 0.0)
        assertEquals(0.0, matchRate(compareWords("I walk", "you run more words"), 2), 0.0)
        assertEquals(0.0, matchRate(compareWords("", "extra words"), 0), 0.0)
    }
}
