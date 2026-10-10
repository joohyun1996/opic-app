package com.jooh.opic.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** 내보낸 v1 스키마(schemas/…/1.json)로 실제 v1 DB 파일을 만든다. */
    private fun createV1(file: File) = create(file, "schema.v1", 1)

    /** 내보낸 스키마(schemas/…/N.json)로 실제 vN DB 파일을 만들고 단어 기록 1개를 넣는다. */
    private fun create(file: File, property: String, version: Int, extra: (SQLiteDatabase) -> Unit = {}) {
        val schema = Json.parseToJsonElement(File(requireNotNull(System.getProperty(property))).readText())
            .jsonObject["database"]!!.jsonObject
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            for (entity in schema["entities"]!!.jsonArray) {
                val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
                db.execSQL(entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                entity.jsonObject["indices"]?.jsonArray?.forEach {
                    db.execSQL(it.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            schema["setupQueries"]!!.jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            db.execSQL("INSERT INTO words (id, language, word, seq, phonetic, meaningKo, meaningEn, example, exampleKo, level, category, partOfSpeech, collocations, deleted) " +
                "VALUES (7, 'en', 'archipelago', 1, '/a/', '군도', 'islands', 'An example.', '예문.', 1, 'noun', 'noun', '[]', 0)")
            db.execSQL("INSERT INTO user_words (wordId, correctCount, wrongCount, lastStudiedAt) VALUES (7, 2, 1, 123)")
            db.execSQL("INSERT INTO data_meta (`key`, value) VALUES ('words_data_version', '1')")
            extra(db)
            db.version = version
        }
    }

    @Test fun v1ToV2KeepsWordProgressAndAddsReviewTable() = runBlocking {
        val file = context.getDatabasePath("migration-test.db").also { it.parentFile?.mkdirs(); it.delete() }
        createV1(file)
        val db = Room.databaseBuilder(context, OpicDatabase::class.java, file.absolutePath).addMigrations(*ALL_MIGRATIONS).build()
        try {
            val word = db.wordDao().find("en", "archipelago")!!
            assertEquals(7L, word.id)
            val progress = db.userWordDao().get(7)!!
            assertEquals(2, progress.correctCount)
            assertEquals(1, progress.wrongCount)
            assertEquals("1", db.dataMetaDao().get(WordImporter.VERSION_KEY))
            assertNull(db.grammarReviewDao().get("en", "u1-01"))
            db.grammarReviewDao().recordPractice("en", "u1-01", "present-simple-continuous", today = 100, now = 1)
            assertEquals(1, db.grammarReviewDao().due("en", 101).size)
        } finally {
            db.close()
        }
    }

    @Test fun v2ToV3KeepsProgressAndReviewsAndAddsHistoryTables() = runBlocking {
        val file = context.getDatabasePath("migration-v2-test.db").also { it.parentFile?.mkdirs(); it.delete() }
        create(file, "schema.v2", 2) { db ->
            db.execSQL("INSERT INTO grammar_reviews (exerciseId, unitId, stage, dueEpochDay, wrongCount, lastStudiedAt) VALUES ('u1-04', 'present', 2, 300, 1, 99)")
        }
        val db = Room.databaseBuilder(context, OpicDatabase::class.java, file.absolutePath).addMigrations(*ALL_MIGRATIONS).build()
        try {
            assertEquals(2, db.userWordDao().get(7)!!.correctCount)
            assertEquals(2, db.grammarReviewDao().get("en", "u1-04")!!.stage)
            val id = db.speakingDao().insert(SpeakingAnswerEntity(language = "en", questionId = "home-1", topicId = "home", createdAt = 10, durationMs = 60_000,
                transcript = "I live here.", editedText = "I live here.", wordCount = 3, wordsPerMinute = 3, fillerCount = 0, sentenceCount = 1))
            db.speakingDao().updateEdit("en", id, "I lived here.", 3, 3, 0, 1)
            assertEquals("I lived here.", db.speakingDao().byQuestion("en", "home-1").single().editedText)
            assertEquals(emptyList<SpeakingAnswerEntity>(), db.speakingDao().byQuestion("zh", "home-1"))
            db.shadowingAttemptDao().insert(ShadowingAttemptEntity(language = "en", videoId = "aircAruvnKk", sentence = "a", heard = "a", matchRate = 0.8, createdAt = 5))
            assertEquals(1, db.shadowingAttemptDao().recentVideos("en").single().attempts)
        } finally {
            db.close()
        }
    }

    @Test fun v3ToV4MovesReviewsToEnglishAndSeparatesLanguages() = runBlocking {
        val file = context.getDatabasePath("migration-v3-test.db").also { it.parentFile?.mkdirs(); it.delete() }
        create(file, "schema.v3", 3) { db ->
            db.execSQL("INSERT INTO grammar_reviews (exerciseId, unitId, stage, dueEpochDay, wrongCount, lastStudiedAt) VALUES ('c4-01', 'c4', 2, 300, 3, 99)")
        }
        val db = Room.databaseBuilder(context, OpicDatabase::class.java, file.absolutePath).addMigrations(*ALL_MIGRATIONS).build()
        try {
            assertEquals(2, db.userWordDao().get(7)!!.correctCount)
            val moved = db.grammarReviewDao().get("en", "c4-01")!!
            assertEquals(3, moved.wrongCount); assertEquals("c4", moved.unitId)
            assertNull(db.grammarReviewDao().get("es", "c4-01"))
            db.grammarReviewDao().recordPractice("es", "c4-01", "c4", today = 100, now = 1)
            assertEquals(1, db.grammarReviewDao().all("es").size)
            assertEquals(3, db.grammarReviewDao().get("en", "c4-01")!!.wrongCount)
        } finally {
            db.close()
        }
    }

    @Test fun reviewLifecycle() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, OpicDatabase::class.java).build()
        val dao = db.grammarReviewDao()
        try {
            dao.recordPractice("en", "u2-01", "past-simple", today = 100, now = 1)
            assertEquals(0, dao.due("en", 100).size)
            assertEquals(1, dao.due("en", 101).size)
            dao.recordReview("en", "u2-01", firstTryCorrect = true, today = 101, now = 2)
            assertEquals(2, dao.get("en", "u2-01")!!.stage)
            assertEquals(104L, dao.get("en", "u2-01")!!.dueEpochDay)
            dao.recordReview("en", "u2-01", firstTryCorrect = false, today = 104, now = 3)
            assertEquals(1, dao.get("en", "u2-01")!!.stage)
            assertEquals(2, dao.get("en", "u2-01")!!.wrongCount)
            dao.recordReview("en", "u2-01", true, 105, 4); dao.recordReview("en", "u2-01", true, 108, 5)
            assertEquals(3, dao.get("en", "u2-01")!!.stage)
            dao.recordReview("en", "u2-01", true, 115, 6)
            assertNull(dao.get("en", "u2-01"))
            dao.recordReview("en", "unknown", true, 115, 7)
            assertNull(dao.get("en", "unknown"))
        } finally {
            db.close()
        }
    }
}
