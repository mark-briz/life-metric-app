package com.markbriz.lifemetric.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.ParsedAccountExport
import com.markbriz.lifemetric.data.buildAccountExport
import com.markbriz.lifemetric.data.parseAccountExport
import com.markbriz.lifemetric.domain.formatHeight
import com.markbriz.lifemetric.domain.formatWeight
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.DangerOutlineButton
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.HistoryRow
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.MessageDialog
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SettingsRow
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.appTextFieldColors
import com.markbriz.lifemetric.ui.components.fieldShape
import com.markbriz.lifemetric.ui.legal.DISCLAIMER
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import com.markbriz.lifemetric.ui.theme.themeById
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
fun ProfileScreen(
    store: FitnessStore,
    themeId: String,
    onHeight: () -> Unit,
    onGoal: () -> Unit,
    onBpWindow: () -> Unit,
    onTheme: () -> Unit,
    onPlans: () -> Unit,
    /** Returns the current tracking data and the saved theme id for export. */
    exportSnapshot: () -> Pair<FitnessStore, String>,
    /** Replaces the data on this device; reports an error message or null on success. */
    onImport: (data: FitnessStore, themeId: String, onDone: (String?) -> Unit) -> Unit,
    onDisclaimer: () -> Unit,
    onAbout: () -> Unit,
    onEraseAll: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var transferring by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingImport by remember { mutableStateOf<ParsedAccountExport?>(null) }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            transferring = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            try {
                val parsed = withContext(Dispatchers.IO) { parseAccountExport(readPickedJson(context, uri)) }
                pendingImport = parsed
            } catch (err: Exception) {
                message = "Could not load data" to (err.message ?: "Unable to load this file.")
                transferring = null
            }
        }
    }

    // Plain lambdas on purpose: local `fun` declarations here were mis-dispatched by the Compose
    // compiler (tapping Export invoked the Load handler), so keep these as values.
    val exportData: () -> Unit = {
        if (transferring == null) {
            transferring = "export"
            scope.launch {
                try {
                    val (data, savedTheme) = exportSnapshot()
                    val json = withContext(Dispatchers.IO) {
                        buildAccountExport(data, savedTheme, Instant.now().toString())
                    }
                    shareAccountJson(context, json)
                } catch (err: Exception) {
                    message = "Could not export data" to (err.message ?: "Unable to export this data.")
                } finally {
                    transferring = null
                }
            }
        }
    }

    val loadData: () -> Unit = {
        if (transferring == null) {
            transferring = "load"
            try {
                pickFile.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
            } catch (err: Exception) {
                message = "Could not load data" to (err.message ?: "Unable to open a file picker on this device.")
                transferring = null
            }
        }
    }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmEraseAgain by remember { mutableStateOf(false) }
    val start = store.weightRecords.find { it.id == store.profile.weightGoalStartId }
    val startValue = start?.let { "${formatWeight(it.weight)} lb" } ?: "Not selected"

    AppScreen(eyebrow = "App", title = "Profile", lead = "Height, goals, appearance, and data on this device.") {
        SectionTitle("Personal")
        SettingsRow("Height", "Used to calculate BMI on the Weight tab.", onHeight, formatHeight(store.profile.heightInches))
        SectionTitle("Goals & charts")
        SettingsRow(
            "Weight goal",
            "Target weight used on the Weight tab.",
            onGoal,
            store.profile.weightGoalLb?.let { "${formatWeight(it)} lb" } ?: "Not set",
        )
        SettingsRow("Starting weight", "The scale reading this goal starts from.", onGoal, startValue)
        SettingsRow(
            "BP average window",
            "How many recent readings the blood pressure chart averages.",
            onBpWindow,
            "${store.profile.bpMovingAverageReadings}",
        )
        SectionTitle("Appearance")
        SettingsRow("Theme", "Colors used across the app.", onTheme, themeById(themeId).label)
        SectionTitle("Workouts")
        SettingsRow(
            "Plans",
            "Create and edit fitness plans and their activities.",
            onPlans,
            "${store.plans.size}",
        )
        SectionTitle("Data")
        SettingsRow(
            "Export data",
            if (transferring == "export") "Preparing your JSON file…"
            else "Save your profile, plans, workouts, weight, and blood pressure as a JSON file.",
            onClick = exportData,
        )
        SettingsRow(
            "Load data",
            if (transferring == "load") "Loading data…" else "Replace the data on this device with a JSON file.",
            onClick = loadData,
        )
        SectionTitle("Legal")
        SettingsRow("Health disclaimer", "Not medical advice, not a medical device, and not for emergencies.", onDisclaimer)
        SectionTitle("App")
        SettingsRow("About", "App name and current version.", onAbout)
        DangerOutlineButton("Erase all data", onClick = { confirmErase = true })
        Text("All data is stored only on this phone and is protected by your device lock.", color = colors.inkMuted, fontSize = 13.sp)
    }
    pendingImport?.let { imported ->
        val summary = listOf(
            "${imported.data.plans.size} plans",
            "${imported.data.workouts.size} workouts",
            "${imported.data.weightRecords.size} weight readings",
            "${imported.data.bloodPressureReadings.size} blood pressure readings",
        ).joinToString(", ")
        ConfirmDeleteDialog(
            title = "Replace your data?",
            body = "This file has $summary. It replaces the profile, plans, workouts, weight, and blood pressure on this device.",
            confirmLabel = "Load data",
            onConfirm = {
                pendingImport = null
                val themeToApply = imported.themeId ?: themeId
                onImport(imported.data, themeToApply) { loadError ->
                    transferring = null
                    if (loadError == null) {
                        message = "Data loaded" to "Your profile, plans, workouts, weight, and blood pressure now match the file."
                    } else {
                        message = "Could not load data" to loadError
                    }
                }
            },
            onDismiss = { pendingImport = null; transferring = null },
        )
    }
    message?.let { (title, body) ->
        MessageDialog(title = title, body = body, onDismiss = { message = null })
    }
    if (confirmErase) {
        ConfirmDeleteDialog(
            title = "Erase all data?",
            body = "This permanently removes all data stored on this device (profile, weight, blood pressure, workouts, and plans). This cannot be undone. Use Export data first if you want a copy.",
            confirmLabel = "Continue",
            onConfirm = { confirmErase = false; confirmEraseAgain = true },
            onDismiss = { confirmErase = false },
        )
    }
    if (confirmEraseAgain) {
        ConfirmDeleteDialog(
            title = "Are you sure?",
            body = "Erase everything on this device now?",
            confirmLabel = "Erase all data",
            onConfirm = { confirmEraseAgain = false; onEraseAll() },
            onDismiss = { confirmEraseAgain = false },
        )
    }
}

