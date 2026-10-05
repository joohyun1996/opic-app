package com.jooh.opic.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class WordStatusTest {
    @Test fun status() {
        assertEquals(WordStatus.NEW, wordStatus(null))
        assertEquals(WordStatus.LEARNING, wordStatus(2))
        assertEquals(WordStatus.MASTERED, wordStatus(3))
    }
}
