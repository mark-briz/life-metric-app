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
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.data.newId
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.markbriz.lifemetric.domain.BloodPressureSave
import com.markbriz.lifemetric.domain.clampBpWindow
import com.markbriz.lifemetric.domain.lastUsedPlanId
import com.markbriz.lifemetric.domain.nowTime
import com.markbriz.lifemetric.domain.roundToTenth
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.domain.validateBloodPressureTarget
import com.markbriz.lifemetric.domain.validateProfile
import com.markbriz.lifemetric.domain.validateWaistInches
import com.markbriz.lifemetric.reminders.BloodPressureReminders
import com.markbriz.lifemetric.reminders.WeightReminders
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

data class UndoToast(val message: String, val token: Long = System.nanoTime())

data class AppUiState(
    val booting: Boolean = true,
    /** False until the one-time health disclaimer has been accepted on this device. */
    val disclaimerAccepted: Boolean = false,
    val store: FitnessStore = FitnessStore(),
    val themeId: String = "mint",
    val savedThemeId: String = "mint",
    /** In-memory session started from Log workout; not written until the first saved entry. */
    val draftWorkout: Workout? = null,
    val undo: UndoToast? = null,
    val notice: UndoToast? = null,
)

private sealed class DeletedRecord {
    data class BloodPressure(val reading: BloodPressureReading) : DeletedRecord()
    data class Weight(val record: WeightRecord, val clearedStart: Boolean) : DeletedRecord()
    data class Session(val workout: Workout) : DeletedRecord()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = LocalRepository(app)
    private val mutex = Mutex()
    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()
    private var lastDeleted: DeletedRecord? = null

