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

    @Test fun rejectsOtherLanguageKindAndHost() {
        assertNull(captionRequest("https://www.youtube.com/api/timedtext?lang=ko"))
        assertNull(captionRequest("https://www.youtube.com/api/timedtext?lang=en&kind=forced"))
        assertNull(captionRequest("https://evil.example/api/timedtext?lang=en"))
        assertNull(captionRequest("https://www.youtube.com/watch?lang=en"))
        assertNull(captionRequest("http://www.youtube.com/api/timedtext?lang=en"))
    }
}
