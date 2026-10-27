package com.markbriz.lifemetric

import com.markbriz.lifemetric.data.ACCOUNT_EXPORT_FORMAT
import com.markbriz.lifemetric.data.AccountExportException
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.UserProfile
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.data.accountExportFilename
import com.markbriz.lifemetric.data.buildAccountExport
import com.markbriz.lifemetric.data.parseAccountExport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.LocalDateTime

class AccountExportTest {
    private val sample = FitnessStore(
        plans = listOf(
            FitnessPlan(
                id = "plan-1",
                name = "Upper",
                activityTypeId = "strength_training",
                category = "strength",
                machines = listOf(Machine("machine-1", "Bench")),
            ),
        ),
        workouts = listOf(
            Workout(
                id = "workout-1",
                date = "2026-10-01",
                planId = "plan-1",
                activityTypeId = "strength_training",
                entries = listOf(
                    WorkoutEntry(
                        id = "entry-1",
                        planId = "plan-1",
                        machineId = "machine-1",
                        schema = "load",
                        weight = 100.0,
                        repetitions = 8,
                    ),
                ),
            ),
        ),
        weightRecords = listOf(
            WeightRecord("11111111-1111-4111-8111-111111111111", "2026-10-02", "08:15", 180.2),
        ),
        bloodPressureReadings = listOf(
            BloodPressureReading("bp-1", "2026-10-02", "08:16", 120, 80, 60),
        ),
        profile = UserProfile("Ada", "Lovelace", 65, 170.0, 10),
    )

    // Same sample the Expo app's accountExport.test.ts uses, as it would be written by that app.
    private val expoFile = """
        {
          "format": "life-metric-account",
          "version": 1,
          "exportedAt": "2026-10-12T12:00:00.000Z",
          "themeId": "ocean",
          "data": {
            "plans": [{"id":"plan-1","name":"Upper","activityTypeId":"strength_training","category":"strength","machines":[{"id":"machine-1","name":"Bench"}]}],
            "workouts": [{"id":"workout-1","date":"2026-10-01","planId":"plan-1","activityTypeId":"strength_training","entries":[{"id":"entry-1","planId":"plan-1","machineId":"machine-1","schema":"load","weight":100,"repetitions":8}]}],
            "weightRecords": [{"id":"11111111-1111-4111-8111-111111111111","date":"2026-10-02","time":"08:15","weight":180.2}],
            "bloodPressureReadings": [{"id":"bp-1","date":"2026-10-02","time":"08:16","systolic":120,"diastolic":80,"heartRate":60}],
            "profile": {"firstName":"Ada","lastName":"Lovelace","heightInches":65,"weightGoalLb":170,"bpMovingAverageReadings":10}
          }
        }
    """.trimIndent()

    @Test
    fun filenameUsesLocalClock() {
        assertEquals(
            "life-metric-2026-10-12-170405.json",
            accountExportFilename(LocalDateTime.of(2026, 10, 12, 17, 4, 5)),
        )
    }

    @Test
    fun buildRoundTripsTrackingDataAndTheme() {
        val raw = buildAccountExport(sample, "ocean", "2026-10-12T12:00:00.000Z")
        val root = Json.parseToJsonElement(raw).jsonObject
        assertEquals(ACCOUNT_EXPORT_FORMAT, root["format"]!!.jsonPrimitive.content)
        assertEquals("1", root["version"]!!.jsonPrimitive.content)

        val parsed = parseAccountExport(raw)
        assertEquals("ocean", parsed.themeId)
        assertEquals(sample.profile, parsed.data.profile)
        assertEquals(sample.plans, parsed.data.plans)
        assertEquals(sample.workouts, parsed.data.workouts)
        assertEquals(sample.weightRecords, parsed.data.weightRecords)
        assertEquals(sample.bloodPressureReadings, parsed.data.bloodPressureReadings)
    }

    @Test
    fun readsFileWrittenByExpoApp() {
        val parsed = parseAccountExport(expoFile)
        assertEquals("ocean", parsed.themeId)
        assertEquals(sample.copy(themeId = "mint"), parsed.data)
    }

