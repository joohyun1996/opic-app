package com.jooh.opic.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AffixImporterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private fun word(name: String) = WordEntity(language = "en", word = name, seq = if (name == "similar") 1 else 2,
        phonetic = "", meaningKo = if (name == "similar") "비슷한" else "비슷하지 않은", meaningEn = "", example = "", exampleKo = "",
        level = 1, category = "", partOfSpeech = "")
    private val seed = """{"dataVersion":1,"affixes":[{"language":"en","form":"dis-","type":"prefix","meaningKo":"아닌"}],"words":[{"language":"en","word":"dissimilar","base":"similar","affix":"dis-","type":"prefix","glossKo":"비슷하지 않은","transparent":true}]}"""

    @Test fun importsOnlyValidatedPartsAndPreservesUserProgressOnReload() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, OpicDatabase::class.java).build()
        try {
            val baseId = db.wordDao().insert(word("similar"))
            val wordId = db.wordDao().insert(word("dissimilar"))
            db.userWordDao().recordResult(wordId, true, 123)
            val before = db.userWordDao().get(wordId)
            assertEquals(ImportResult.Imported(1, 1), AffixImporter(db).importAffixes(seed))
            val parts = db.affixDao().partsForWord("en", wordId)
            assertEquals(2, parts.size)
            assertEquals("dis-", parts[0].label)
            assertEquals("비슷하지 않은", parts[0].glossKo)
            assertEquals(baseId, parts[1].baseWordId)
            assertEquals(emptyList<AffixPartRow>(), db.affixDao().partsForWord("zh", wordId))
            assertEquals(1, db.affixDao().wordsWithAffix("en", parts[0].affixId!!).size)
            assertEquals(1, db.affixDao().wordsWithBase("en", baseId).size)
            assertEquals(emptyList<WordEntity>(), db.affixDao().wordsWithAffix("zh", parts[0].affixId!!))
            assertEquals(ImportResult.UpToDate(1), AffixImporter(db).importAffixes(seed))
            // 같은 버전이면 스트림 앞부분만 보고 끝낸다 (뒤가 깨진 JSON이어도 UpToDate).
            val broken = "{\"dataVersion\": 1, \"affixes\": [ not json"
            assertEquals(ImportResult.UpToDate(1), AffixImporter(db).importAffixes(broken.byteInputStream()))
            assertEquals(before, db.userWordDao().get(wordId))
        } finally { db.close() }
    }

    @Test fun bundledSeedContainsOnlyThe426ApprovedWords() {
        val file = File(requireNotNull(System.getProperty("words.file"))).resolveSibling("affixes.json")
        val entries = Json.parseToJsonElement(file.readText()).jsonObject["words"]!!.jsonArray
        assertEquals(426, entries.size)
        assertTrue(entries.all { it.jsonObject["transparent"]!!.jsonPrimitive.content == "true" })
        assertTrue(entries.none { it.jsonObject["word"]!!.jsonPrimitive.content in setOf("cavity", "cubic", "gallant", "infraction", "undermine") })
    }
}
