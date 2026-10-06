package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DayTest {
    @Test fun dayAndRange() {
        assertEquals(1, dayOf(1))
        assertEquals(1, dayOf(40))
        assertEquals(2, dayOf(41))
        assertEquals(138, dayOf(5517))
        assertEquals(138, totalDays(5517))
        assertEquals(0, totalDays(0))
        assertEquals(41..80, seqRange(2))
        assertThrows(IllegalArgumentException::class.java) { dayOf(0) }
    }
}
