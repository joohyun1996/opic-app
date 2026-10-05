package com.jooh.opic.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.jooh.opic.core.model.WordStatus
import com.jooh.opic.core.model.wordStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DaoTest {
    private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), OpicDatabase::class.java).build()
    private val words = db.wordDao()
    private val progress = db.userWordDao()

    @After fun close() = db.close()

    private fun word(language: String, text: String, seq: Int, deleted: Boolean = false) = WordEntity(
        language = language, word = text, seq = seq, phonetic = "/test/", meaningKo = "뜻",
        meaningEn = "meaning", example = "A test.", exampleKo = "시험.", level = 1,
        category = "noun", partOfSpeech = "noun", collocations = listOf("test word"), deleted = deleted,
    )

    @Test fun dayFiltersLanguageDeletionAndOrder() = runBlocking {
        words.upsertWords(listOf(word("en", "second", 2), word("en", "first", 1), word("en", "hidden", 3, true), word("zh", "other", 4), word("en", "next", 41)))
        assertEquals(listOf("first", "second"), words.getDayWords("en", 1).map { it.word })
        assertEquals(listOf("other"), words.getDayWords("zh", 1).map { it.word })
        assertEquals(41, words.maxSeq("en"))
    }

    @Test fun upsertPreservesIdentitySequenceAndLearning() = runBlocking {
        words.upsertWords(listOf(word("en", "abc", 1)))
        val original = words.find("en", "abc")!!
        progress.recordResult(original.id, true, 100)
        words.upsertWords(listOf(word("en", "ABC", 20).copy(meaningKo = "새 뜻")))
        val updated = words.find("en", "abc")!!
        assertEquals(original.id, updated.id)
        assertEquals(1, updated.seq)
        assertEquals("새 뜻", updated.meaningKo)
        assertEquals(1, progress.get(updated.id)?.correctCount)
        words.upsertWords(listOf(word("en", "new", 2)))
        val inserted = words.find("en", "new")!!
        words.upsertWords(listOf(word("en", "new", 2)))
        assertEquals(inserted.id, words.find("en", "new")?.id)
        assertEquals(2, words.countByLanguage("en"))
    }

    @Test fun recordResultTracksStatusAndErrors() = runBlocking {
        words.upsertWords(listOf(word("en", "abc", 1)))
        val id = words.find("en", "abc")!!.id
        assertEquals(WordStatus.NEW, wordStatus(progress.get(id)?.correctCount))
        repeat(3) { progress.recordResult(id, true, it.toLong()) }
        progress.recordResult(id, false, 4)
        val learned = progress.get(id)
        assertNotNull(learned)
        assertEquals(WordStatus.MASTERED, wordStatus(learned!!.correctCount))
        assertEquals(1, learned.wrongCount)
    }
}
