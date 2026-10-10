package com.jooh.opic.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 고른 기록만, 그 언어만 지우고 단어 데이터는 남는다. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecordDeletionTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun word(language: String, w: String, seq: Int) = WordEntity(language = language, word = w, seq = seq, phonetic = "", meaningKo = "뜻",
        meaningEn = "", example = "", exampleKo = "", level = 1, category = "", partOfSpeech = "")
    private fun answer(language: String, at: Long) = SpeakingAnswerEntity(language = language, questionId = "q", topicId = "t", createdAt = at,
        durationMs = 1, transcript = "", editedText = "", wordCount = 0, wordsPerMinute = 0, fillerCount = 0, sentenceCount = 0)

    @Test fun deletesOnlySelectedRowsOfLanguage() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, OpicDatabase::class.java).allowMainThreadQueries().build()
        try {
            val a = db.wordDao().insert(word("en", "apple", 1)); val b = db.wordDao().insert(word("en", "brave", 2))
            val es = db.wordDao().insert(word("es", "hola", 1))
            listOf(a, b, es).forEach { db.userWordDao().insert(UserWordEntity(it, 1, 1, 10)) }
            assertEquals(1, deleteRecords(db, "en", RecordKind.WORDS, listOf(a.toString(), es.toString())))
            assertNull(db.userWordDao().get(a)); assertNotNull(db.userWordDao().get(b)); assertNotNull(db.userWordDao().get(es))
            assertNotNull(db.wordDao().find("en", "apple")) // 단어 자체는 남는다
            assertEquals(listOf("brave"), db.userWordDao().records("en").map { it.word })

            db.grammarReviewDao().insert(GrammarReviewEntity("en", "c4-01", "c4", 1, 1, 1, 1))
            db.grammarReviewDao().insert(GrammarReviewEntity("es", "c4-01", "c4", 1, 1, 1, 1))
            assertEquals(1, deleteRecords(db, "en", RecordKind.GRAMMAR, listOf("c4-01")))
            assertNotNull(db.grammarReviewDao().get("es", "c4-01"))

            val s1 = db.speakingDao().insert(answer("en", 1)); val s2 = db.speakingDao().insert(answer("en", 2)); val s3 = db.speakingDao().insert(answer("es", 3))
            assertEquals(1, deleteRecords(db, "en", RecordKind.SPEAKING, listOf(s1.toString(), s3.toString())))
            assertEquals(listOf(s2), db.speakingDao().all("en").map { it.id }); assertEquals(1, db.speakingDao().all("es").size)

            val v = db.shadowingAttemptDao().insert(ShadowingAttemptEntity(language = "en", videoId = "v", sentence = "s", heard = "h", matchRate = 0.5, createdAt = 1))
            assertEquals(1, deleteRecords(db, "en", RecordKind.SHADOWING, listOf(v.toString())))
            assertEquals(0, db.shadowingAttemptDao().all("en").size)

            // 많이 골라도(변수 제한) 나눠서 지운다
            val many = (10..1210).map { db.wordDao().insert(word("en", "w$it", it)) }
            many.forEach { db.userWordDao().insert(UserWordEntity(it, 0, 1, 5)) }
            assertEquals(1201, deleteRecords(db, "en", RecordKind.WORDS, many.map { it.toString() }))
        } finally {
            db.close()
        }
    }
}
