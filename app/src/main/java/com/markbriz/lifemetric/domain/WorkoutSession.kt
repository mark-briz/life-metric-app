package com.markbriz.lifemetric.domain

import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutEntry

/** One activity card on the session screen. */
data class SessionActivity(
    val machineId: String,
    val name: String,
    val entries: List<WorkoutEntry>,
    /** False when the entries point at an activity that is no longer on the plan. */
    val onPlan: Boolean,
)

/**
 * Activities logged in this session come first, in the order they were first logged.
 * The rest of the plan's activities follow in plan order so each one has its own Add button.
 */
fun sessionActivities(plan: FitnessPlan?, entries: List<WorkoutEntry>): List<SessionActivity> {
    val machines = plan?.machines.orEmpty()
    val byMachine = entries.groupBy { it.machineId }
    val logged = byMachine.map { (machineId, sets) ->
        val machine = machines.find { it.id == machineId }
        SessionActivity(machineId, machine?.name ?: "Unknown activity", sets, onPlan = machine != null)
    }
    val unlogged = machines
        .filter { it.id !in byMachine }
        .map { SessionActivity(it.id, it.name, emptyList(), onPlan = true) }
    return logged + unlogged
}

/** The set a new entry for [machineId] should copy: the latest real set already logged this session. */
fun sessionPrefillEntry(entries: List<WorkoutEntry>, machineId: String, schema: String): WorkoutEntry? =
    entries.lastOrNull { it.machineId == machineId && it.schema == schema && !isHollowWorkoutEntry(it) }

/** True when an unsaved draft holds typed details that Back or Finish would throw away. */
fun draftHasDetails(workout: Workout): Boolean =
    workout.entries.isEmpty() && (workout.note.isNotBlank() || (workout.durationSec ?: 0) > 0)

/** Most recent session date for a plan, or null if it was never logged. */
fun planLastDone(workouts: List<Workout>, planId: String): String? =
    workouts.filter { it.planId == planId && it.entries.isNotEmpty() }.maxOfOrNull { it.date }

fun formatPlanLastDone(workouts: List<Workout>, planId: String): String =
    planLastDone(workouts, planId)?.let { "Last done ${formatRelativeDays(it).lowercase()}" } ?: "Not logged yet"

/** Summary line on the session screen: date, then duration and note or prompts to add them. */
fun formatSessionDetailsLine(workout: Workout): String {
    val duration = sessionDurationLabel(workout.durationSec) ?: "Add duration"
    val note = workout.note.trim().ifEmpty { "Add note" }
    return "$duration · $note"
}

fun formatRestTimerSetting(sec: Int): String = if (sec <= 0) "Off" else "$sec s"

data class SessionDetailsResult(
    val durationSec: Int?,
    val note: String,
    val error: String? = null,
)

fun validateSessionDetails(durationText: String, noteText: String): SessionDetailsResult {
    val note = noteText.trim()
    if (durationText.isBlank()) return SessionDetailsResult(null, note)
    val sec = parseDurationInput(durationText)
        ?: return SessionDetailsResult(null, note, "Enter minutes, mm:ss, or h:mm:ss.")
    return SessionDetailsResult(sec, note)
}