    init {
        viewModelScope.launch {
            val started = System.currentTimeMillis()
            val file = withContext(Dispatchers.IO) { repo.load() }
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < 900) delay(900 - elapsed)
            _state.value = stateFrom(file, booting = false)
            BloodPressureReminders.sync(getApplication(), file.store.profile)
            WeightReminders.sync(getApplication(), file.store.profile)
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
                lastDeleted = null
                val file = withContext(Dispatchers.IO) { repo.eraseAll() }
                _state.value = stateFrom(file, booting = false)
                BloodPressureReminders.sync(getApplication(), file.store.profile)
                WeightReminders.sync(getApplication(), file.store.profile)
            }
        }
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
        val heightOk = result.heightFeetError == null && result.heightInchesError == null
        val goalOk = result.weightGoalError == null
        val bpOk = result.bpError == null
        val startId = weightGoalStartId?.takeIf { id -> _state.value.store.weightRecords.any { it.id == id } }
        _state.update {
            it.copy(
                themeId = themeId,
                savedThemeId = themeId,
                store = it.store.copy(
                    themeId = themeId,
                    profile = it.store.profile.copy(
                        heightInches = if (heightOk) result.heightInches else it.store.profile.heightInches,
                        weightGoalLb = if (goalOk) result.weightGoalLb else it.store.profile.weightGoalLb,
                        bpMovingAverageReadings = if (bpOk) clampBpWindow(result.bpMovingAverageReadings) else it.store.profile.bpMovingAverageReadings,
                        weightGoalStartId = startId,
                    ),
                ),
            )
        }
        mutate { store ->
            store.copy(
                themeId = themeId,
                profile = store.profile.copy(
                    heightInches = if (heightOk) result.heightInches else store.profile.heightInches,
                    weightGoalLb = if (goalOk) result.weightGoalLb else store.profile.weightGoalLb,
                    bpMovingAverageReadings = if (bpOk) clampBpWindow(result.bpMovingAverageReadings) else store.profile.bpMovingAverageReadings,
                    weightGoalStartId = startId?.takeIf { id -> store.weightRecords.any { it.id == id } },
                ),
            )
        }
        return result.heightFeetError ?: result.heightInchesError ?: result.weightGoalError ?: result.bpError
    }

    fun saveBpTarget(systolicText: String, diastolicText: String): String? {
        val result = validateBloodPressureTarget(systolicText, diastolicText)
        if (!result.ok) return result.error
        mutate { store ->
            store.copy(
                profile = store.profile.copy(
                    bpTargetSystolic = result.systolic,
                    bpTargetDiastolic = result.diastolic,
                ),
            )
        }
        return null
    }

    fun saveBpReminders(
        morning: Boolean,
        evening: Boolean,
        morningTime: String,
        eveningTime: String,
    ) {
        val morningClock = morningTime.ifBlank { "07:00" }
        val eveningClock = eveningTime.ifBlank { "19:00" }
        mutate { store ->
            store.copy(
                profile = store.profile.copy(
                    bpMorningReminder = morning,
                    bpEveningReminder = evening,
                    bpMorningReminderTime = morningClock,
                    bpEveningReminderTime = eveningClock,
                ),
            )
        }
        BloodPressureReminders.schedule(getApplication(), morning, evening, morningClock, eveningClock)
    }

    fun saveWeightReminders(enabled: Boolean, time: String) {
        val clock = time.ifBlank { "07:00" }
        mutate { store ->
            store.copy(
                profile = store.profile.copy(
                    weightMorningReminder = enabled,
                    weightMorningReminderTime = clock,
                ),
            )
        }
        WeightReminders.schedule(getApplication(), enabled, clock)
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

    /** Starts an in-memory session. Returns its id, or null if no plan exists. */
    fun startWorkout(): String? {
        val store = _state.value.store
        val planId = lastUsedPlanId(store.workouts, store.plans) ?: return null
        val plan = store.plans.find { it.id == planId } ?: return null
        val workout = Workout(
            id = newId("workout"),
            date = todayIso(),
            planId = plan.id,
            activityTypeId = plan.activityTypeId,
            entries = emptyList(),
        )
        _state.update { it.copy(draftWorkout = workout) }
        return workout.id
    }

    /** Drops an unused draft so backing out of Log workout does not leave an empty session. */
    fun discardEmptyDraft(workoutId: String?) {
        val draft = _state.value.draftWorkout ?: return
        if (workoutId != null && draft.id != workoutId) return
        if (draft.entries.isEmpty()) _state.update { it.copy(draftWorkout = null) }
    }

    fun updateWorkoutDate(workoutId: String, date: String) {
        if (updateDraft(workoutId) { it.copy(date = date) }) return
        replaceWorkout(workoutId) { it.copy(date = date) }
    }

    fun updateWorkoutPlan(workoutId: String, planId: String) {
        val plan = _state.value.store.plans.find { it.id == planId } ?: return
        val apply = { workout: Workout ->
            if (workout.planId == planId) workout
            else workout.copy(planId = plan.id, activityTypeId = plan.activityTypeId, entries = emptyList())
        }
        if (updateDraft(workoutId, apply)) return
        replaceWorkout(workoutId, apply)
    }

    fun deleteWorkout(workoutId: String) {
        if (_state.value.draftWorkout?.id == workoutId) {
            _state.update { it.copy(draftWorkout = null) }
            return
        }
        val workout = _state.value.store.workouts.find { it.id == workoutId } ?: return
        offerUndo(DeletedRecord.Session(workout), "Workout deleted")
        replaceWorkouts { workouts -> workouts.filterNot { it.id == workoutId } }
    }

    fun upsertSavedEntry(workoutId: String, entry: WorkoutEntry) {
        val saved = if (entry.id.isBlank()) entry.copy(id = newId("entry")) else entry
        val draft = _state.value.draftWorkout
        if (draft != null && draft.id == workoutId) {
            val committed = draft.withEntry(saved)
            _state.update { it.copy(draftWorkout = null) }
            replaceWorkouts { workouts -> workouts.filterNot { it.id == committed.id } + committed }
            return
        }
        replaceWorkout(workoutId) { it.withEntry(saved) }
    }

    fun deleteWorkoutEntry(workoutId: String, entryId: String) {
        if (updateDraft(workoutId) { workout ->
                workout.copy(entries = workout.entries.filterNot { it.id == entryId })
            }
        ) return
        replaceWorkout(workoutId) { workout ->
            workout.copy(entries = workout.entries.filterNot { it.id == entryId })
        }
    }

    private fun updateDraft(workoutId: String, transform: (Workout) -> Workout): Boolean {
        val draft = _state.value.draftWorkout ?: return false
        if (draft.id != workoutId) return false
        _state.update { it.copy(draftWorkout = transform(draft)) }
        return true
    }

    private fun Workout.withEntry(entry: WorkoutEntry): Workout {
        val entries = entries.toMutableList()
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) entries[index] = entry else entries += entry
        return copy(entries = entries)
    }

    fun saveBloodPressure(readingId: String?, draft: BloodPressureSave) {
        mutate { store ->
            val readings = store.bloodPressureReadings.toMutableList()
            val reading = BloodPressureReading(
                id = readingId ?: newId("bp"),
                date = draft.date,
                time = draft.time.ifBlank { nowTime() },
                systolic = draft.systolic,
                diastolic = draft.diastolic,
                heartRate = draft.heartRate,
                period = draft.period,
                pairId = draft.pairId,
                averagedFrom = draft.averagedFrom,
                tags = draft.tags,
                note = draft.note,
            )
            val index = readings.indexOfFirst { it.id == reading.id }
            if (index >= 0) readings[index] = reading else readings += reading
            store.copy(bloodPressureReadings = readings)
        }
    }

    fun deleteBloodPressure(id: String) {
        val reading = _state.value.store.bloodPressureReadings.find { it.id == id } ?: return
        offerUndo(DeletedRecord.BloodPressure(reading), "Reading deleted")
        mutate { store ->
            store.copy(bloodPressureReadings = store.bloodPressureReadings.filterNot { it.id == id })
        }
    }

    fun saveWeightDisplayKg(useKg: Boolean) {
        mutate { store ->
            store.copy(profile = store.profile.copy(weightDisplayKg = useKg))
        }
    }

    fun saveWaist(waistText: String): String? {
        val result = validateWaistInches(waistText)
        if (result.second != null) return result.second
        mutate { store ->
            store.copy(profile = store.profile.copy(waistInches = result.first))
        }
        return null
    }

    fun saveWeight(
        recordId: String?,
        date: String,
        time: String,
        weight: Double,
        tags: List<String> = emptyList(),
        note: String = "",
    ) {
        mutate { store ->
            val records = store.weightRecords.toMutableList()
            val record = WeightRecord(
                id = recordId ?: newId("bw"),
                date = date,
                time = time.ifBlank { nowTime() },
                weight = roundToTenth(weight),
                tags = tags,
                note = note.trim(),
            )
            val index = records.indexOfFirst { it.id == record.id }
            if (index >= 0) records[index] = record else records += record
            store.copy(weightRecords = records)
        }
    }

    fun deleteWeight(id: String) {
        val record = _state.value.store.weightRecords.find { it.id == id } ?: return
        val clearedStart = _state.value.store.profile.weightGoalStartId == id
        offerUndo(DeletedRecord.Weight(record, clearedStart), "Reading deleted")
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

    fun undoDelete() {
        val item = lastDeleted ?: return
        lastDeleted = null
        _state.update { it.copy(undo = null) }
        when (item) {
            is DeletedRecord.BloodPressure -> mutate { store ->
                if (store.bloodPressureReadings.any { it.id == item.reading.id }) store
                else store.copy(bloodPressureReadings = store.bloodPressureReadings + item.reading)
            }
            is DeletedRecord.Weight -> mutate { store ->
                val records = if (store.weightRecords.any { it.id == item.record.id }) {
                    store.weightRecords
                } else {
                    store.weightRecords + item.record
                }
                val profile = if (item.clearedStart) {
                    store.profile.copy(weightGoalStartId = item.record.id)
                } else {
                    store.profile
                }
                store.copy(weightRecords = records, profile = profile)
            }
            is DeletedRecord.Session -> replaceWorkouts { workouts ->
                if (workouts.any { it.id == item.workout.id }) workouts else workouts + item.workout
            }
        }
    }

    fun dismissUndo(token: Long? = null) {
        if (token != null && _state.value.undo?.token != token) return
        lastDeleted = null
        _state.update { it.copy(undo = null) }
    }

    fun showSaved() {
        if (_state.value.undo != null) return
        _state.update { it.copy(notice = UndoToast("Saved")) }
    }

    fun dismissNotice(token: Long? = null) {
        if (token != null && _state.value.notice?.token != token) return
        _state.update { it.copy(notice = null) }
    }

    private fun offerUndo(record: DeletedRecord, message: String) {
        lastDeleted = record
        _state.update { it.copy(undo = UndoToast(message), notice = null) }
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
                    lastDeleted = null
                    _state.update {
                        it.copy(
                            store = updated.store,
                            themeId = updated.store.themeId,
                            savedThemeId = updated.store.themeId,
                            draftWorkout = null,
                            undo = null,
                            notice = null,
                        )
                    }
                    BloodPressureReminders.sync(getApplication(), updated.store.profile)
                    WeightReminders.sync(getApplication(), updated.store.profile)
                    onDone(null)
                }.onFailure { onDone(it.message ?: "Unable to load this file.") }
            }
        }
    }

    private fun replaceWorkout(workoutId: String, transform: (Workout) -> Workout) {
        replaceWorkouts { workouts ->
            workouts.map { workout -> if (workout.id == workoutId) transform(workout) else workout }
        }
    }

    private fun replaceWorkouts(transform: (List<Workout>) -> List<Workout>) {
        _state.update { it.copy(store = it.store.copy(workouts = transform(it.store.workouts))) }
        mutate { store -> store.copy(workouts = transform(store.workouts)) }
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