@Composable
fun PlansScreen(
    plans: List<FitnessPlan>,
    onBack: () -> Unit,
    backLabel: String = "Profile",
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onOpen: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    AppScreen(
        eyebrow = "Settings · Fitness plans",
        title = "Your plans",
        lead = "Build activity lists for each training focus, then log sets against them.",
        onBack = onBack,
        backLabel = backLabel,
        showFab = true,
        fabLabel = "New plan",
        onFab = onAdd,
    ) {
        if (plans.isEmpty()) {
            EmptyState(
                "No plans yet",
                "Create a fitness plan, add activities, then start logging workouts.",
                "Create your first plan",
                onAdd,
            )
        } else {
            plans.forEach { plan ->
                HistoryRow(
                    primary = plan.name,
                    secondary = ActivityCatalog.planLabel(plan) + if (plan.description.isNotBlank()) " · ${plan.description}" else "",
                    onClick = { onOpen(plan.id) },
                    trailing = {
                        Text(
                            "${plan.machines.size} ${if (plan.machines.size == 1) "activity" else "activities"}",
                            color = colors.brandDark,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                        )
                    },
                    overflowLabel = "Edit",
                    onOverflow = { onEdit(plan.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanFormScreen(
    existing: FitnessPlan?,
    missing: Boolean,
    onBack: () -> Unit,
    backLabel: String = "Plans",
    onSave: (name: String, description: String, typeId: String) -> String?,
) {
    if (missing) {
        AppScreen(title = "Plan not found", lead = "That plan may have been deleted.", onBack = onBack, backLabel = "Plans") {}
        return
    }
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var typeId by remember(existing?.id) { mutableStateOf(existing?.activityTypeId ?: ActivityCatalog.DEFAULT_TYPE) }
    var familyId by remember(existing?.id) { mutableStateOf(ActivityCatalog.familyOf(typeId)) }
    var error by remember { mutableStateOf<String?>(null) }
    var familyOpen by remember { mutableStateOf(false) }
    var typeOpen by remember { mutableStateOf(false) }
    val isEdit = existing != null
    AppScreen(
        eyebrow = if (isEdit) "Edit plan" else "New plan",
        title = if (isEdit) "Update plan" else "Create a plan",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SurfaceCard {
            LabeledField("Name", name, { name = it }, error = error)
            ExposedDropdownMenuBox(expanded = familyOpen, onExpandedChange = { familyOpen = it }) {
                OutlinedTextField(
                    value = ActivityCatalog.families.find { it.id == familyId }?.label ?: "Family",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Activity family") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(familyOpen) },
                    modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = fieldShape,
                    colors = appTextFieldColors(),
                )
                ExposedDropdownMenu(expanded = familyOpen, onDismissRequest = { familyOpen = false }) {
                    ActivityCatalog.families.forEach { family ->
                        DropdownMenuItem(text = { Text(family.label) }, onClick = {
                            familyId = family.id
                            typeId = ActivityCatalog.typesIn(family.id).first().id
                            familyOpen = false
                        })
                    }
                }
            }
            ExposedDropdownMenuBox(expanded = typeOpen, onExpandedChange = { typeOpen = it }) {
                OutlinedTextField(
                    value = ActivityCatalog.label(typeId),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Activity type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeOpen) },
                    modifier = Modifier.menuAnchor(androidx.compose.material3.ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = fieldShape,
                    colors = appTextFieldColors(),
                )
                ExposedDropdownMenu(expanded = typeOpen, onDismissRequest = { typeOpen = false }) {
                    ActivityCatalog.typesIn(familyId).forEach { type ->
                        DropdownMenuItem(text = { Text(type.label) }, onClick = { typeId = type.id; typeOpen = false })
                    }
                }
            }
            Text(
                "This chooses what you log: weight and reps, distance and time, rounds, or a note.",
                color = LocalAppColors.current.inkMuted,
                fontSize = 13.sp,
            )
            LabeledField("Description (optional)", description, { description = it }, singleLine = false, minLines = 3)
            PrimaryButton(if (isEdit) "Save changes" else "Create plan", onClick = {
                error = onSave(name, description, typeId)
            })
        }
    }
}

@Composable
fun PlanDetailScreen(
    plan: FitnessPlan?,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Machine) -> Unit,
    onDelete: (Machine) -> Unit,
) {
    if (plan == null) {
        AppScreen(title = "Plan not found", lead = "That plan may have been deleted.", onBack = onBack, backLabel = "Plans") {}
        return
    }
    var pending by remember { mutableStateOf<Machine?>(null) }
    AppScreen(
        eyebrow = "${ActivityCatalog.planLabel(plan)} · Fitness plan",
        title = plan.name,
        lead = plan.description.ifBlank { "No description." },
        onBack = onBack,
        backLabel = "Plans",
        showFab = true,
        fabLabel = "Add an activity",
        onFab = onAdd,
    ) {
        SectionTitle("Activities")
        if (plan.machines.isEmpty()) {
            EmptyState(
                "No activities on this plan",
                "Add the activities you'll log, for example Chest Press, Easy run, or a 20-minute stretch.",
                "Add an activity",
                onAdd,
            )
        } else {
            plan.machines.forEach { machine ->
                HistoryRow(
                    primary = machine.name,
                    secondary = machine.description.ifBlank { "No description" },
                    onClick = { onEdit(machine) },
                    overflowLabel = "Remove",
                    onOverflow = { pending = machine },
                )
            }
        }
    }
    pending?.let { machine ->
        ConfirmDeleteDialog(
            title = "Remove this activity?",
            body = "Remove ${machine.name} from ${plan.name}?",
            confirmLabel = "Remove",
            onConfirm = { onDelete(machine); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
fun ActivityFormScreen(
    planMissing: Boolean,
    activityMissing: Boolean,
    existing: Machine?,
    onBack: () -> Unit,
    backLabel: String = "Plan",
    onSave: (name: String, description: String) -> String?,
) {
    if (planMissing) {
        AppScreen(title = "Plan not found", lead = "That plan may have been deleted.", onBack = onBack, backLabel = "Plans") {}
        return
    }
    if (activityMissing) {
        AppScreen(title = "Activity not found", lead = "That activity may have been removed.", onBack = onBack, backLabel = "Plan") {}
        return
    }
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var description by remember(existing?.id) { mutableStateOf(existing?.description.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val isEdit = existing != null
    AppScreen(
        eyebrow = if (isEdit) "Edit activity" else "New activity",
        title = if (isEdit) "Update activity" else "Add an activity",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SurfaceCard {
            LabeledField("Name", name, { name = it }, error = error)
            LabeledField("Description (optional)", description, { description = it }, singleLine = false, minLines = 3)
            PrimaryButton(if (isEdit) "Save activity" else "Add activity", onClick = { error = onSave(name, description) })
        }
    }
}

@Composable
fun LegalScreen(title: String, body: String, onBack: () -> Unit) {
    AppScreen(eyebrow = "Legal", title = title, onBack = onBack, backLabel = "Profile") {
        SurfaceCard {
            Text(body, color = LocalAppColors.current.ink, fontSize = 14.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    AppScreen(eyebrow = "App", title = "About", lead = "Life Metric version on this device.", onBack = onBack, backLabel = "Profile") {
        SurfaceCard {
            Text("Life Metric", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("VERSION", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
            Text("1.0.1", color = colors.ink, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
            Text("Data is stored only on this device.", color = colors.inkMuted)
        }
    }
}

@Composable
fun DisclaimerScreen(onBack: () -> Unit) = LegalScreen("Health disclaimer", DISCLAIMER, onBack)
