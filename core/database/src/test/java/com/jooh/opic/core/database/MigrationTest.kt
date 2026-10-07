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
    private fun createV1(file: File) {
        val schema = Json.parseToJsonElement(File(requireNotNull(System.getProperty("schema.v1"))).readText())
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
            db.version = 1
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
            assertNull(db.grammarReviewDao().get("u1-01"))
            db.grammarReviewDao().recordPractice("u1-01", "present-simple-continuous", today = 100, now = 1)
            assertEquals(1, db.grammarReviewDao().due(101).size)
        } finally {
            db.close()
        }
    }

    @Test fun reviewLifecycle() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, OpicDatabase::class.java).build()
        val dao = db.grammarReviewDao()
        try {
            dao.recordPractice("u2-01", "past-simple", today = 100, now = 1)
            assertEquals(0, dao.due(100).size)
            assertEquals(1, dao.due(101).size)
            dao.recordReview("u2-01", firstTryCorrect = true, today = 101, now = 2)
            assertEquals(2, dao.get("u2-01")!!.stage)
            assertEquals(104L, dao.get("u2-01")!!.dueEpochDay)
            dao.recordReview("u2-01", firstTryCorrect = false, today = 104, now = 3)
            assertEquals(1, dao.get("u2-01")!!.stage)
            assertEquals(2, dao.get("u2-01")!!.wrongCount)
            dao.recordReview("u2-01", true, 105, 4); dao.recordReview("u2-01", true, 108, 5)
            assertEquals(3, dao.get("u2-01")!!.stage)
            dao.recordReview("u2-01", true, 115, 6)
            assertNull(dao.get("u2-01"))
            dao.recordReview("unknown", true, 115, 7)
            assertNull(dao.get("unknown"))
        } finally {
            db.close()
        }
    }
}
