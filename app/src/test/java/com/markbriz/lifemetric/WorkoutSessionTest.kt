package com.markbriz.lifemetric

import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.domain.draftHasDetails
import com.markbriz.lifemetric.domain.formatPlanLastDone
import com.markbriz.lifemetric.domain.formatRestTimerSetting
import com.markbriz.lifemetric.domain.formatSessionDetailsLine
import com.markbriz.lifemetric.domain.sessionActivities
import com.markbriz.lifemetric.domain.sessionPrefillEntry
import com.markbriz.lifemetric.domain.validateSessionDetails
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionTest {
    private val plan = FitnessPlan(
        id = "p1",
        name = "Push",
        activityTypeId = "strength",
        category = "Strength",
        machines = listOf(Machine("bench", "Bench press"), Machine("row", "Row"), Machine("curl", "Curl")),
    )

    private fun set(id: String, machineId: String, weight: Double? = 100.0, reps: Int? = 8) =
        WorkoutEntry(id = id, planId = "p1", machineId = machineId, weight = weight, repetitions = reps)

    @Test
    fun sessionActivitiesPutsLoggedFirstThenPlanOrder() {
        val entries = listOf(set("1", "curl"), set("2", "bench"), set("3", "curl"))
        val cards = sessionActivities(plan, entries)
        assertEquals(listOf("curl", "bench", "row"), cards.map { it.machineId })
        assertEquals(2, cards[0].entries.size)
        assertTrue(cards[2].entries.isEmpty())
        assertTrue(cards.all { it.onPlan })
    }

    @Test
    fun sessionActivitiesKeepsEntriesForRemovedActivities() {
        val cards = sessionActivities(plan, listOf(set("1", "gone")))
        assertEquals("Unknown activity", cards.first().name)
        assertFalse(cards.first().onPlan)
        assertEquals(4, cards.size)
    }

    @Test
    fun sessionActivitiesWithoutPlanShowsOnlyLogged() {
        assertTrue(sessionActivities(null, emptyList()).isEmpty())
    }

    @Test
    fun prefillUsesLatestRealSetForThatActivity() {
        val entries = listOf(
            set("1", "bench", 100.0, 8),
            set("2", "row", 80.0, 10),
            set("3", "bench", 110.0, 6),
            set("4", "bench", null, null),
        )
        assertEquals("3", sessionPrefillEntry(entries, "bench", "load")?.id)
        assertEquals("2", sessionPrefillEntry(entries, "row", "load")?.id)
        assertNull(sessionPrefillEntry(entries, "curl", "load"))
        assertNull(sessionPrefillEntry(entries, "bench", "duration"))
    }

    @Test
    fun draftHasDetailsOnlyWhenTypedDetailsWouldBeLost() {
        val empty = Workout("w", "2026-10-28", "p1", "strength")
        assertFalse(draftHasDetails(empty))
        assertTrue(draftHasDetails(empty.copy(note = "Felt strong")))
        assertTrue(draftHasDetails(empty.copy(durationSec = 1800)))
        assertFalse(draftHasDetails(empty.copy(note = "   ")))
        assertFalse(draftHasDetails(empty.copy(note = "x", entries = listOf(set("1", "bench")))))
    }

    @Test
    fun validateSessionDetailsAcceptsBlankAndParsesDuration() {
        val blank = validateSessionDetails("", "  note ")
        assertNull(blank.error)
        assertNull(blank.durationSec)
        assertEquals("note", blank.note)
        assertEquals(45 * 60, validateSessionDetails("45", "").durationSec)
        assertEquals(3600 + 5 * 60, validateSessionDetails("1:05:00", "").durationSec)
        assertEquals("Enter minutes, mm:ss, or h:mm:ss.", validateSessionDetails("abc", "").error)
    }

    @Test
    fun planLastDoneLabels() {
        val today = LocalDate.now()
        val workouts = listOf(
            Workout("a", today.minusDays(3).toString(), "p1", "strength", listOf(set("1", "bench"))),
            Workout("b", today.toString(), "p2", "strength"),
        )
        assertEquals("Last done 3 days ago", formatPlanLastDone(workouts, "p1"))
        assertEquals("Not logged yet", formatPlanLastDone(workouts, "p2"))
        val withToday = workouts + Workout("c", today.toString(), "p1", "strength", listOf(set("2", "row")))
        assertEquals("Last done today", formatPlanLastDone(withToday, "p1"))
    }

    @Test
    fun sessionDetailsLinePromptsForMissingDetails() {
        val workout = Workout("w", "2026-10-28", "p1", "strength")
        assertEquals("Add duration · Add note", formatSessionDetailsLine(workout))
        assertTrue(formatSessionDetailsLine(workout.copy(durationSec = 2700, note = "Legs")).endsWith("· Legs"))
    }

    @Test
    fun restTimerSettingLabel() {
        assertEquals("Off", formatRestTimerSetting(0))
        assertEquals("90 s", formatRestTimerSetting(90))
    }
}
