package com.jooh.opic.core.common

import org.junit.Assert.*
import org.junit.Test

class CaptionsTest {
    private fun fixture(name: String) = checkNotNull(javaClass.getResource("/captions/$name")).readText()
    @Test fun trackPriority() {
        val human = CaptionTrack("a", "en")
        val regional = CaptionTrack("b", "en-GB")
        val auto = CaptionTrack("c", "en", "asr")
        assertEquals(human, pickTrack(listOf(auto, regional, human)))
        assertEquals(regional, pickTrack(listOf(auto, regional)))
        assertEquals(auto, pickTrack(listOf(CaptionTrack("d", "ko"), auto)))
        assertNull(pickTrack(listOf(CaptionTrack("d", "ko"), CaptionTrack("e", "en-GB", "asr"))))
        assertNull(pickTrack(emptyList()))
    }
    @Test fun trackSelectionUsesRequestedLanguage() {
        val english = CaptionTrack("en", "en")
        val spanish = CaptionTrack("es", "es")
        assertEquals(spanish, pickTrack(listOf(english, spanish), "es"))
    }
    @Test fun tracksFromHtml() {
        val tracks = extractCaptionTracks(fixture("watch.html"))
        assertEquals(2, tracks.size)
        assertEquals("https://www.youtube.com/api/timedtext?v=AbC_123-xYz&lang=en&fmt=json3", tracks.first().baseUrl)
        assertEquals("asr", tracks.last().kind)
        assertTrue(extractCaptionTracks(fixture("no-captions.html")).isEmpty())
        assertTrue(extractCaptionTracks("\"captionTracks\": [broken").isEmpty())
    }
    @Test fun jsonTimingTextAndEmptyEvents() {
        val cues = parseJson3(fixture("sample.json"))
        assertEquals(listOf(Cue(1000, 2000, "Hello & welcome"), Cue(2000, 3000, "to our"),
            Cue(3000, 4000, "'home' \"today\".")), cues)
        assertTrue(parseJson3("").isEmpty())
        assertTrue(parseJson3("not json").isEmpty())
    }
    @Test fun sentenceMergingAndLimit() {
        val cues = parseJson3(fixture("sample.json"))
        assertEquals(listOf(Cue(1000, 4000, "Hello & welcome to our 'home' \"today\".")), mergeSentences(cues))
        val long = listOf(Cue(0, 8000, "one"), Cue(8000, 13000, "two"), Cue(13000, 14000, "three."))
        assertEquals(listOf(long[0], Cue(8000, 14000, "two three.")), mergeSentences(long))
        val complete = listOf(Cue(0, 1000, "One."), Cue(1000, 2000, "Two!"), Cue(2000, 3000, "Three?"))
        assertEquals(complete, mergeSentences(complete))
        assertTrue(mergeSentences(emptyList()).isEmpty())
    }
    @Test fun currentCueBoundaries() {
        val cues = listOf(Cue(1000, 2000, "a"), Cue(2000, 3000, "b"), Cue(4000, 5000, "c"))
        assertNull(cueAt(cues, 999))
        assertEquals(0, cueAt(cues, 1000))
        assertEquals(1, cueAt(cues, 2000))
        assertNull(cueAt(cues, 3000))
        assertEquals(2, cueAt(cues, 4999))
        assertNull(cueAt(cues, 5000))
        assertNull(cueAt(emptyList(), 0))
    }
}
