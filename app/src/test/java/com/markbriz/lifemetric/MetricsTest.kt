package com.markbriz.lifemetric

import com.markbriz.lifemetric.domain.REMOVED_ACTIVITY_NAME
import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.plansWithLoggedActivities
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.bmiCategory
import com.markbriz.lifemetric.domain.BmiCategory
import com.markbriz.lifemetric.domain.emptyWorkoutEntry
import com.markbriz.lifemetric.domain.findPreviousLoad
import com.markbriz.lifemetric.domain.formatPreviousLoadSuggestion
import com.markbriz.lifemetric.domain.formatWorkoutEntrySummary
import com.markbriz.lifemetric.domain.lastUsedMachineId
import com.markbriz.lifemetric.domain.lastUsedPlanId
import com.markbriz.lifemetric.domain.PreviousLoad
import com.markbriz.lifemetric.domain.parseDurationInput
import com.markbriz.lifemetric.domain.summarizeWorkoutEntries
import com.markbriz.lifemetric.domain.trailingMovingAverage
import com.markbriz.lifemetric.domain.validateProfile
import com.markbriz.lifemetric.domain.weightGoalProgress
import com.markbriz.lifemetric.domain.withParsedMetrics
import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.EntryMetrics
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsTest {
    @Test
    fun bloodPressureCategoriesMatchGuidelines() {
        assertEquals(BpStatus.Normal, bloodPressureStatus(119, 79))
        assertEquals(BpStatus.Elevated, bloodPressureStatus(120, 79))
        assertEquals(BpStatus.Elevated, bloodPressureStatus(129, 79))
        assertEquals(BpStatus.Stage1, bloodPressureStatus(120, 80))
        assertEquals(BpStatus.Stage1, bloodPressureStatus(130, 79))
        assertEquals(BpStatus.Stage1, bloodPressureStatus(139, 89))
        assertEquals(BpStatus.Stage2, bloodPressureStatus(140, 79))
        assertEquals(BpStatus.Stage2, bloodPressureStatus(139, 90))
        assertEquals(BpStatus.Stage2, bloodPressureStatus(180, 120))
        assertEquals(BpStatus.Severe, bloodPressureStatus(181, 100))
        assertEquals(BpStatus.Severe, bloodPressureStatus(140, 121))
        assertEquals(
            listOf(
                "Normal",
                "Elevated",
                "Stage 1 Hypertension",
                "Stage 2 Hypertension",
                "Severe Hypertension",
            ),
            BpStatus.explained.map { it.label },
        )
        assertEquals("Less than 120 and less than 80", BpStatus.Normal.range)
        assertEquals("120–129 and less than 80", BpStatus.Elevated.range)
        assertEquals("130–139 or 80–89", BpStatus.Stage1.range)
        assertEquals("140 or higher or 90 or higher", BpStatus.Stage2.range)
        assertEquals("Higher than 180 and/or higher than 120", BpStatus.Severe.range)
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
    fun editedEntryKeepsRecordedSchemaWhenPlanTypeChanges() {
        val logged = WorkoutEntry("e", "p", "m", schema = "load", weight = 135.0, repetitions = 8)
        assertEquals("load", ActivityCatalog.entrySchema(logged, "running"))
        assertEquals("distance_time", ActivityCatalog.entrySchema(null, "running"))
        assertEquals("distance_time", ActivityCatalog.entrySchema(logged.copy(schema = "not-a-schema"), "running"))
    }

    @Test
    fun loadEntrySummary() {
        val summary = formatWorkoutEntrySummary(
            WorkoutEntry("e", "p", "m", schema = "load", weight = 135.0, repetitions = 10),
        )
        assertEquals("135 lb · 10 reps", summary)
    }

    @Test
    fun cyclingSummaryShowsOnlyMilesAndTime() {
        val entry = WorkoutEntry(
            "e",
            "p",
            "m",
            schema = "distance_time",
            distanceMi = 12.0,
            durationSec = 3600,
            avgSpeedMph = 16.5,
            avgRpm = 88,
            avgHeartRateBpm = 142,
            maxHeartRateBpm = 171,
        )
        assertEquals("12 mi · 1:00:00", formatWorkoutEntrySummary(entry))
        assertEquals("12 mi · 1:00:00", summarizeWorkoutEntries(listOf(entry)))
        assertTrue(!formatWorkoutEntrySummary(entry).contains("mph"))
        assertTrue(!formatWorkoutEntrySummary(entry).contains("rpm"))
        assertTrue(!formatWorkoutEntrySummary(entry).contains("bpm"))
        val withResistance = entry.copy(resistance = 8.0)
        assertEquals("Resistance 8 · 12 mi · 1:00:00", formatWorkoutEntrySummary(withResistance))
        assertEquals("Resistance 8 · 12 mi · 1:00:00", summarizeWorkoutEntries(listOf(withResistance)))
        assertEquals(
            "Resistance 6–8 · 24 mi · 2:00:00",
            summarizeWorkoutEntries(listOf(withResistance, withResistance.copy(id = "e2", resistance = 6.0))),
        )
    }

    @Test
    fun blankOptionalMetricsDefaultToZero() {
        val entry = WorkoutEntry("e", "p", "m", schema = "distance_time", distanceMi = 5.0, durationSec = 1200)
        val (parsed, errors) = entry.withParsedMetrics(
            ActivityCatalog.entryMetrics("cycling"),
            mapOf(
                EntryMetrics.RESISTANCE to "",
                EntryMetrics.AVG_SPEED to "",
                EntryMetrics.AVG_RPM to "",
                EntryMetrics.AVG_HEART_RATE to "",
                EntryMetrics.MAX_HEART_RATE to "",
            ),
        )
        assertTrue(errors.isEmpty())
        assertEquals(0.0, parsed.resistance, 0.0)
        assertEquals(0.0, parsed.avgSpeedMph, 0.0)
        assertEquals(0, parsed.avgRpm)
        assertEquals(0, parsed.avgHeartRateBpm)
        assertEquals(0, parsed.maxHeartRateBpm)
    }

    @Test
    fun lastUsedPlanPrefersMostRecentWorkout() {
        val upper = FitnessPlan("plan-upper", "Upper", activityTypeId = "strength_training", category = "strength")
        val lower = FitnessPlan("plan-lower", "Lower", activityTypeId = "strength_training", category = "strength")
        val workouts = listOf(
            Workout("w1", "2026-10-01", "plan-upper", "strength_training"),
            Workout("w2", "2026-10-10", "plan-lower", "strength_training"),
        )
        assertEquals("plan-lower", lastUsedPlanId(workouts, listOf(upper, lower)))
        assertEquals("plan-upper", lastUsedPlanId(workouts, listOf(upper)))
        assertNull(lastUsedPlanId(workouts, emptyList()))
        assertEquals("plan-upper", lastUsedPlanId(emptyList(), listOf(upper, lower)))
    }

    @Test
    fun lastUsedActivityPrefersLatestSetOnThatPlan() {
        val machines = listOf(Machine("bench", "Bench"), Machine("row", "Row"))
        val older = Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(load("e1", "plan-upper", "bench", 135.0, 10)))
        val newer = Workout("w2", "2026-10-10", "plan-upper", "strength_training", listOf(load("e2", "plan-upper", "row", 90.0, 8)))
        assertEquals("row", lastUsedMachineId(listOf(older, newer), "plan-upper", machines))
        assertEquals("bench", lastUsedMachineId(emptyList(), "plan-upper", machines))
        assertNull(lastUsedMachineId(listOf(newer), "plan-upper", emptyList()))
    }

    @Test
    fun previousLoadSuggestionIsVisibleCopy() {
        assertEquals("Last time: 50 lb × 12.", formatPreviousLoadSuggestion(PreviousLoad(50.0, 12)))
    }

    @Test
    fun emptyTabataBlockUsesDefaults() {
        val plan = FitnessPlan(
            id = "plan-t",
            name = "Tabata",
            activityTypeId = "tabata",
            category = "conditioning",
            machines = listOf(Machine("block", "Main")),
        )
        val entry = emptyWorkoutEntry(plan)
        assertEquals("interval", entry.schema)
        assertEquals(8, entry.rounds)
        assertEquals(20, entry.workSec)
        assertEquals(10, entry.restSec)
    }

    @Test
    fun emptyCyclingSegmentStartsAtZero() {
        val plan = FitnessPlan(
            id = "plan-c",
            name = "Ride",
            activityTypeId = "cycling",
            category = "cardio",
            machines = listOf(Machine("machine-c", "Indoor")),
        )
        val entry = emptyWorkoutEntry(plan)
        assertEquals("distance_time", entry.schema)
        assertEquals(0.0, entry.distanceMi!!, 0.0)
        assertEquals(0, entry.durationSec)
        assertEquals(0.0, entry.avgSpeedMph, 0.0)
        assertEquals(0, entry.avgRpm)
        assertEquals(0.0, entry.resistance, 0.0)
    }

    @Test
    fun cyclingIsTheOnlyActivityWithOptionalMetricsForNow() {
        assertEquals(5, ActivityCatalog.entryMetrics("cycling").size)
        assertTrue(ActivityCatalog.entryMetrics("running").isEmpty())
        assertTrue(ActivityCatalog.entryMetrics("strength_training").isEmpty())
    }

    @Test
    fun profileRejectsOutOfRangeHeight() {
        val result = validateProfile("2", "0", "", 10)
        assertEquals("Height must be between 3'0\" and 8'0\".", result.heightFeetError)
    }

    @Test
    fun previousLoadUsesMostRecentSetWithTheSameActivityName() {
        val upper = FitnessPlan(
            id = "plan-upper",
            name = "Upper",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench-a", "Chest Press"), Machine("row", "Seated Row")),
        )
        val lower = FitnessPlan(
            id = "plan-lower",
            name = "Lower",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench-b", "chest press")),
        )
        val older = Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(
            load("e1", "plan-upper", "bench-a", 135.0, 10),
            load("e3", "plan-upper", "row", 90.0, 12),
        ))
        val newer = Workout("w2", "2026-10-10", "plan-upper", "strength_training", listOf(
            load("e2", "plan-upper", "bench-a", 145.0, 8),
        ))
        val otherPlan = Workout("w3", "2026-10-12", "plan-lower", "strength_training", listOf(
            load("e4", "plan-lower", "bench-b", 155.0, 6),
        ))

        val previous = findPreviousLoad(
            workouts = listOf(older, newer, otherPlan),
            plans = listOf(upper, lower),
            activityId = "bench-a",
            currentWorkoutDate = "2026-10-19",
            currentEntries = emptyList(),
        )
        assertEquals(155.0, previous!!.weight, 0.0)
        assertEquals(6, previous.repetitions)
    }

    @Test
    fun previousLoadPrefersLaterSetOnSameDayAndIncludesCurrentWorkout() {
        val plan = FitnessPlan(
            id = "plan-upper",
            name = "Upper",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench", "Bench")),
        )
        val first = load("e1", "plan-upper", "bench", 135.0, 10)
        val second = load("e2", "plan-upper", "bench", 140.0, 8)
        val previous = findPreviousLoad(
            workouts = emptyList(),
            plans = listOf(plan),
            activityId = "bench",
            currentWorkoutDate = "2026-10-19",
            currentEntries = listOf(first, second),
        )
        assertEquals(140.0, previous!!.weight, 0.0)
        assertEquals(8, previous.repetitions)
    }

    @Test
    fun previousLoadIgnoresTheEntryBeingEdited() {
        val plan = FitnessPlan(
            id = "plan-upper",
            name = "Upper",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("bench", "Bench")),
        )
        val saved = load("e1", "plan-upper", "bench", 155.0, 6)
        val editing = load("e2", "plan-upper", "bench", 40.0, 20)
        val previous = findPreviousLoad(
            workouts = listOf(Workout("w1", "2026-10-01", "plan-upper", "strength_training", listOf(saved))),
            plans = listOf(plan),
            activityId = "bench",
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
    fun catalogUsesVocabularyLabelsAndEverySchema() {
        assertEquals(setOf("load", "distance_time", "duration", "interval", "sport", "note"), ActivityCatalog.types.map { it.schema }.toSet())
        assertEquals(6, ActivityCatalog.families.size)
        assertEquals("Add set", ActivityCatalog.addEntryLabel("load"))
        assertEquals("Add segment", ActivityCatalog.addEntryLabel("distance_time"))
        assertEquals("Add block", ActivityCatalog.addEntryLabel("interval"))
        assertEquals("Add note", ActivityCatalog.addEntryLabel("note"))
        assertEquals("Add block", ActivityCatalog.addEntryLabel("duration"))
        assertEquals("sets", ActivityCatalog.entryNoun("load", 2))
        assertEquals("segment", ActivityCatalog.entryNoun("distance_time", 1))
        val plans = ActivityCatalog.starterPlans()
        assertEquals(listOf("Upper Body Strength", "Lower Body Focus"), plans.map { it.name })
        assertEquals(9, plans[0].machines.size)
        assertEquals(3, plans[1].machines.size)
        assertTrue(plans.all { it.activityTypeId == "strength_training" })
    }

    @Test
    fun emptyWorkoutSummaryAvoidsEntryWording() {
        assertEquals("Nothing logged", summarizeWorkoutEntries(emptyList()))
        assertEquals("2 sets", summarizeWorkoutEntries(listOf(
            load("e1", "p", "m", 100.0, 8),
            load("e2", "p", "m", 110.0, 6),
        )))
    }

    @Test
    fun exportRepairKeepsOnePlaceholderForARemovedActivity() {
        val workout = Workout(
            "w1",
            "2026-10-01",
            "plan-1",
            "strength_training",
            listOf(
                load("e1", "plan-1", "machine-1", 100.0, 8),
                load("e2", "plan-1", "machine-1", 110.0, 6),
            ),
        )
        val plan = FitnessPlan(
            id = "plan-1",
            name = "Upper",
            activityTypeId = "strength_training",
            category = "strength",
            machines = listOf(Machine("machine-2", "Row")),
        )
        val repaired = plansWithLoggedActivities(listOf(plan), listOf(workout))
        assertEquals(listOf("machine-2", "machine-1"), repaired[0].machines.map { it.id })
        assertEquals(REMOVED_ACTIVITY_NAME, repaired[0].machines[1].name)
        val complete = plan.copy(machines = plan.machines + Machine("machine-1", "Bench"))
        assertEquals(complete, plansWithLoggedActivities(listOf(complete), listOf(workout))[0])
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
