package com.jooh.opic.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** TASK 39: 집계 쿼리가 전체 행을 읽어 Kotlin으로 센 값과 같고, 다른 언어 행은 섞이지 않는다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class StatsQueriesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun word(language: String, w: String, seq: Int) = WordEntity(language = language, word = w, seq = seq, phonetic = "", meaningKo = "뜻",
        meaningEn = "", example = "", exampleKo = "", level = 1, category = "", partOfSpeech = "")

    private fun answer(language: String, createdAt: Long, wpm: Int, fillers: Int) = SpeakingAnswerEntity(language = language, questionId = "q", topicId = "t",
        createdAt = createdAt, durationMs = 1, transcript = "long text", editedText = "long text", wordCount = 1, wordsPerMinute = wpm, fillerCount = fillers, sentenceCount = 1)

    @Test fun aggregatesMatchKotlinCountsAndFilterLanguage() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, OpicDatabase::class.java).allowMainThreadQueries().build()
        try {
            val ids = listOf("apple", "brave", "cider").mapIndexed { i, w -> db.wordDao().insert(word("en", w, i + 1)) }
            val es = db.wordDao().insert(word("es", "hola", 1))
            db.userWordDao().insert(UserWordEntity(ids[0], correctCount = 3, wrongCount = 2, lastStudiedAt = 100))
            db.userWordDao().insert(UserWordEntity(ids[1], correctCount = 0, wrongCount = 5, lastStudiedAt = 200))
            db.userWordDao().insert(UserWordEntity(ids[2], correctCount = 1, wrongCount = 0, lastStudiedAt = null))
            db.userWordDao().insert(UserWordEntity(es, correctCount = 9, wrongCount = 9, lastStudiedAt = 300))
            val rows = db.userWordDao().backupRows("en")
            assertEquals(rows.mapNotNull { it.lastStudiedAt }.sorted(), db.userWordDao().studiedAt("en").sorted())
            assertEquals(rows.count { it.correctCount >= 3 }, db.userWordDao().masteredCount("en"))
            assertEquals(listOf(KeyCount("brave", 5), KeyCount("apple", 2)), db.userWordDao().mostWrong("en"))
            assertEquals(3, db.wordDao().activeCount("en"))

            db.grammarReviewDao().insert(GrammarReviewEntity("en", "c4-01", "c4", 1, dueEpochDay = 10, wrongCount = 2, lastStudiedAt = 7))
            db.grammarReviewDao().insert(GrammarReviewEntity("en", "c4-02", "c4", 1, dueEpochDay = 12, wrongCount = 1, lastStudiedAt = 8))
            db.grammarReviewDao().insert(GrammarReviewEntity("en", "c1-01", "c1", 1, dueEpochDay = 9, wrongCount = 1, lastStudiedAt = 9))
            db.grammarReviewDao().insert(GrammarReviewEntity("es", "c4-01", "c4", 1, dueEpochDay = 1, wrongCount = 50, lastStudiedAt = 1))
            assertEquals(listOf(KeyCount("c4", 3), KeyCount("c1", 1)), db.grammarReviewDao().mostWrongUnits("en"))
            assertEquals(2, db.grammarReviewDao().dueCount("en", today = 10))
            assertEquals(listOf(7L, 8L, 9L), db.grammarReviewDao().studiedAt("en").sorted())

            (1..12).forEach { db.speakingDao().insert(answer("en", it.toLong(), wpm = it * 10, fillers = it)) }
            db.speakingDao().insert(answer("es", 99, 999, 9))
            val pace = db.speakingDao().recentPace("en")
            assertEquals((12 downTo 3).map { it * 10 }, pace.map { it.wordsPerMinute })
            assertEquals(12, db.speakingDao().createdAt("en").size)

            assertNull(db.shadowingAttemptDao().averageMatch("en"))
            listOf(0.4, 0.8).forEach { db.shadowingAttemptDao().insert(ShadowingAttemptEntity(language = "en", videoId = "v", sentence = "s", heard = "h", matchRate = it, createdAt = 5)) }
            db.shadowingAttemptDao().insert(ShadowingAttemptEntity(language = "es", videoId = "v", sentence = "s", heard = "h", matchRate = 0.0, createdAt = 5))
            assertEquals(0.6, db.shadowingAttemptDao().averageMatch("en")!!, 1e-9)

            // Day 통계 Flow는 dayStats와 같은 값을 내고, 기록이 바뀌면 새 값을 낸다
            assertEquals(db.wordDao().dayStats("en", 40), db.wordDao().observeDayStats("en", 40).first())
            db.userWordDao().update(UserWordEntity(ids[1], correctCount = 3, wrongCount = 5, lastStudiedAt = 300))
            assertEquals(2, db.wordDao().observeDayStats("en", 40).first().single().mastered)
        } finally {
            db.close()
        }
    }
}
