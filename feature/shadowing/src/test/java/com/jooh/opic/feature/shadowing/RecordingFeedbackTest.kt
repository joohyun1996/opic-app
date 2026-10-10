package com.jooh.opic.feature.shadowing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingFeedbackTest {
    @Test fun shortRecordingBoundary() {
        assertTrue(recordingTooShort(47_999))
        assertFalse(recordingTooShort(48_000))
    }

    @Test fun silenceTagsAreRemovedWithoutDiscardingWords() {
        assertEquals("", cleanWhisperText("[silence] (BLANK_AUDIO)"))
        assertEquals("I like the park.", cleanWhisperText("[silence] I like the park. (silence)"))
    }
}
