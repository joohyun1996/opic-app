package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val questions = parsed!!.topics.flatMap { it.questions }
        assertTrue(questions.size >= 140)
        val ids = questions.map { it.id }.toSet()
        // TASK 17의 43문항 id 유지
        listOf("intro-1", "home-1", "home-3", "music-2", "travel_abroad-3", "weather-3", "restaurant-3", "roleplay_ask-3", "roleplay_solve-3")
            .forEach { assertTrue(it, it in ids) }
        assertEquals(SPEAKING_CATEGORIES, parsed.topics.map { it.category }.toSet())
        assertEquals(SPEAKING_LEVELS, questions.map { it.level }.toSet())
    }

    @Test fun rejectsInvalidCatalogs() {
        assertNotNull(parseSpeakingCatalog(catalog()))
        assertNull(parseSpeakingCatalog(catalog(secondId = "t-1")))
        assertNull(parseSpeakingCatalog(catalog(type = "monologue")))
        assertNull(parseSpeakingCatalog(catalog(en = " ")))
        assertNull(parseSpeakingCatalog("{}"))
        assertNull(parseSpeakingCatalog(catalog().replace("\"titleKo\":\"주제\"", "\"titleKo\":\"주제\",\"category\":\"exam\"")))
        assertNull(parseSpeakingCatalog(catalog().replace("\"tip\":\"팁\"}", "\"tip\":\"팁\",\"level\":\"AH\"}")))
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

    @Test fun mockExamFollowsOpicOrder() {
        val catalog = parseSpeakingCatalog(File("../../exports/speaking.json").readText())!!
        val exam = buildMockExam(catalog, kotlin.random.Random(7))!!
        assertEquals(15, exam.size)
        assertEquals((1..15).toList(), exam.map { it.number })
        val topic = catalog.topics.associateBy { it.id }
        assertEquals("intro", topic[exam[0].topicId]!!.category)
        val a = exam.subList(1, 4).map { it.topicId }.toSet(); val b = exam.subList(4, 7).map { it.topicId }.toSet()
        assertEquals(1, a.size); assertEquals(1, b.size); assertTrue(a != b)
        assertEquals("survey", topic[a.first()]!!.category); assertEquals("survey", topic[b.first()]!!.category)
        assertTrue(exam.subList(7, 10).all { topic[it.topicId]!!.category == "unexpected" })
        assertEquals(listOf("roleplay_ask", "roleplay_solve", "experience"), exam.subList(10, 13).map { it.question.type })
        assertEquals(1, exam.subList(10, 13).map { it.topicId }.toSet().size)
        assertEquals("compare", exam[13].question.type); assertEquals("issue", exam[14].question.type)
        assertEquals(15, exam.map { it.question.id }.toSet().size)
        assertEquals(exam, buildMockExam(catalog, kotlin.random.Random(7)))
        assertNull(buildMockExam(SpeakingCatalog(1, catalog.topics.filter { it.category != "roleplay" }), kotlin.random.Random(1)))
    }
}
