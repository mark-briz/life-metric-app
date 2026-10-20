package com.markbriz.lifemetric.ui.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.ActivityCatalog
import com.markbriz.lifemetric.data.FitnessPlan
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.Machine
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.domain.MAX_BP_WINDOW
import com.markbriz.lifemetric.domain.MIN_BP_WINDOW
import com.markbriz.lifemetric.domain.clampBpWindow
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.domain.formatWeight
import com.markbriz.lifemetric.domain.heightParts
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.domain.recentWeights
import com.markbriz.lifemetric.domain.roundToTenth
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.MessageDialog
import com.markbriz.lifemetric.ui.components.DangerOutlineButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.markbriz.lifemetric.data.ParsedAccountExport
import com.markbriz.lifemetric.data.buildAccountExport
import com.markbriz.lifemetric.data.parseAccountExport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.appTextFieldColors
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SettingsRow
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.legal.DISCLAIMER
import com.markbriz.lifemetric.ui.theme.AppThemes
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import com.markbriz.lifemetric.ui.components.fieldShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    store: FitnessStore,
    themeId: String,
    onPreviewTheme: (String) -> Unit,
    onRevertTheme: () -> Unit,
    onSave: (feet: String, inches: String, goal: String, window: Int, themeId: String, startId: String?) -> String?,
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
    val parts = heightParts(store.profile.heightInches)
    var feet by remember(store.profile.heightInches) { mutableStateOf(parts.first) }
    var inches by remember(store.profile.heightInches) { mutableStateOf(parts.second) }
    var goal by remember(store.profile.weightGoalLb) {
        mutableStateOf(store.profile.weightGoalLb?.let { formatWeight(it) }.orEmpty())
    }
    var startId by remember(store.profile.weightGoalStartId) {
        mutableStateOf(store.profile.weightGoalStartId.orEmpty())
    }
    var window by remember(store.profile.bpMovingAverageReadings) { mutableIntStateOf(store.profile.bpMovingAverageReadings) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(false) }
    var confirmErase by remember { mutableStateOf(false) }
    var confirmEraseAgain by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { onRevertTheme() } }

    AppScreen(eyebrow = "App", title = "Profile", lead = "Your height, appearance, and weight goal.") {
        SurfaceCard {
            SectionTitle("Personal info")
            Text("Height", color = colors.ink, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("Feet", feet, { feet = it.filter(Char::isDigit); saved = false }, Modifier.weight(1f), hint = "e.g. 5", keyboardType = KeyboardType.Number)
                LabeledField("Inches", inches, { inches = it.filter(Char::isDigit); saved = false }, Modifier.weight(1f), hint = "0–11", keyboardType = KeyboardType.Number)
            }
            SectionTitle("Themes")
            AppThemes.forEach { theme ->
                val selected = theme.id == themeId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .border(if (selected) 2.dp else 1.dp, if (selected) colors.brand else colors.border, RoundedCornerShape(16.dp))
                        .clickable { onPreviewTheme(theme.id); saved = false }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        theme.preview.forEach { swatch ->
                            Box(Modifier.size(18.dp).clip(RoundedCornerShape(6.dp)).background(swatch))
                        }
                    }
                    Column {
                        Text(theme.label, color = colors.ink, fontWeight = FontWeight.Bold)
                        Text(theme.description, color = colors.inkMuted, fontSize = 12.sp)
                    }
                }
            }
            SectionTitle("Weight goal")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("−", onClick = {
                    val base = goal.toDoubleOrNull() ?: latestWeight(store.weightRecords)?.weight ?: 150.0
                    goal = formatWeight(roundToTenth((base - 0.1).coerceAtLeast(0.0)))
                    saved = false
                })
                LabeledField("Weight (lb)", goal, { goal = it; saved = false }, Modifier.weight(1f), hint = "Use − / + to adjust by 0.1 lb.", keyboardType = KeyboardType.Decimal)
                GhostButton("+", onClick = {
                    val base = goal.toDoubleOrNull() ?: latestWeight(store.weightRecords)?.weight ?: 150.0
                    goal = formatWeight(roundToTenth(base + 0.1))
                    saved = false
                })
            }
            StartingWeightMenu(
                records = store.weightRecords,
                selectedId = startId,
                onSelect = { startId = it; saved = false },
            )
            SectionTitle("BP moving average")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("−", onClick = { window = clampBpWindow(window - 1); saved = false })
                LabeledField(
                    "Readings",
                    window.toString(),
                    { raw ->
                        val digits = raw.filter(Char::isDigit)
                        window = digits.toIntOrNull() ?: window
                        saved = false
                    },
                    Modifier.weight(1f),
                    hint = "Average the last $MIN_BP_WINDOW–$MAX_BP_WINDOW readings. Default 10.",
                    keyboardType = KeyboardType.Number,
                )
                GhostButton("+", onClick = { window = clampBpWindow(window + 1); saved = false })
            }
            if (saved) Text("Saved", color = colors.success, fontWeight = FontWeight.SemiBold)
            if (error != null) Text(error!!, color = colors.danger)
            PrimaryButton("Save profile", onClick = {
                val message = onSave(feet, inches, goal, window, themeId, startId.ifBlank { null })
                error = message
                saved = message == null
            })
        }
        SettingsRow("Workout Plans", "Create and edit fitness plans and their activities.", onPlans)
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
                onImport(imported.data, themeToApply) { error ->
                    transferring = null
                    message = if (error == null) {
                        "Data loaded" to "Your profile, plans, workouts, weight, and blood pressure now match the file."
                    } else {
                        "Could not load data" to error
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartingWeightMenu(
    records: List<WeightRecord>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    val colors = LocalAppColors.current
    val enabled = records.isNotEmpty()
    var open by remember { mutableStateOf(false) }
    val selected = records.find { it.id == selectedId }
    val label = when {
        !enabled -> "Record a weight first"
        selected != null -> "${formatDate(selected.date)} · ${formatWeight(selected.weight)} lb"
        else -> "Not selected"
    }
    ExposedDropdownMenuBox(expanded = open && enabled, onExpandedChange = { if (enabled) open = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text("Starting weight") },
            supportingText = {
                Text(
                    if (enabled) "Choose an existing reading as the start of this goal."
                    else "Grayed out until you record at least one weight.",
                )
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open && enabled) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Not selected") },
                onClick = { onSelect(""); open = false },
            )
            recentWeights(records, records.size).forEach { record ->
                DropdownMenuItem(
                    text = { Text("${formatDate(record.date)} · ${formatTime(record.time)} · ${formatWeight(record.weight)} lb") },
                    onClick = { onSelect(record.id); open = false },
                )
            }
        }
    }
    if (!enabled) {
        Text("Starting weight uses a recorded scale reading.", color = colors.inkFaint, fontSize = 12.sp)
    }
}

