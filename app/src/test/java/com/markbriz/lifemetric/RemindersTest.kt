package com.markbriz.lifemetric

import com.markbriz.lifemetric.reminders.nextTriggerMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RemindersTest {
    @Test
    fun nextTriggerUsesTodayWhenTheClockIsStillAhead() {
        val now = LocalDateTime.of(2026, 10, 27, 6, 0)
        val expected = now.toLocalDate().atTime(7, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, nextTriggerMillis("07:00", now))
    }

    @Test
    fun nextTriggerRollsToTomorrowAfterTheWindow() {
        val now = LocalDateTime.of(2026, 10, 27, 7, 0, 20)
        val expected = now.toLocalDate().plusDays(1).atTime(7, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals(expected, nextTriggerMillis("07:00", now))
    }

    @Test
    fun nextTriggerRejectsBadTimes() {
        assertNull(nextTriggerMillis("not-a-time", LocalDateTime.of(2026, 10, 27, 6, 0)))
    }
}
