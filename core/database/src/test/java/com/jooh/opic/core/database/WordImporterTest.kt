package com.jooh.opic.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jooh.opic.core.common.WORDS_PER_DAY
import com.jooh.opic.core.common.seqRange
import com.jooh.opic.core.common.totalDays
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WordImporterTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), OpicDatabase::class.java).allowMainThreadQueries().build()
    private val importer = WordImporter(db)
    private val dao = db.wordDao()
    private val meta = db.dataMetaDao()
    @After fun close() = db.close()

    private fun word(text: String = "alpha", seq: Int = 1, language: String = "en", deleted: Boolean = false): JsonObject = buildJsonObject {
        put("language", language); put("word", text); put("seq", seq); put("level", 1)
        put("phonetic", "/a/"); put("meaningKo", "뜻"); put("meaningEn", "meaning")
        put("example", "An example."); put("exampleKo", "예문.")
        put("category", "noun"); put("partOfSpeech", "noun")
        put("collocations", JsonArray(emptyList())); put("deleted", deleted)
    }
    private fun file(version: Int, vararg words: JsonObject) = buildJsonObject {
        put("dataVersion", version); put("words", JsonArray(words.toList()))
    }.toString()
    private fun JsonObject.change(key: String, value: JsonElement) = JsonObject(toMutableMap().apply { put(key, value) })

    @Test fun actualFileAndRepeatAndLastDay() = runBlocking {
        val raw = File(requireNotNull(System.getProperty("words.file"))).readText()
        assertEquals(ImportResult.Imported(1, 5517), importer.importWords(raw))
        assertEquals(5517, dao.countByLanguage("en"))
        assertEquals("1", meta.get(WordImporter.VERSION_KEY))
        assertEquals(138, totalDays(dao.maxSeq("en")!!))
        assertEquals(37, dao.dayStats("en", WORDS_PER_DAY).last().total)
        val before = dao.getDayWords("en", 1, Int.MAX_VALUE)
        val sql = db.openHelper.writableDatabase
        sql.execSQL("CREATE TABLE write_audit (kind TEXT)")
        for (table in listOf("words", "data_meta")) for (operation in listOf("INSERT", "UPDATE", "DELETE")) {
            sql.execSQL("CREATE TRIGGER audit_${table}_$operation AFTER $operation ON $table BEGIN INSERT INTO write_audit VALUES ('$operation'); END")
        }
        assertEquals(ImportResult.UpToDate(1), importer.importWords(raw))
        assertEquals(before, dao.getDayWords("en", 1, Int.MAX_VALUE))
        sql.query("SELECT COUNT(*) FROM write_audit").use { assertTrue(it.moveToFirst()); assertEquals(0, it.getInt(0)) }
    }

    @Test fun upgradePreservesIdentitySequenceAndProgress() = runBlocking {
        importer.importWords(file(1, word()))
        val before = dao.find("en", "alpha")!!
        db.userWordDao().recordResult(before.id, true, 123)
        val progress = db.userWordDao().get(before.id)
        val updated = word(seq = 99).change("meaningKo", JsonPrimitive("바뀐 뜻"))
        assertEquals(ImportResult.Imported(2, 1), importer.importWords(file(2, updated)))
        assertEquals(before.copy(meaningKo = "바뀐 뜻"), dao.find("en", "alpha"))
        assertEquals(progress, db.userWordDao().get(before.id))
        assertEquals("2", meta.get(WordImporter.VERSION_KEY))
        assertEquals(ImportResult.UpToDate(2), importer.importWords(file(1, word())))
        assertEquals("바뀐 뜻", dao.find("en", "alpha")!!.meaningKo)
    }

    @Test fun collisionRollsBackEarlierUpdatesAndVersion() = runBlocking {
        importer.importWords(file(1, word()))
        val before = dao.find("en", "alpha")
        val update = word(seq = 2).change("meaningKo", JsonPrimitive("롤백될 뜻"))
        assertTrue(importer.importWords(file(2, update, word("beta", 1))) is ImportResult.Failed)
        assertEquals(before, dao.find("en", "alpha"))
        assertEquals(1, dao.countByLanguage("en"))
        assertEquals("1", meta.get(WordImporter.VERSION_KEY))
    }

    @Test fun invalidInputsDoNotWrite() = runBlocking {
        importer.importWords(file(1, word()))
        val before = dao.find("en", "alpha")
        val bad = listOf("{", file(2, word(), word(" ALPHA ", 2)), file(2, word(), word("beta")),
            file(2, word().change("example", JsonPrimitive(" "))), file(2, word().change("language", JsonPrimitive("ko"))),
            file(2, word().change("word", JsonPrimitive(" "))), file(2, word().change("meaningKo", JsonPrimitive(" "))),
            file(2, word(seq = 0)), file(2, word().change("level", JsonPrimitive(0))))
        for (raw in bad) {
            assertTrue(importer.importWords(raw) is ImportResult.Failed)
            assertEquals(before, dao.find("en", "alpha"))
            assertEquals(1, dao.countByLanguage("en"))
            assertEquals("1", meta.get(WordImporter.VERSION_KEY))
        }
    }

    @Test fun dayStatisticsAndExplicitRange() = runBlocking {
        importer.importWords(file(1, word("later", 80), word("first", 41), word("hidden", 42, deleted = true),
            word("next", 81), word("previous", 40), word("chinese", 41, "zh")))
        val first = dao.find("en", "first")!!
        repeat(3) { db.userWordDao().recordResult(first.id, true, it.toLong()) }
        db.userWordDao().recordResult(first.id, false, 4)
        val hidden = dao.find("en", "hidden")!!
        repeat(3) { db.userWordDao().recordResult(hidden.id, true, it.toLong()) }
        db.userWordDao().recordResult(hidden.id, false, 4)
        val range = seqRange(2)
        assertEquals(listOf(41, 80), dao.getDayWords("en", range.first, range.last).map { it.seq })
        assertEquals(DayStats(2, 2, 1, 0), dao.dayStats("en", WORDS_PER_DAY).single { it.day == 2 })
        assertEquals(listOf(DayStats(2, 1, 0, 0)), dao.dayStats("zh", WORDS_PER_DAY))
    }

    @Test fun sameVersionSkipsFullParse() = runBlocking {
        importer.importWords(file(1, word()))
        // dataVersion 1이 이미 적재된 상태에서, 단어 목록이 깨진 같은 버전 파일은 파싱 없이 UpToDate다.
        assertEquals(ImportResult.UpToDate(1), importer.importWords("{\"dataVersion\": 1, \"words\": [ not json"))
        assertEquals(1, dao.countByLanguage("en"))
    }

    @Test fun correctionTurnsLastWrongIntoCorrect() = runBlocking {
        importer.importWords(file(1, word()))
        val id = dao.find("en", "alpha")!!.id
        val progress = db.userWordDao()
        progress.recordResult(id, false, 1)
        assertTrue(progress.correctLastWrong(id, 2))
        val row = progress.get(id)!!
        assertEquals(1, row.correctCount)
        assertEquals(0, row.wrongCount)
        assertEquals(false, progress.correctLastWrong(id, 3))
        assertEquals(row, progress.get(id))
        assertEquals(false, progress.correctLastWrong(999, 3))
    }

    @Test fun dayWordsWithProgressSkipsDeletedAndMarksNew() = runBlocking {
        importer.importWords(file(1, word("first", 1), word("hidden", 2, deleted = true), word("third", 3), word("chinese", 1, "zh")))
        val first = dao.find("en", "first")!!
        repeat(3) { db.userWordDao().recordResult(first.id, true, it.toLong()) }
        val rows = dao.getDayWordsWithProgress("en", 1, 40)
        assertEquals(listOf("first", "third"), rows.map { it.word.word })
        assertEquals(3, rows[0].correctCount)
        assertEquals(null, rows[1].correctCount)
        assertEquals(DayStats(1, 2, 1, 0), dao.dayStats("en", WORDS_PER_DAY).single())
    }

    @Test fun wrongWordsExcludeMasteredDeletedAndOtherLanguage() = runBlocking {
        importer.importWords(file(1, word("wrong", 1), word("mastered", 2), word("right", 3), word("hidden", 4, deleted = true),
            word("later", 41), word("chinese", 1, "zh")))
        val progress = db.userWordDao()
        suspend fun record(text: String, language: String = "en", correct: Int = 0, wrong: Int = 0) {
            val id = dao.find(language, text)!!.id
            repeat(correct) { progress.recordResult(id, true, 0) }
            repeat(wrong) { progress.recordResult(id, false, 0) }
        }
        record("wrong", wrong = 1); record("mastered", correct = 3, wrong = 1); record("right", correct = 1)
        record("hidden", wrong = 1); record("later", wrong = 2); record("chinese", "zh", wrong = 1)
        assertEquals(listOf("wrong", "later"), dao.getWrongWords("en", 1, Int.MAX_VALUE).map { it.word.word })
        val day1 = seqRange(1)
        assertEquals(listOf("wrong"), dao.getWrongWords("en", day1.first, day1.last).map { it.word.word })
        assertEquals(listOf(1, 1), dao.dayStats("en", WORDS_PER_DAY).map { it.wrong })
        assertEquals(listOf("chinese"), dao.getWrongWords("zh", 1, Int.MAX_VALUE).map { it.word.word })
    }
}
