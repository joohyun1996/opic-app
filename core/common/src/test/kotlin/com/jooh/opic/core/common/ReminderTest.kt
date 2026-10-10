package com.jooh.opic.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderTest {
    private val nine = LocalTime.of(21, 0)

    @Test fun nextReminderIsTodayOrTomorrow() {
        assertEquals(LocalDateTime.of(2026, 10, 10, 21, 0), nextReminderAt(LocalDateTime.of(2026, 10, 10, 20, 0), nine))
        assertEquals(LocalDateTime.of(2026, 10, 11, 21, 0), nextReminderAt(LocalDateTime.of(2026, 10, 10, 22, 0), nine))
        assertEquals(LocalDateTime.of(2026, 10, 11, 21, 0), nextReminderAt(LocalDateTime.of(2026, 10, 10, 21, 0), nine))
    }

    @Test fun textSkipsWhenStudiedAndFallsBackWhenNothingDue() {
        assertNull(reminderText(studiedToday = true, nextDay = 3, grammarDue = 2))
        assertEquals("오늘도 5분만 해 볼까요?", reminderText(false, null, 0))
        assertEquals("오늘 할 일: 영단어 Day 3 · 문법 복습 2문제", reminderText(false, 3, 2))
        assertEquals("오늘 할 일: 영단어 Day 3", reminderText(false, 3, 0))
    }
}
