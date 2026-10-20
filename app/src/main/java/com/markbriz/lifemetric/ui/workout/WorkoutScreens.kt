package com.markbriz.lifemetric.ui.workout

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.Workout
import com.markbriz.lifemetric.data.WorkoutDraft
import com.markbriz.lifemetric.data.WorkoutEntry
import com.markbriz.lifemetric.data.newId
import com.markbriz.lifemetric.domain.findPreviousLoad
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatDurationClock
import com.markbriz.lifemetric.domain.formatLoadWeight
import com.markbriz.lifemetric.domain.formatWorkoutEntrySummary
import com.markbriz.lifemetric.domain.parseDurationInput
import com.markbriz.lifemetric.domain.summarizeWorkoutEntries
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.appTextFieldColors
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.fieldShape
import com.markbriz.lifemetric.ui.theme.LocalAppColors

@Composable
fun WorkoutsScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val groups = store.workouts.sortedWith(compareByDescending<Workout> { it.date }.thenBy { it.id })
        .groupBy { it.date }
    var pending by remember { mutableStateOf<Workout?>(null) }
    AppScreen(
        eyebrow = "Workout log",
        title = "Workouts",
        lead = "Log strength, cardio, and other sessions",
        showFab = true,
        fabLabel = "New workout",
        onFab = onAdd,
    ) {
        if (groups.isEmpty()) {
            EmptyState("No workouts yet", "Create a workout for a date, then add activity sets to it.") {
                PrimaryButton("Create your first workout", onClick = onAdd)
            }
        } else {
            groups.forEach { (date, workouts) ->
                SurfaceCard {
                    Text(formatDate(date), color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    workouts.forEach { workout ->
                        val plan = store.plans.find { it.id == workout.planId }
                        val label = "${ActivityCatalog.workoutLabel(workout, plan)} · ${plan?.name ?: "Unknown plan"}"
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.layout.Column(
                                Modifier.weight(1f).clickable { onOpen(workout.id) },
                            ) {
                                Text(label, color = colors.ink, fontWeight = FontWeight.Medium)
                                Text(summarizeWorkoutEntries(workout.entries), color = colors.inkMuted, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onEdit(workout.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit $label", tint = colors.brandDark)
                            }
                            IconButton(onClick = { pending = workout }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete $label", tint = colors.danger)
                            }
                        }
                    }
                }
            }
        }
    }
    pending?.let { workout ->
        val planName = store.plans.find { it.id == workout.planId }?.name ?: "this plan"
        ConfirmDeleteDialog(
            title = "Delete this workout?",
            body = "Delete $planName on ${formatDate(workout.date)} and all ${workout.entries.size} ${if (workout.entries.size == 1) "entry" else "entries"}?",
            onConfirm = { onDelete(workout.id); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
fun WorkoutDetailScreen(
    workout: Workout?,
    plan: FitnessPlan?,
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (WorkoutEntry) -> Unit,
    onDeleteEntry: (WorkoutEntry) -> Unit,
) {
    val colors = LocalAppColors.current
    if (workout == null) {
        AppScreen(title = "Workout not found", lead = "That workout may have been deleted.", onBack = onBack, backLabel = "Workouts") {}
        return
    }
    var pending by remember { mutableStateOf<WorkoutEntry?>(null) }
    val schema = plan?.let { ActivityCatalog.schema(it.activityTypeId) } ?: "load"
    AppScreen(
        title = formatDate(workout.date),
        lead = "${ActivityCatalog.workoutLabel(workout, plan)} · ${plan?.name ?: "Unknown plan"} · ${summarizeWorkoutEntries(workout.entries)}",
        onBack = onBack,
        backLabel = "Workouts",
        showFab = true,
        fabLabel = ActivityCatalog.addEntryLabel(schema),
        onFab = onAddEntry,
    ) {
        if (workout.entries.isEmpty()) {
            EmptyState("No entries in this workout", "Add activity sets when you edit this workout.") {
                PrimaryButton(ActivityCatalog.addEntryLabel(schema), onClick = onAddEntry)
            }
        } else {
            workout.entries.forEach { entry ->
                val name = plan?.machines?.find { it.id == entry.machineId }?.name ?: "Unknown activity"
                SurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(formatWorkoutEntrySummary(entry), color = colors.inkMuted, fontSize = 13.sp)
                        }
                        IconButton(onClick = { onEditEntry(entry) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit $name", tint = colors.brandDark)
                        }
                        IconButton(onClick = { pending = entry }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove $name", tint = colors.danger)
                        }
                    }
                }
            }
        }
    }
    pending?.let { entry ->
        val name = plan?.machines?.find { it.id == entry.machineId }?.name ?: "this entry"
        ConfirmDeleteDialog(
            title = "Remove this entry?",
            body = "Remove $name from this workout?",
            confirmLabel = "Remove",
            onConfirm = { onDeleteEntry(entry); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
fun WorkoutFormScreen(
    store: FitnessStore,
    draft: WorkoutDraft?,
    missing: Boolean,
    onBack: () -> Unit,
    onDate: (String) -> Unit,
    onPlan: (String) -> Unit,
    onAddEntry: () -> Unit,
    onEditEntry: (WorkoutEntry) -> Unit,
    onRemoveEntry: (String) -> Unit,
    onSave: () -> String?,
) {
    val colors = LocalAppColors.current
    if (missing || draft == null) {
        AppScreen(title = "Workout not found", lead = "That workout may have been deleted.", onBack = onBack, backLabel = "Workouts") {}
        return
    }
    if (store.plans.isEmpty()) {
        AppScreen(
            title = "Create a plan first",
            lead = "Workout sessions are logged against a fitness plan. Add one from Profile.",
            onBack = onBack,
            backLabel = "Workouts",
        ) {}
        return
    }
    var dateError by remember { mutableStateOf<String?>(null) }
    var planError by remember { mutableStateOf<String?>(null) }
    var listError by remember { mutableStateOf<String?>(null) }
    val plan = store.plans.find { it.id == draft.planId }
    val schema = plan?.let { ActivityCatalog.schema(it.activityTypeId) } ?: "load"
    val isEdit = draft.workoutId != null
    AppScreen(
        title = if (isEdit) "Edit workout" else "New workout",
        lead = "Pick a date and plan, then add entries.",
        onBack = onBack,
        backLabel = if (draft.backToDetail) "Workout" else "Workouts",
    ) {
        SurfaceCard {
            DateTimeFields(draft.date.ifBlank { todayIso() }, "", onDate, {}, showTime = false)
            if (dateError != null) Text(dateError!!, color = colors.danger, fontSize = 12.sp)
            PlanMenu(store.plans, draft.planId, onPlan)
            if (planError != null) Text(planError!!, color = colors.danger, fontSize = 12.sp)
            PrimaryButton(ActivityCatalog.addEntryLabel(schema), onClick = {
                if (draft.planId.isBlank()) planError = "Select a workout plan before adding entries." else onAddEntry()
            })
        }
        if (draft.entries.isEmpty()) {
            EmptyState("No entries yet", "Add at least one entry before saving this workout.") {
                PrimaryButton(ActivityCatalog.addEntryLabel(schema), onClick = onAddEntry)
            }
        } else {
            draft.entries.groupBy { it.machineId }.forEach { (machineId, sets) ->
                val name = plan?.machines?.find { it.id == machineId }?.name ?: "Unknown activity"
                SurfaceCard {
                    Text(name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    sets.forEachIndexed { index, entry ->
                        if (index > 0) HorizontalDivider(color = colors.border)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                if (sets.size > 1) {
                                    Text("Set ${index + 1}", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text(formatWorkoutEntrySummary(entry), color = colors.inkMuted, fontSize = 13.sp)
                            }
                            IconButton(onClick = { onEditEntry(entry) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit $name set ${index + 1}", tint = colors.brandDark)
                            }
                            IconButton(onClick = { onRemoveEntry(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove $name set ${index + 1}", tint = colors.danger)
                            }
                        }
                    }
                }
            }
        }
        if (listError != null) Text(listError!!, color = colors.danger)
        PrimaryButton(if (isEdit) "Save workout" else "Create workout", onClick = {
            dateError = if (draft.date.isBlank()) "Date is required." else null
            planError = if (store.plans.none { it.id == draft.planId }) "Select a workout plan." else null
            listError = if (draft.entries.isEmpty()) "Add at least one workout entry before saving." else null
            if (dateError == null && planError == null && listError == null) {
                listError = onSave()
            }
        })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutEntryFormScreen(
    plan: FitnessPlan?,
    existing: WorkoutEntry?,
    workouts: List<Workout> = emptyList(),
    currentWorkoutId: String? = null,
    currentWorkoutDate: String = todayIso(),
    currentEntries: List<WorkoutEntry> = emptyList(),
    onBack: () -> Unit,
    onSave: (WorkoutEntry) -> Unit,
) {
    val colors = LocalAppColors.current
    if (plan == null) {
        AppScreen(title = "Workout plan required", lead = "Select a plan before adding an entry.", onBack = onBack, backLabel = "Workout") {}
        return
    }
    val schema = ActivityCatalog.schema(plan.activityTypeId)
    val isEdit = existing != null
    var machineId by remember(existing?.id, plan.id) { mutableStateOf(existing?.machineId ?: plan.machines.firstOrNull()?.id.orEmpty()) }
    fun previousLoad(forMachineId: String) = findPreviousLoad(
        workouts = workouts,
        planId = plan.id,
        machineId = forMachineId,
        currentWorkoutDate = currentWorkoutDate,
        currentEntries = currentEntries,
        currentWorkoutId = currentWorkoutId,
        excludeEntryId = existing?.id,
    )
    val initialPrevious = if (!isEdit && schema == "load") previousLoad(machineId) else null
    var weight by remember(existing?.id) {
        mutableStateOf(existing?.weight?.let(::formatLoadWeight) ?: initialPrevious?.weight?.let(::formatLoadWeight).orEmpty())
    }
    var reps by remember(existing?.id) {
        mutableStateOf(existing?.repetitions?.toString() ?: initialPrevious?.repetitions?.toString().orEmpty())
    }
    var distance by remember(existing?.id) { mutableStateOf(existing?.distanceMi?.toString().orEmpty()) }
    var duration by remember(existing?.id) {
        mutableStateOf(
            when (schema) {
                "distance_time" -> existing?.durationSec?.let { formatDurationClock(it) }.orEmpty()
                else -> existing?.durationSec?.let { (it / 60.0).let { m -> if (m % 1.0 == 0.0) m.toInt().toString() else m.toString() } }.orEmpty()
            },
        )
    }
    val defaults = ActivityCatalog.intervalDefaults(plan.activityTypeId)
    var rounds by remember(existing?.id) { mutableStateOf((existing?.rounds ?: defaults.first).toString()) }
    var work by remember(existing?.id) { mutableStateOf((existing?.workSec ?: defaults.second)?.toString().orEmpty()) }
    var rest by remember(existing?.id) { mutableStateOf((existing?.restSec ?: defaults.third)?.toString().orEmpty()) }
    var intensity by remember(existing?.id) { mutableStateOf(existing?.intensity.orEmpty()) }
    var note by remember(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }

    AppScreen(
        title = if (isEdit) "Edit entry" else "Add entry",
        lead = ActivityCatalog.label(plan.activityTypeId),
        onBack = onBack,
        backLabel = "Workout",
    ) {
        SurfaceCard {
            MachineMenu(plan, machineId) { nextId ->
                machineId = nextId
                if (schema == "load" && !isEdit) {
                    val previous = previousLoad(nextId)
                    if (previous != null) {
                        weight = formatLoadWeight(previous.weight)
                        reps = previous.repetitions.toString()
                    } else {
                        weight = ""
                        reps = ""
                    }
                }
            }
            if (errors["machineId"] != null) Text(errors["machineId"]!!, color = colors.danger, fontSize = 12.sp)
            when (schema) {
                "load" -> {
                    LabeledField("Weight", weight, { weight = it }, error = errors["weight"], keyboardType = KeyboardType.Decimal)
                    LabeledField("Repetitions", reps, { reps = it.filter(Char::isDigit) }, error = errors["repetitions"], keyboardType = KeyboardType.Number)
                }
                "distance_time" -> {
                    LabeledField("Distance (mi)", distance, { distance = it }, error = errors["distanceMi"], keyboardType = KeyboardType.Decimal)
                    LabeledField("Time", duration, { duration = it }, error = errors["durationSec"], hint = "mm:ss", keyboardType = KeyboardType.Text)
                }
                "duration", "sport" -> {
                    LabeledField("Minutes", duration, { duration = it }, error = errors["durationSec"], keyboardType = KeyboardType.Decimal)
                    if (schema == "sport") {
                        IntensityMenu(intensity) { intensity = it }
                    }
                }
                "interval" -> {
                    LabeledField("Rounds", rounds, { rounds = it.filter(Char::isDigit) }, error = errors["rounds"], keyboardType = KeyboardType.Number)
                    LabeledField("Work (sec)", work, { work = it.filter(Char::isDigit) }, error = errors["workSec"], keyboardType = KeyboardType.Number)
                    LabeledField("Rest (sec)", rest, { rest = it.filter(Char::isDigit) }, error = errors["restSec"], keyboardType = KeyboardType.Number)
                }
                else -> LabeledField("Note (optional)", note, { note = it }, singleLine = false, minLines = 3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Cancel", onBack, Modifier.weight(1f))
                PrimaryButton(if (isEdit) "Save entry" else "Add entry", modifier = Modifier.weight(1f), onClick = {
                    val next = mutableMapOf<String, String>()
                    if (machineId.isBlank() || plan.machines.none { it.id == machineId }) next["machineId"] = "Select an activity."
                    val built = WorkoutEntry(
                        id = existing?.id ?: newId("entry"),
                        planId = plan.id,
                        machineId = machineId,
                        schema = schema,
                    )
                    val entry = when (schema) {
                        "load" -> {
                            val w = weight.toDoubleOrNull()
                            val r = reps.toIntOrNull()
                            if (w == null) next["weight"] = "Enter a numeric weight."
                            else if (w < 0) next["weight"] = "Weight must be 0 or greater."
                            if (r == null) next["repetitions"] = "Enter a whole number of repetitions."
                            else if (r < 1) next["repetitions"] = "Repetitions must be at least 1."
                            built.copy(weight = w, repetitions = r)
                        }
                        "distance_time" -> {
                            val miles = distance.toDoubleOrNull()
                            val sec = parseDurationInput(duration)
                            if (miles == null || miles <= 0) next["distanceMi"] = "Enter a distance greater than 0."
                            if (sec == null) next["durationSec"] = "Enter time as mm:ss or minutes."
                            built.copy(distanceMi = miles, durationSec = sec)
                        }
                        "duration", "sport" -> {
                            val sec = parseDurationInput(duration)
                            if (sec == null) next["durationSec"] = "Enter minutes greater than 0."
                            built.copy(durationSec = sec, intensity = intensity.ifBlank { null })
                        }
                        "interval" -> {
                            val roundCount = rounds.toIntOrNull()
                            if (roundCount == null || roundCount < 1) next["rounds"] = "Enter at least 1 round."
                            val workSec = work.toIntOrNull()
                            val restSec = rest.toIntOrNull()
                            if (work.isNotBlank() && (workSec == null || workSec < 1)) next["workSec"] = "Enter whole-number seconds."
                            if (rest.isNotBlank() && (restSec == null || restSec < 1)) next["restSec"] = "Enter whole-number seconds."
                            built.copy(rounds = roundCount, workSec = workSec, restSec = restSec)
                        }
                        else -> built.copy(note = note.trim())
                    }
                    errors = next
                    if (next.isEmpty()) onSave(entry)
                })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanMenu(plans: List<FitnessPlan>, selectedId: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = plans.find { it.id == selectedId }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Workout plan",
            onValueChange = {},
            readOnly = true,
            label = { Text("Workout plan") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            plans.forEach { plan ->
                DropdownMenuItem(
                    text = { Text("${plan.name} · ${ActivityCatalog.planLabel(plan)}") },
                    onClick = { onSelect(plan.id); open = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MachineMenu(plan: FitnessPlan, selectedId: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val selected = plan.machines.find { it.id == selectedId }
    val label = ActivityCatalog.segmentFieldLabel(ActivityCatalog.schema(plan.activityTypeId))
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected?.name ?: "Select $label",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            plan.machines.forEach { machine ->
                DropdownMenuItem(text = { Text(machine.name) }, onClick = { onSelect(machine.id); open = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntensityMenu(selected: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val options = listOf("easy", "moderate", "hard")
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
        OutlinedTextField(
            value = selected.ifBlank { "Optional" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Intensity") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); open = false })
            }
        }
    }
}
