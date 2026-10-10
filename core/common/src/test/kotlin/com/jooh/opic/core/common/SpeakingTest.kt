package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class SpeakingTest {
    private fun catalog(type: String = "describe", secondId: String = "t-2", en: String = "Tell me.") = """
        {"dataVersion":1,"topics":[{"id":"t","titleKo":"주제","questions":[
          {"id":"t-1","type":"$type","en":"Describe it.","ko":"묘사","tip":"팁"},
          {"id":"$secondId","type":"routine","en":"$en","ko":"말해","tip":"팁"}]}]}
    """.trimIndent()

    @Test fun parsesBundledFile() {
        val parsed = parseSpeakingCatalog(File("../../exports/speaking.json").readText())
        assertNotNull(parsed)
        assertEquals(15, parsed!!.topics.size)
        assertEquals(43, parsed.topics.sumOf { it.questions.size })
    }

    @Test fun rejectsInvalidCatalogs() {
        assertNotNull(parseSpeakingCatalog(catalog()))
        assertNull(parseSpeakingCatalog(catalog(secondId = "t-1")))
        assertNull(parseSpeakingCatalog(catalog(type = "monologue")))
        assertNull(parseSpeakingCatalog(catalog(en = " ")))
        assertNull(parseSpeakingCatalog("{}"))
    }

    @Test fun countsWordsAndFillers() {
        val m = speakingMetrics("Um, I like, uh, walking in the park. You know, it is quiet.", 10_000)
        assertEquals(9, m.wordCount) // um·uh·you know 제외, like는 셈
        assertEquals(3, m.fillerCount) // um, uh, you know(구는 1회)
        assertEquals(2, m.sentenceCount)
    }

    @Test fun wordsPerMinute() {
        val text = List(120) { "word$it" }.joinToString(" ")
        assertEquals(120, speakingMetrics(text, 60_000).wordsPerMinute)
        assertEquals(0, speakingMetrics(text, 0).wordsPerMinute)
        assertEquals(0, speakingMetrics(text, 999).wordsPerMinute)
    }

    @Test fun repeatedWordsSkipFunctionWords() {
        val m = speakingMetrics("park park park the the the the cafe cafe cafe cafe dog dog dog movie movie movie", 30_000)
        assertEquals(listOf("cafe" to 4, "dog" to 3, "movie" to 3), m.repeatedWords)
    }

    @Test fun emptyTranscript() {
        val m = speakingMetrics("", 5_000)
        assertEquals(0, m.wordCount); assertEquals(0, m.fillerCount); assertEquals(0, m.sentenceCount)
        assertEquals(emptyList<Pair<String, Int>>(), m.repeatedWords)
    }

    @Test fun advice() {
        assertEquals("조금 더 빠르게", paceAdvice(89)); assertEquals("적당한 속도", paceAdvice(150)); assertEquals("조금 천천히", paceAdvice(151))
        assertEquals("1분 이상 말해 보세요", durationAdvice(59_999)); assertNull(durationAdvice(60_000))
    }
}
