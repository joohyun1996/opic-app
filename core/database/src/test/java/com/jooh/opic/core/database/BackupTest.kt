package com.jooh.opic.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private suspend fun dbWithWords(): OpicDatabase {
        val db = Room.inMemoryDatabaseBuilder(context, OpicDatabase::class.java).build()
        listOf("apple", "brave").forEachIndexed { i, w ->
            db.wordDao().insert(WordEntity(language = "en", word = w, seq = i + 1, phonetic = "", meaningKo = "", meaningEn = "", example = "",
                exampleKo = "", level = 1, category = "", partOfSpeech = ""))
        }
        return db
    }

    @Test fun exportThenRestoreIntoFreshDatabase() = runBlocking {
        val source = dbWithWords()
        val target = dbWithWords()
        try {
            val apple = source.wordDao().find("en", "apple")!!
            source.userWordDao().insert(UserWordEntity(apple.id, 3, 1, 500))
            source.grammarReviewDao().recordPractice("en", "u1-04", "present", today = 100, now = 50)
            source.speakingDao().insert(SpeakingAnswerEntity(questionId = "home-1", topicId = "home", createdAt = 10, durationMs = 60_000,
                transcript = "t", editedText = "e", wordCount = 1, wordsPerMinute = 1, fillerCount = 0, sentenceCount = 1, mockId = 9))
            source.shadowingAttemptDao().insert(ShadowingAttemptEntity(videoId = "aircAruvnKk", sentence = "s", heard = "h", matchRate = 0.5, createdAt = 5))
            val raw = BackupManager(source).export(now = 1)

            val first = BackupManager(target).restore(raw)!!
            assertEquals(RestoreResult(1, 0, 1, 1, 1), first)
            val targetApple = target.wordDao().find("en", "apple")!!
            assertEquals(3, target.userWordDao().get(targetApple.id)!!.correctCount)
            assertEquals(1, target.grammarReviewDao().get("en", "u1-04")!!.stage)
            assertEquals(9L, target.speakingDao().all("en").single().mockId)
            assertEquals(1, target.shadowingAttemptDao().all("en").size)

            // 두 번 복원해도 늘지 않는다
            assertEquals(RestoreResult(0, 0, 0, 0, 0), BackupManager(target).restore(raw))
            assertEquals(1, target.speakingDao().all("en").size)

            // 더 최근 기록은 덮어쓰지 않는다
            target.userWordDao().update(UserWordEntity(targetApple.id, 9, 9, 900))
            BackupManager(target).restore(raw)
            assertEquals(9, target.userWordDao().get(targetApple.id)!!.correctCount)
        } finally { source.close(); target.close() }
    }

    @Test fun unknownWordsAreSkippedAndBadJsonChangesNothing() = runBlocking {
        val db = dbWithWords()
        try {
            val raw = """{"backupVersion":1,"exportedAt":1,"userWords":[{"language":"en","word":"zebra","correctCount":1,"wrongCount":0,"lastStudiedAt":5}]}"""
            assertEquals(1, BackupManager(db).restore(raw)!!.skippedWords)
            assertNull(BackupManager(db).restore("not json"))
            assertNull(BackupManager(db).restore("""{"backupVersion":2,"exportedAt":1}"""))
            assertEquals(0, db.userWordDao().backupRows("en").size)
        } finally { db.close() }
    }
}
