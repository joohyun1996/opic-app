package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class WerTest {
    private val ref = "I usually walk my dog in the evening."

    @Test fun identicalIgnoringCaseAndPunctuation() {
        assertEquals(0.0, wordErrorRate(ref, "i usually walk my dog in the evening"), 1e-9)
        assertEquals(0.0, wordErrorRate("I don't know.", "I don’t know"), 1e-9)
    }

    @Test fun substitutionInsertionDeletion() {
        assertEquals(1.0 / 8, wordErrorRate(ref, "I usually walked my dog in the evening"), 1e-9) // 치환
        assertEquals(1.0 / 8, wordErrorRate(ref, "I usually walk my little dog in the evening"), 1e-9) // 삽입
        assertEquals(1.0 / 8, wordErrorRate(ref, "I walk my dog in the evening"), 1e-9) // 삭제
    }

    @Test fun emptyCases() {
        assertEquals(0.0, wordErrorRate("", ""), 1e-9)
        assertEquals(1.0, wordErrorRate("", "hello"), 1e-9)
        assertEquals(1.0, wordErrorRate("hello there", ""), 1e-9)
    }
}
