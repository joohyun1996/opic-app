package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashcardTest {
    @Test fun meaningMatchesAnyCandidate() {
        assertTrue(gradeMeaning("큰", "주요한; 큰"))
        assertTrue(gradeMeaning("주요한", "주요한; 큰"))
        assertFalse(gradeMeaning("주요", "주요한; 큰"))
        assertTrue(gradeMeaning("가하다", "(피해를) 가하다"))
        assertFalse(gradeMeaning("피해를 가하다", "(피해를) 가하다"))
        assertTrue(gradeMeaning("꾸밈 없는", "영향받지 않은; 꾸밈없는"))
        assertTrue(gradeMeaning("영향받지 않은", "영향받지 않은; 꾸밈없는"))
    }

    @Test fun meaningWithAsteriskAndCommasAndBlank() {
        assertTrue(gradeMeaning("계기", "*계기 (gauge의 변형 철자)"))
        assertTrue(gradeMeaning("동정", "동정; 공감"))
        assertTrue(gradeMeaning("인공 유물", "인공 유물"))
        assertTrue(gradeMeaning("강점", "장점, 강점"))
        assertFalse(gradeMeaning("   ", "큰"))
        assertFalse(gradeMeaning("", "큰"))
    }

    @Test fun wordIsTrimmedAndCaseInsensitive() {
        assertTrue(gradeWord("  Contract ", "contract"))
        assertFalse(gradeWord("contracts", "contract"))
    }

    @Test fun hints() {
        assertEquals("c _ _ _ _ _ _ t", maskHint("contract"))
        assertEquals("o _", maskHint("ox"))
        assertEquals("a", maskHint("a"))
        assertEquals("w _ _ _ - _ _ _ _ g", maskHint("well-being"))
    }
}
