package com.markbriz.lifemetric

import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.bmiCategory
import com.markbriz.lifemetric.domain.BmiCategory
import com.markbriz.lifemetric.domain.findPreviousLoad
import com.markbriz.lifemetric.domain.formatWorkoutEntrySummary
import com.markbriz.lifemetric.domain.parseDurationInput
import com.markbriz.lifemetric.domain.trailingMovingAverage
import com.markbriz.lifemetric.domain.validateProfile
import com.markbriz.lifemetric.domain.weightGoalProgress
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MetricsTest {
    @Test
    fun bloodPressureCategoriesMatchGuidelines() {
        assertEquals(BpStatus.Normal, bloodPressureStatus(118, 76))
        assertEquals(BpStatus.Elevated, bloodPressureStatus(125, 78))
        assertEquals(BpStatus.Stage1, bloodPressureStatus(132, 82))
        assertEquals(BpStatus.Stage2, bloodPressureStatus(145, 92))
        assertEquals(BpStatus.Crisis, bloodPressureStatus(181, 100))
    }

    @Test
    fun bmiUsesStandardFormula() {
        val bmi = calculateBmi(182.3, 70)
        assertEquals(BmiCategory.Over, bmiCategory(bmi))
    }

    @Test
    fun movingAverageUsesTrailingWindow() {
        val averages = trailingMovingAverage(listOf(2.0, 4.0, 6.0, 8.0), 2)
        assertEquals(listOf(2.0, 3.0, 5.0, 7.0), averages)
    }

    @Test
    fun durationAcceptsClockAndMinutes() {
        assertEquals(90, parseDurationInput("1:30"))
        assertEquals(120, parseDurationInput("2"))
        assertNull(parseDurationInput("1:70"))
    }

    @Test
    fun loadEntrySummary() {
        val summary = formatWorkoutEntrySummary(
            WorkoutEntry("e", "p", "m", schema = "load", weight = 135.0, repetitions = 10),
        )
        assertEquals("135 lb · 10 reps", summary)
    }

    @Test
    fun profileRejectsOutOfRangeHeight() {
        val result = validateProfile("2", "0", "", 10)
        assertEquals("Height must be between 3'0\" and 8'0\".", result.heightFeetError)
    }

    @Test
    fun previousLoadUsesMostRecentSetForSamePlanAndActivity() {
        val benchA = load("e1", "plan-upper", "bench", 135.0, 10)
        val benchB = load("e2", "plan-upper", "bench", 145.0, 8)
        val row = load("e3", "plan-upper", "row", 90.0, 12)
        val lower = load("e4", "plan-lower", "bench", 200.0, 5)
        val older = Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(benchA, row))
        val newer = Workout("w2", "2026-10-10", "plan-upper", "strength_training", listOf(benchB))
        val otherPlan = Workout("w3", "2026-10-12", "plan-lower", "strength_training", listOf(lower))

        val previous = findPreviousLoad(
            workouts = listOf(older, newer, otherPlan),
            planId = "plan-upper",
            machineId = "bench",
            currentWorkoutDate = "2026-10-19",
            currentEntries = emptyList(),
        )
        assertEquals(145.0, previous!!.weight, 0.0)
        assertEquals(8, previous.repetitions)
    }

    @Test
    fun previousLoadPrefersLaterSetOnSameDayAndIncludesCurrentWorkout() {
        val first = load("e1", "plan-upper", "bench", 135.0, 10)
        val second = load("e2", "plan-upper", "bench", 140.0, 8)
        val previous = findPreviousLoad(
            workouts = emptyList(),
            planId = "plan-upper",
            machineId = "bench",
            currentWorkoutDate = "2026-10-19",
            currentEntries = listOf(first, second),
        )
        assertEquals(140.0, previous!!.weight, 0.0)
        assertEquals(8, previous.repetitions)
    }

    @Test
    fun previousLoadIgnoresTheEntryBeingEdited() {
        val saved = load("e1", "plan-upper", "bench", 155.0, 6)
        val editing = load("e2", "plan-upper", "bench", 40.0, 20)
        val previous = findPreviousLoad(
            workouts = listOf(Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(saved))),
            planId = "plan-upper",
            machineId = "bench",
            currentWorkoutDate = "2026-10-19",
            currentEntries = listOf(editing),
            currentWorkoutId = "w-now",
            excludeEntryId = "e2",
        )
        assertEquals(155.0, previous!!.weight, 0.0)
        assertEquals(6, previous.repetitions)
    }

    @Test
    fun weightGoalProgressUsesStartAndLatestTowardTarget() {
        val start = WeightRecord("start", "2026-09-01", "08:00", 180.0)
        val later = WeightRecord("now", "2026-10-19", "08:00", 176.0)
        val progress = weightGoalProgress(listOf(start, later), "start", 170.0)!!
        assertEquals(10.0, progress.targetToLoseLb, 0.0)
        assertEquals(4.0, progress.weightLostLb, 0.0)
        assertEquals(40.0, progress.percentComplete, 0.01)
    }

    @Test
    fun weightGoalProgressNeedsAStartingReading() {
        val record = WeightRecord("w1", "2026-10-01", "08:00", 180.0)
        assertNull(weightGoalProgress(listOf(record), null, 170.0))
        assertNull(weightGoalProgress(listOf(record), "missing", 170.0))
        assertNull(weightGoalProgress(listOf(record), "w1", null))
    }

    private fun load(id: String, planId: String, machineId: String, weight: Double, reps: Int) =
        WorkoutEntry(id, planId, machineId, schema = "load", weight = weight, repetitions = reps)
}