@Composable
fun PlansScreen(plans: List<FitnessPlan>, onBack: () -> Unit, onAdd: () -> Unit, onEdit: (String) -> Unit, onOpen: (String) -> Unit) {
    val colors = LocalAppColors.current
    AppScreen(
        eyebrow = "Settings · Fitness plans",
        title = "Your plans",
        lead = "Build activity lists for each training focus, then log sets against them.",
        onBack = onBack,
        backLabel = "Profile",
        showFab = true,
        fabLabel = "New plan",
        onFab = onAdd,
    ) {
        if (plans.isEmpty()) {
            EmptyState("No plans yet", "Create a fitness plan, add activities, then start logging workouts.") {
                PrimaryButton("Create your first plan", onClick = onAdd)
            }
        } else {
            plans.forEach { plan ->
                SurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).clickable { onEdit(plan.id) }) {
                            Text(plan.name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                ActivityCatalog.planLabel(plan) + if (plan.description.isNotBlank()) " · ${plan.description}" else "",
                                color = colors.inkMuted,
                                fontSize = 13.sp,
                            )
                        }
                        Row(Modifier.clickable { onOpen(plan.id) }, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${plan.machines.size} ${if (plan.machines.size == 1) "activity" else "activities"}",
                                color = colors.brandDark,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = "View activities", tint = colors.brandDark)
                        }
                    }
                }
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
        backLabel = "Plans",
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
    val colors = LocalAppColors.current
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
            EmptyState("No activities on this plan", "Add the activities you'll log so workout entries can pick from this list.") {
                PrimaryButton("Add an activity", onClick = onAdd)
            }
        } else {
            plan.machines.forEach { machine ->
                SurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(machine.name, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(machine.description.ifBlank { "No description" }, color = if (machine.description.isBlank()) colors.inkFaint else colors.inkMuted, fontSize = 13.sp)
                        }
                        IconButton(onClick = { onEdit(machine) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit ${machine.name}", tint = colors.brandDark)
                        }
                        IconButton(onClick = { pending = machine }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove ${machine.name}", tint = colors.danger)
                        }
                    }
                }
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
        backLabel = "Plan",
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
