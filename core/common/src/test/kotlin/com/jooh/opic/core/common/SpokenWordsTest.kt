package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpokenWordsTest {
    private val tokens = listOf(
        RawToken("[_BEG_]", 0, 0, 1f), RawToken(" I", 0, 200, 0.9f), RawToken(" li", 200, 350, 0.8f),
        RawToken("ke", 350, 500, 0.4f), RawToken(" it", 500, 700, 0.95f), RawToken(".", 700, 720, 0.99f),
    )

    @Test fun mergesTokensIntoWords() {
        val words = mergeTokens(tokens)
        assertEquals(listOf("I", "like", "it."), words.map { it.text })
        assertEquals(200L, words[1].startMs); assertEquals(500L, words[1].endMs)
        assertEquals(0.4f, words[1].confidence, 1e-6f)
        assertEquals(720L, words[2].endMs)
        assertEquals(emptyList<SpokenWord>(), mergeTokens(emptyList()))
    }

    @Test fun parsesTokenLines() {
        val parsed = parseTokenLines("0\t20\t0.9000\t I\nbad line\n20\t35\t0.5\t li")
        assertEquals(listOf(" I", " li"), parsed.map { it.text })
        assertEquals(200L, parsed[0].endMs)
    }

    @Test fun editsWords() {
        val base = editable(mergeTokens(tokens))
        val replaced = base.replaceAt(1, "liked")
        assertEquals(EditState.REPLACED, replaced[1].state)
        assertEquals("I liked it.", editedText(replaced))
        assertEquals(EditState.KEPT, replaced.replaceAt(1, "like")[1].state)
        val deleted = base.deleteAt(0)
        assertEquals("like it.", editedText(deleted))
        val inserted = base.insertAfter(1, "really")
        assertEquals("I like really it.", editedText(inserted))
        assertEquals(base, inserted.restoreAt(2))
        assertEquals(base, inserted.deleteAt(2))
        assertEquals(base, deleted.restoreAt(0))
        assertEquals(base, replaced.insertAfter(0, "do").deleteAt(2).restoreAll())
        assertEquals(base, base.replaceAt(9, "x"))
        assertEquals(base, base.insertAfter(9, "x"))
    }

    @Test fun playRangeUsesNeighbourForInsertedWord() {
        val inserted = editable(mergeTokens(tokens)).insertAfter(0, "do")
        assertEquals(0L..400L, playRange(inserted, 0))
        assertEquals(0L..400L, playRange(inserted, 1))
        assertNull(playRange(inserted, 9))
    }
}