    @Test
    fun exportRewritesPrefixedWeightIdsToUuids() {
        val store = sample.copy(weightRecords = listOf(WeightRecord("bw-abc", "2026-10-02", "08:15", 180.2)))
        val raw = buildAccountExport(store, "mint", "2026-10-12T12:00:00.000Z")
        val id = Json.parseToJsonElement(raw).jsonObject["data"]!!.jsonObject["weightRecords"]!!
            .let { it.toString() }
        assertTrue(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").containsMatchIn(id))
    }

    @Test
    fun missingCyclingMetricsImportAsZero() {
        val raw = """
            {
              "format": "life-metric-account",
              "version": 1,
              "exportedAt": "2026-10-12T12:00:00.000Z",
              "themeId": "mint",
              "data": {
                "plans": [{"id":"plan-c","name":"Ride","activityTypeId":"cycling","category":"cardio","machines":[{"id":"machine-c","name":"Indoor"}]}],
                "workouts": [{"id":"workout-c","date":"2026-10-01","planId":"plan-c","activityTypeId":"cycling","entries":[{"id":"entry-c","planId":"plan-c","machineId":"machine-c","schema":"distance_time","distanceMi":12.4,"durationSec":2400}]}],
                "weightRecords": [],
                "bloodPressureReadings": [],
                "profile": {"firstName":"Ada","lastName":"Lovelace"}
              }
            }
        """.trimIndent()
        val entry = parseAccountExport(raw).data.workouts[0].entries[0]
        assertEquals(0.0, entry.resistance, 0.0)
        assertEquals(0.0, entry.avgSpeedMph, 0.0)
        assertEquals(0, entry.avgRpm)
        assertEquals(0, entry.avgHeartRateBpm)
        assertEquals(0, entry.maxHeartRateBpm)
    }

    @Test
    fun allLoggingSchemasAndGoalStartRoundTrip() {
        val store = FitnessStore(
            plans = listOf(
                FitnessPlan(id = "plan-load", name = "Lift", activityTypeId = "strength_training", category = "strength", machines = listOf(Machine("m-load", "Bench"))),
                FitnessPlan(id = "plan-ride", name = "Ride", activityTypeId = "cycling", category = "cardio", machines = listOf(Machine("m-ride", "Bike"))),
                FitnessPlan(id = "plan-yoga", name = "Flow", activityTypeId = "yoga", category = "mobility_flexibility", machines = listOf(Machine("m-yoga", "Mat"))),
                FitnessPlan(id = "plan-hiit", name = "HIIT", activityTypeId = "hiit", category = "conditioning", machines = listOf(Machine("m-hiit", "Main"))),
                FitnessPlan(id = "plan-ball", name = "Pickup", activityTypeId = "basketball", category = "sports", machines = listOf(Machine("m-ball", "Court"))),
                FitnessPlan(id = "plan-rest", name = "Rest", activityTypeId = "rest_day", category = "recovery", machines = listOf(Machine("m-rest", "Note"))),
            ),
            workouts = listOf(
                Workout("w-load", "2026-10-01", "plan-load", "strength_training", listOf(
                    WorkoutEntry("e-load", "plan-load", "m-load", schema = "load", weight = 135.0, repetitions = 8),
                )),
                Workout("w-ride", "2026-10-02", "plan-ride", "cycling", listOf(
                    WorkoutEntry("e-ride", "plan-ride", "m-ride", schema = "distance_time", distanceMi = 8.5, durationSec = 1800),
                )),
                Workout("w-yoga", "2026-10-03", "plan-yoga", "yoga", listOf(
                    WorkoutEntry("e-yoga", "plan-yoga", "m-yoga", schema = "duration", durationSec = 2400),
                )),
                Workout("w-hiit", "2026-10-04", "plan-hiit", "hiit", listOf(
                    WorkoutEntry("e-hiit", "plan-hiit", "m-hiit", schema = "interval", rounds = 8, workSec = 20, restSec = 10),
                )),
                Workout("w-ball", "2026-10-05", "plan-ball", "basketball", listOf(
                    WorkoutEntry("e-ball", "plan-ball", "m-ball", schema = "sport", durationSec = 3600, intensity = "moderate"),
                )),
                Workout("w-rest", "2026-10-06", "plan-rest", "rest_day", listOf(
                    WorkoutEntry("e-rest", "plan-rest", "m-rest", schema = "note", note = "Easy walk"),
                )),
            ),
            weightRecords = listOf(WeightRecord("11111111-1111-4111-8111-111111111111", "2026-10-02", "08:15", 180.2)),
            profile = UserProfile("Ada", "Lovelace", 65, 170.0, 10, "11111111-1111-4111-8111-111111111111"),
        )
        val parsed = parseAccountExport(buildAccountExport(store, "sunset", "2026-10-12T12:00:00.000Z"))
        assertEquals("sunset", parsed.themeId)
        assertEquals("11111111-1111-4111-8111-111111111111", parsed.data.profile.weightGoalStartId)
        assertEquals(6, parsed.data.plans.size)
        assertEquals(6, parsed.data.workouts.size)
        assertEquals(135.0, parsed.data.workouts.first { it.id == "w-load" }.entries[0].weight)
        assertEquals(8.5, parsed.data.workouts.first { it.id == "w-ride" }.entries[0].distanceMi)
        assertEquals(2400, parsed.data.workouts.first { it.id == "w-yoga" }.entries[0].durationSec)
        assertEquals(8, parsed.data.workouts.first { it.id == "w-hiit" }.entries[0].rounds)
        assertEquals(20, parsed.data.workouts.first { it.id == "w-hiit" }.entries[0].workSec)
        assertEquals("moderate", parsed.data.workouts.first { it.id == "w-ball" }.entries[0].intensity)
        assertEquals("Easy walk", parsed.data.workouts.first { it.id == "w-rest" }.entries[0].note)
    }

    @Test
    fun cyclingMetricsRoundTripThroughExport() {
        val store = FitnessStore(
            plans = listOf(
                FitnessPlan(
                    id = "plan-c",
                    name = "Ride",
                    activityTypeId = "cycling",
                    category = "cardio",
                    machines = listOf(Machine("machine-c", "Indoor")),
                ),
            ),
            workouts = listOf(
                Workout(
                    id = "workout-c",
                    date = "2026-10-01",
                    planId = "plan-c",
                    activityTypeId = "cycling",
                    entries = listOf(
                        WorkoutEntry(
                            id = "entry-c",
                            planId = "plan-c",
                            machineId = "machine-c",
                            schema = "distance_time",
                            distanceMi = 12.4,
                            durationSec = 2400,
                            resistance = 8.0,
                            avgSpeedMph = 16.5,
                            avgRpm = 88,
                            avgHeartRateBpm = 142,
                            maxHeartRateBpm = 171,
                        ),
                    ),
                ),
            ),
            profile = UserProfile("Ada", "Lovelace"),
        )
        val parsed = parseAccountExport(buildAccountExport(store, "mint", "2026-10-12T12:00:00.000Z"))
        val entry = parsed.data.workouts[0].entries[0]
        assertEquals(8.0, entry.resistance, 0.0)
        assertEquals(16.5, entry.avgSpeedMph, 0.0)
        assertEquals(88, entry.avgRpm)
        assertEquals(142, entry.avgHeartRateBpm)
        assertEquals(171, entry.maxHeartRateBpm)
    }

    @Test
    fun acceptsRawTrackingBlob() {
        val data = Json.parseToJsonElement(expoFile).jsonObject["data"]!!.toString()
        val parsed = parseAccountExport(data)
        assertNull(parsed.themeId)
        assertEquals("Upper", parsed.data.plans[0].name)
    }

    @Test
    fun rejectsDifferentFormatOrVersion() {
        val data = Json.parseToJsonElement(expoFile).jsonObject["data"]!!.toString()
        assertThrowsMessage("not a Life Metric data export") {
            parseAccountExport("""{"format":"other","version":1,"data":$data}""")
        }
        assertThrowsMessage("different version") {
            parseAccountExport("""{"format":"life-metric-account","version":2,"data":$data}""")
        }
        assertThrowsMessage("not valid JSON") { parseAccountExport("{nope") }
    }

    @Test
    fun rejectsUnreadablePlanAndNonUuidWeight() {
        val brokenPlan = expoFile.replace(
            """"machines":[{"id":"machine-1","name":"Bench"}]}]""",
            """"machines":[{"id":"machine-1","name":"Bench"}]},{"id":"plan-2","machines":[]}]""",
        )
        assertThrowsMessage("workout plan") { parseAccountExport(brokenPlan) }
        val brokenWeight = expoFile.replace("11111111-1111-4111-8111-111111111111", "")
        assertThrowsMessage("weight reading") { parseAccountExport(brokenWeight) }
    }

    @Test
    fun rejectsWorkoutPointingAtMissingPlan() {
        val broken = expoFile.replace("\"planId\":\"plan-1\",\"activityTypeId\"", "\"planId\":\"plan-x\",\"activityTypeId\"")
        assertThrowsMessage("plan that is not in the file") { parseAccountExport(broken) }
    }

    private fun assertThrowsMessage(fragment: String, block: () -> Unit) {
        try {
            block()
            fail("Expected failure containing '$fragment'")
        } catch (err: AccountExportException) {
            assertTrue("'${err.message}' should contain '$fragment'", err.message!!.contains(fragment))
        }
    }
}
