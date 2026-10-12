package com.jooh.opic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jooh.opic.core.common.StudyLanguages
import com.jooh.opic.core.database.AffixImporter
import com.jooh.opic.core.database.OpicDatabase
import com.jooh.opic.core.database.WordEntity
import com.jooh.opic.feature.words.StudyMode
import com.jooh.opic.feature.words.StudySource
import com.jooh.opic.feature.words.StudyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AffixStudyStateTest {
    private lateinit var database: OpicDatabase

    @Before fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), OpicDatabase::class.java).build()
    }

    @After fun cleanup() { database.close(); Dispatchers.resetMain() }

    @Test fun breakdownIsHiddenUntilCheckAndClearedOnNextCard() = runBlocking {
        fun word(name: String, seq: Int) = WordEntity(language = "en", word = name, seq = seq, phonetic = "", meaningKo = "비슷한",
            meaningEn = "", example = "", exampleKo = "", level = 1, category = "", partOfSpeech = "")
        database.wordDao().insert(word("similar", 1))
        database.wordDao().insert(word("dissimilar", 2))
        val seed = """{"dataVersion":1,"affixes":[{"language":"en","form":"dis-","type":"prefix","meaningKo":"아닌"}],"words":[{"language":"en","word":"dissimilar","base":"similar","affix":"dis-","type":"prefix","glossKo":"비슷하지 않은","transparent":true}]}"""
        AffixImporter(database).importAffixes(seed)
        val model = StudyViewModel(database, StudyLanguages.EN, StudySource(1, false), StudyMode.EN_KO, {})
        val loaded = withTimeout(5_000) { model.state.first { !it.loading } }
        assertTrue(loaded.breakdown.isEmpty())
        model.showHint()
        assertTrue(model.state.value.breakdown.isEmpty())
        // 카드 순서는 섞이므로 검수된 단어가 나올 때까지 최대 두 장만 확인한다.
        if (model.state.value.current?.word != "dissimilar") {
            model.setInput("x")
            model.check()
            model.next()
        }
        assertEquals("dissimilar", model.state.value.current?.word)
        assertEquals(null, model.state.value.checked)
        assertTrue(model.state.value.breakdown.isEmpty())
        model.setInput("x")
        model.check()
        val checked = withTimeout(5_000) { model.state.first { it.breakdown.size == 2 } }
        assertEquals(false, checked.checked)
        assertEquals("dis-", checked.breakdown.first().label)
        model.next()
        assertEquals(null, model.state.value.checked)
        assertTrue(model.state.value.breakdown.isEmpty())
    }
}
