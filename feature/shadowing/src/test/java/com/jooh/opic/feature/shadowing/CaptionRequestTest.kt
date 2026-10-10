package com.jooh.opic.feature.shadowing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptionRequestTest {
    @Test fun englishHumanAndAsr() {
        val human = captionRequest("https://www.youtube.com/api/timedtext?v=abc&lang=en&fmt=srv3")
        assertEquals(null, human?.kind)
        assertEquals("https://www.youtube.com/api/timedtext?v=abc&lang=en&fmt=json3", human?.jsonUrl)
        val asr = captionRequest("https://www.youtube.com/api/timedtext?lang=en&kind=asr&fmt=json3")
        assertEquals("asr", asr?.kind)
    }

    @Test fun englishVariants() {
        listOf("en", "en-GB", "en-US").forEach { assertEquals(it, null, captionRequest("https://www.youtube.com/api/timedtext?v=a&lang=$it&fmt=json3")?.kind) }
        listOf("en-GB", "en-US").forEach { assertEquals("https://www.youtube.com/api/timedtext?v=a&lang=$it&fmt=json3", captionRequest("https://www.youtube.com/api/timedtext?v=a&lang=$it")?.jsonUrl) }
        assertEquals("asr", captionRequest("https://www.youtube.com/api/timedtext?lang=en-GB&kind=asr")?.kind)
        assertNull(captionRequest("https://www.youtube.com/api/timedtext?lang=es"))
        assertNull(captionRequest("https://www.youtube.com/api/timedtext?lang=english"))
        assertNull(captionRequest("https://www.youtube.com/api/timedtext?v=a"))
    }

    @Test fun rejectsOtherLanguageKindAndHost() {
        assertNull(captionRequest("https://www.youtube.com/api/timedtext?lang=ko"))
        assertNull(captionRequest("https://www.youtube.com/api/timedtext?lang=en&kind=forced"))
        assertNull(captionRequest("https://evil.example/api/timedtext?lang=en"))
        assertNull(captionRequest("https://www.youtube.com/watch?lang=en"))
        assertNull(captionRequest("http://www.youtube.com/api/timedtext?lang=en"))
    }
}
