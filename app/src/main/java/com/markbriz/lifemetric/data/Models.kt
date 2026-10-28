package com.markbriz.lifemetric.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(prefix: String): String = "$prefix-${UUID.randomUUID()}"

@Serializable
data class Machine(
    val id: String,
    val name: String,
    val description: String = "",
)

@Serializable
data class FitnessPlan(
    val id: String,
    val name: String,
    val description: String = "",
    val activityTypeId: String,
    val category: String,
    val machines: List<Machine> = emptyList(),
)

@Serializable
data class WorkoutEntry(
    val id: String,
    val planId: String,
    val machineId: String,
    val schema: String = "load",
    val weight: Double? = null,
    val repetitions: Int? = null,
    val distanceMi: Double? = null,
    val durationSec: Int? = null,
    val rounds: Int? = null,
    val workSec: Int? = null,
    val restSec: Int? = null,
    val intensity: String? = null,
    val note: String? = null,
    /** Optional telemetry any activity type can record. Missing values load as 0. */
    val resistance: Double = 0.0,
    val avgSpeedMph: Double = 0.0,
    val avgRpm: Int = 0,
    val avgHeartRateBpm: Int = 0,
    val maxHeartRateBpm: Int = 0,
    /** warmup | working | drop | failure. Missing values load as working. */
    val setKind: String = "working",
    /** Optional Borg-style 1–10. */
    val rpe: Int? = null,
    /** Actual rest after this set, not the interval prescription. */
    val restSecTaken: Int? = null,
)

@Serializable
data class Workout(
    val id: String,
    val date: String,
    val planId: String,
    val activityTypeId: String,
    val entries: List<WorkoutEntry> = emptyList(),
    /** Optional elapsed session time. Date-only rows stay valid without it. */
    val durationSec: Int? = null,
    /** Optional one-line session context. */
    val note: String = "",
)

@Serializable
data class WeightRecord(
    val id: String,
    val date: String,
    val time: String,
    val weight: Double,
    val tags: List<String> = emptyList(),
    val note: String = "",
)

@Serializable
data class BloodPressureSample(
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int? = null,
)

@Serializable
data class BloodPressureReading(
    val id: String,
    val date: String,
    val time: String,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int? = null,
    /** Stored morning / evening / other override. Null means infer from time. */
    val period: String? = null,
    val pairId: String? = null,
    /** Raw cuff inflations when systolic/diastolic are the pair average. */
    val averagedFrom: List<BloodPressureSample> = emptyList(),
    val tags: List<String> = emptyList(),
    val note: String = "",
)

@Serializable
data class UserProfile(
    val firstName: String = "",
    val lastName: String = "",
    val heightInches: Int? = null,
    val weightGoalLb: Double? = null,
    val bpMovingAverageReadings: Int = 10,
    /** Id of the weight reading that starts the current goal, if any. */
    val weightGoalStartId: String? = null,
    val bpTargetSystolic: Int? = null,
    val bpTargetDiastolic: Int? = null,
    val bpMorningReminder: Boolean = false,
    val bpEveningReminder: Boolean = false,
    val bpMorningReminderTime: String = "07:00",
    val bpEveningReminderTime: String = "19:00",
    val weightMorningReminder: Boolean = false,
    val weightMorningReminderTime: String = "07:00",
    val weightDisplayKg: Boolean = false,
    val waistInches: Double? = null,
    val workoutMorningReminder: Boolean = false,
    val workoutMorningReminderTime: String = "07:00",
    /** 0 means the rest timer is off. */
    val workoutRestTimerSec: Int = 0,
)

@Serializable
data class FitnessStore(
    val plans: List<FitnessPlan> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val weightRecords: List<WeightRecord> = emptyList(),
    val bloodPressureReadings: List<BloodPressureReading> = emptyList(),
    val profile: UserProfile = UserProfile(),
    val themeId: String = "mint",
)

/** Everything the app persists. There is no account; the phone's own lock screen protects access. */
@Serializable
data class LocalFile(
    val store: FitnessStore = FitnessStore(),
    val disclaimerAccepted: Boolean = false,
)

