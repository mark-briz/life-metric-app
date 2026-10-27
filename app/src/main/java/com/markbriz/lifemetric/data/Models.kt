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
)

@Serializable
data class Workout(
    val id: String,
    val date: String,
    val planId: String,
    val activityTypeId: String,
    val entries: List<WorkoutEntry> = emptyList(),
)

@Serializable
data class WeightRecord(
    val id: String,
    val date: String,
    val time: String,
    val weight: Double,
)

@Serializable
data class BloodPressureReading(
    val id: String,
    val date: String,
    val time: String,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
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

