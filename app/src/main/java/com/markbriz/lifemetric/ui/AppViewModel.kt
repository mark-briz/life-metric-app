package com.markbriz.lifemetric.ui

import android.app.Application
import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.LocalFile
import com.markbriz.lifemetric.data.LocalRepository
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutDraft
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.data.newId
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.markbriz.lifemetric.domain.clampBpWindow
import com.markbriz.lifemetric.domain.nowTime
import com.markbriz.lifemetric.domain.roundToTenth
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.domain.validateProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AppUiState(
    val booting: Boolean = true,
    /** False until the one-time health disclaimer has been accepted on this device. */
    val disclaimerAccepted: Boolean = false,
    val store: FitnessStore = FitnessStore(),
    val themeId: String = "mint",
    val savedThemeId: String = "mint",
    val workoutDraft: WorkoutDraft? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LocalRepository(app)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            val file = withContext(Dispatchers.IO) { repo.load() }
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < 900) delay(900 - elapsed)
            _state.value = stateFrom(file, booting = false)
        }
    }

    fun acceptDisclaimer() {
        viewModelScope.launch {
            mutex.withLock {
                val file = withContext(Dispatchers.IO) { repo.acceptDisclaimer() }
                _state.value = stateFrom(file, booting = false)
            }
        }
    }

    /** Deletes every entry on this device and resets to the starter plans. */
    fun eraseAllData() {
        viewModelScope.launch {
            mutex.withLock {
                val file = withContext(Dispatchers.IO) { repo.eraseAll() }
                _state.value = stateFrom(file, booting = false)
            }
        }
    }

    fun previewTheme(themeId: String) {
        _state.update { it.copy(themeId = themeId) }
    }

    fun revertThemePreview() {
        _state.update { it.copy(themeId = it.savedThemeId) }
    }

    fun saveProfile(
        heightFeet: String,
        heightInches: String,
        weightGoalText: String,
        bpWindow: Int,
        themeId: String,
        weightGoalStartId: String?,
    ): String? {
        val result = validateProfile(heightFeet, heightInches, weightGoalText, bpWindow)
        if (!result.ok) return result.heightFeetError ?: result.heightInchesError ?: result.weightGoalError ?: result.bpError
        val startId = weightGoalStartId?.takeIf { id -> _state.value.store.weightRecords.any { it.id == id } }
        _state.update {
            it.copy(
                themeId = themeId,
                savedThemeId = themeId,
                store = it.store.copy(
                    themeId = themeId,
                    profile = it.store.profile.copy(
                        heightInches = result.heightInches,
                        weightGoalLb = result.weightGoalLb,
                        bpMovingAverageReadings = clampBpWindow(result.bpMovingAverageReadings),
                        weightGoalStartId = startId,
                    ),
                ),
            )
        }
        mutate { store ->
            store.copy(
                profile = store.profile.copy(
                    heightInches = result.heightInches,
                    weightGoalLb = result.weightGoalLb,
                    bpMovingAverageReadings = clampBpWindow(result.bpMovingAverageReadings),
                    weightGoalStartId = startId?.takeIf { id -> store.weightRecords.any { it.id == id } },
                ),
                themeId = themeId,
            )
        }
        return null
    }

    fun savePlan(planId: String?, name: String, description: String, activityTypeId: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Name is required."
        val type = ActivityCatalog.get(activityTypeId)
        mutate { store ->
            val plans = store.plans.toMutableList()
            if (planId == null) {
                plans += FitnessPlan(
                    id = newId("plan"),
                    name = trimmed,
                    description = description.trim(),
                    activityTypeId = type.id,
                    category = ActivityCatalog.category(type.id),
                )
            } else {
                val index = plans.indexOfFirst { it.id == planId }
                if (index < 0) return@mutate store
                val existing = plans[index]
                plans[index] = existing.copy(
                    name = trimmed,
                    description = description.trim(),
                    activityTypeId = type.id,
                    category = ActivityCatalog.category(type.id),
                )
            }
            store.copy(plans = plans)
        }
        return null
    }

    fun saveActivity(planId: String, machineId: String?, name: String, description: String): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "Name is required."
        mutate { store ->
            val plans = store.plans.map { plan ->
                if (plan.id != planId) return@map plan
                val machines = plan.machines.toMutableList()
                if (machineId == null) {
                    machines += Machine(newId("machine"), trimmed, description.trim())
                } else {
                    val index = machines.indexOfFirst { it.id == machineId }
                    if (index >= 0) {
                        machines[index] = machines[index].copy(name = trimmed, description = description.trim())
                    }
                }
                plan.copy(machines = machines)
            }
            store.copy(plans = plans)
        }
        return null
    }

    fun deleteActivity(planId: String, machineId: String) {
        mutate { store ->
            store.copy(
                plans = store.plans.map { plan ->
                    if (plan.id != planId) plan else plan.copy(machines = plan.machines.filterNot { it.id == machineId })
                },
            )
        }
    }

    fun beginNewWorkout() {
        val plans = _state.value.store.plans
        _state.update {
            it.copy(
                workoutDraft = WorkoutDraft(
                    workoutId = null,
                    date = todayIso(),
                    planId = plans.firstOrNull()?.id.orEmpty(),
                    entries = emptyList(),
                    backToDetail = false,
                ),
            )
        }
    }

    fun beginEditWorkout(workoutId: String, backToDetail: Boolean) {
        val workout = _state.value.store.workouts.find { it.id == workoutId } ?: return
        _state.update {
            it.copy(
                workoutDraft = WorkoutDraft(
                    workoutId = workout.id,
                    date = workout.date,
                    planId = workout.planId,
                    entries = workout.entries,
                    backToDetail = backToDetail,
                ),
            )
        }
    }

    fun updateDraftDate(date: String) {
        _state.update { state ->
            val draft = state.workoutDraft ?: return@update state
            state.copy(workoutDraft = draft.copy(date = date))
        }
    }

    fun updateDraftPlan(planId: String) {
        _state.update { state ->
            val draft = state.workoutDraft ?: return@update state
            val entries = if (planId == draft.planId) draft.entries else emptyList()
            state.copy(workoutDraft = draft.copy(planId = planId, entries = entries))
        }
    }

    fun upsertDraftEntry(entry: WorkoutEntry) {
        _state.update { state ->
            val draft = state.workoutDraft ?: return@update state
            val entries = draft.entries.toMutableList()
            val index = entries.indexOfFirst { it.id == entry.id }
            if (index >= 0) entries[index] = entry else entries += entry
            state.copy(workoutDraft = draft.copy(entries = entries))
        }
    }

    fun removeDraftEntry(entryId: String) {
        _state.update { state ->
            val draft = state.workoutDraft ?: return@update state
            state.copy(workoutDraft = draft.copy(entries = draft.entries.filterNot { it.id == entryId }))
        }
    }

    fun saveDraftWorkout(): String? {
        val draft = _state.value.workoutDraft ?: return "Workout not found."
        if (draft.date.isBlank()) return "Date is required."
        val plan = _state.value.store.plans.find { it.id == draft.planId } ?: return "Select a workout plan."
        if (draft.entries.isEmpty()) return "Add at least one workout entry before saving."
        mutate { store ->
            val workouts = store.workouts.toMutableList()
            val workout = Workout(
                id = draft.workoutId ?: newId("workout"),
                date = draft.date,
                planId = plan.id,
                activityTypeId = plan.activityTypeId,
                entries = draft.entries,
            )
            val index = workouts.indexOfFirst { it.id == workout.id }
            if (index >= 0) workouts[index] = workout else workouts += workout
            store.copy(workouts = workouts)
        }
        _state.update { it.copy(workoutDraft = null) }
        return null
    }

    fun deleteWorkout(workoutId: String) {
        mutate { store ->
            store.copy(workouts = store.workouts.filterNot { it.id == workoutId })
        }
    }

    fun upsertSavedEntry(workoutId: String, entry: WorkoutEntry) {
        mutate { store ->
            store.copy(
                workouts = store.workouts.map { workout ->
                    if (workout.id != workoutId) workout
                    else {
                        val entries = workout.entries.toMutableList()
                        val index = entries.indexOfFirst { it.id == entry.id }
                        if (index >= 0) entries[index] = entry else entries += entry
                        workout.copy(entries = entries)
                    }
                },
            )
        }
    }

    fun deleteWorkoutEntry(workoutId: String, entryId: String) {
        mutate { store ->
            store.copy(
                workouts = store.workouts.map { workout ->
                    if (workout.id != workoutId) workout
                    else workout.copy(entries = workout.entries.filterNot { it.id == entryId })
                },
            )
        }
    }

    fun saveBloodPressure(
        readingId: String?,
        date: String,
        time: String,
        systolic: Int,
        diastolic: Int,
        heartRate: Int,
    ) {
        mutate { store ->
            val readings = store.bloodPressureReadings.toMutableList()
            val reading = BloodPressureReading(
                id = readingId ?: newId("bp"),
                date = date,
                time = time.ifBlank { nowTime() },
                systolic = systolic,
                diastolic = diastolic,
                heartRate = heartRate,
            )
            val index = readings.indexOfFirst { it.id == reading.id }
            if (index >= 0) readings[index] = reading else readings += reading
            store.copy(bloodPressureReadings = readings)
        }
    }

    fun deleteBloodPressure(id: String) {
        mutate { store ->
            store.copy(bloodPressureReadings = store.bloodPressureReadings.filterNot { it.id == id })
        }
    }

    fun saveWeight(recordId: String?, date: String, time: String, weight: Double) {
        mutate { store ->
            val records = store.weightRecords.toMutableList()
            val record = WeightRecord(
                id = recordId ?: newId("bw"),
                date = date,
                time = time.ifBlank { nowTime() },
                weight = roundToTenth(weight),
            )
            val index = records.indexOfFirst { it.id == record.id }
            if (index >= 0) records[index] = record else records += record
            store.copy(weightRecords = records)
        }
    }

    fun deleteWeight(id: String) {
        mutate { store ->
            val profile = if (store.profile.weightGoalStartId == id) {
                store.profile.copy(weightGoalStartId = null)
            } else {
                store.profile
            }
            store.copy(
                weightRecords = store.weightRecords.filterNot { it.id == id },
                profile = profile,
            )
        }
    }

    /** Snapshot of the tracking data plus saved theme, for export. */
    fun exportSnapshot(): Pair<FitnessStore, String> = _state.value.store to _state.value.savedThemeId

    /**
     * Replaces the profile, theme, plans, workouts, weight, and BP on this device with an
     * imported file. Mirrors `replaceAllData` + `commitThemeId` in the Expo app.
     */
    fun importData(data: FitnessStore, themeId: String, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            mutex.withLock {
                val result = runCatching {
                    withContext(Dispatchers.IO) { repo.mutate { data.copy(themeId = themeId) } }
                }
                result.onSuccess { updated ->
                    _state.update {
                        it.copy(
                            store = updated.store,
                            themeId = updated.store.themeId,
                            savedThemeId = updated.store.themeId,
                            workoutDraft = null,
                        )
                    }
                    onDone(null)
                }.onFailure { onDone(it.message ?: "Unable to load this file.") }
            }
        }
    }

    private fun mutate(transform: (FitnessStore) -> FitnessStore) {
        viewModelScope.launch {
            mutex.withLock {
                val updated = withContext(Dispatchers.IO) { repo.mutate(transform) }
                _state.update {
                    it.copy(
                        store = updated.store,
                        themeId = if (it.themeId == it.savedThemeId) updated.store.themeId else it.themeId,
                        savedThemeId = updated.store.themeId,
                    )
                }
            }
        }
    }

    private fun stateFrom(file: LocalFile, booting: Boolean): AppUiState = AppUiState(
        booting = booting,
        disclaimerAccepted = file.disclaimerAccepted,
        store = file.store,
        themeId = file.store.themeId,
        savedThemeId = file.store.themeId,
    )
}
