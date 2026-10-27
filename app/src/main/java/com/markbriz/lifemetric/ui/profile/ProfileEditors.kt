package com.markbriz.lifemetric.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.markbriz.lifemetric.data.FitnessStore
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
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.SavedHint
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.appTextFieldColors
import com.markbriz.lifemetric.ui.components.fieldShape
import com.markbriz.lifemetric.ui.components.rememberSaveAck
import com.markbriz.lifemetric.ui.theme.AppThemes
import com.markbriz.lifemetric.ui.theme.LocalAppColors

internal typealias ProfileSave = (feet: String, inches: String, goal: String, window: Int, themeId: String, startId: String?) -> String?

internal fun saveProfileKeeping(
    store: FitnessStore,
    themeId: String,
    onSave: ProfileSave,
    feet: String = heightParts(store.profile.heightInches).first,
    inches: String = heightParts(store.profile.heightInches).second,
    goal: String = store.profile.weightGoalLb?.let { formatWeight(it) }.orEmpty(),
    window: Int = store.profile.bpMovingAverageReadings,
    startId: String? = store.profile.weightGoalStartId,
    nextTheme: String = themeId,
): String? = onSave(feet, inches, goal, window, nextTheme, startId)

@Composable
fun HeightEditorScreen(
    store: FitnessStore,
    themeId: String,
    onSave: ProfileSave,
    onBack: () -> Unit,
    backLabel: String = "Profile",
) {
    val colors = LocalAppColors.current
    val parts = heightParts(store.profile.heightInches)
    var feet by remember { mutableStateOf(parts.first) }
    var inches by remember { mutableStateOf(parts.second) }
    var error by remember { mutableStateOf<String?>(null) }
    val ack = rememberSaveAck()
    fun persist(nextFeet: String = feet, nextInches: String = inches) {
        error = saveProfileKeeping(store, themeId, onSave, feet = nextFeet, inches = nextInches)
        ack.mark(error == null)
    }
    AppScreen(
        title = "Height",
        lead = "Used to calculate BMI from your latest weight.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SurfaceCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("Feet", feet, {
                    feet = it.filter(Char::isDigit)
                    persist(nextFeet = feet)
                }, Modifier.weight(1f), hint = "e.g. 5", keyboardType = KeyboardType.Number)
                LabeledField("Inches", inches, {
                    inches = it.filter(Char::isDigit)
                    persist(nextInches = inches)
                }, Modifier.weight(1f), hint = "0–11", keyboardType = KeyboardType.Number)
            }
            if (error != null) Text(error!!, color = colors.danger)
            SavedHint(ack.visible)
        }
    }
}

@Composable
fun WeightGoalEditorScreen(
    store: FitnessStore,
    themeId: String,
    onSave: ProfileSave,
    onBack: () -> Unit,
    backLabel: String = "Profile",
) {
    val colors = LocalAppColors.current
    var goal by remember { mutableStateOf(store.profile.weightGoalLb?.let { formatWeight(it) }.orEmpty()) }
    var startId by remember { mutableStateOf(store.profile.weightGoalStartId.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val ack = rememberSaveAck()
    fun persist(nextGoal: String = goal, nextStart: String = startId) {
        error = saveProfileKeeping(store, themeId, onSave, goal = nextGoal, startId = nextStart.ifBlank { null })
        ack.mark(error == null)
    }
    AppScreen(
        title = "Weight goal",
        lead = "Set a target and the scale reading this goal starts from.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(
                    "−",
                    onClick = {
                        val base = goal.toDoubleOrNull() ?: latestWeight(store.weightRecords)?.weight ?: 150.0
                        goal = formatWeight(roundToTenth((base - 0.1).coerceAtLeast(0.0)))
                        persist()
                    },
                    contentDescription = "Decrease goal weight",
                )
                LabeledField(
                    "Weight (lb)",
                    goal,
                    {
                        goal = it
                        persist()
                    },
                    Modifier.weight(1f),
                    hint = "Use − / + to adjust by 0.1 lb.",
                    keyboardType = KeyboardType.Decimal,
                )
                GhostButton(
                    "+",
                    onClick = {
                        val base = goal.toDoubleOrNull() ?: latestWeight(store.weightRecords)?.weight ?: 150.0
                        goal = formatWeight(roundToTenth(base + 0.1))
                        persist()
                    },
                    contentDescription = "Increase goal weight",
                )
            }
            StartingWeightMenu(
                records = store.weightRecords,
                selectedId = startId,
                onSelect = {
                    startId = it
                    persist()
                },
            )
            if (error != null) Text(error!!, color = colors.danger)
            SavedHint(ack.visible)
        }
    }
}

@Composable
fun BpWindowEditorScreen(
    store: FitnessStore,
    themeId: String,
    onSave: ProfileSave,
    onBack: () -> Unit,
    backLabel: String = "Profile",
) {
    val colors = LocalAppColors.current
    var window by remember { mutableIntStateOf(store.profile.bpMovingAverageReadings) }
    var error by remember { mutableStateOf<String?>(null) }
    val ack = rememberSaveAck()
    fun persist(nextWindow: Int = window) {
        error = saveProfileKeeping(store, themeId, onSave, window = nextWindow)
        ack.mark(error == null)
    }
    AppScreen(
        title = "BP moving average",
        lead = "How many recent readings the blood pressure chart averages.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(
                    "−",
                    onClick = {
                        window = clampBpWindow(window - 1)
                        persist()
                    },
                    contentDescription = "Decrease average window",
                )
                LabeledField(
                    "Readings",
                    window.toString(),
                    { raw ->
                        val digits = raw.filter(Char::isDigit)
                        window = digits.toIntOrNull() ?: window
                        persist()
                    },
                    Modifier.weight(1f),
                    hint = "Average the last $MIN_BP_WINDOW–$MAX_BP_WINDOW readings. Default 10.",
                    keyboardType = KeyboardType.Number,
                )
                GhostButton(
                    "+",
                    onClick = {
                        window = clampBpWindow(window + 1)
                        persist()
                    },
                    contentDescription = "Increase average window",
                )
            }
            if (error != null) Text(error!!, color = colors.danger)
            SavedHint(ack.visible)
        }
    }
}

@Composable
fun ThemeEditorScreen(
    store: FitnessStore,
    themeId: String,
    onSave: ProfileSave,
    onBack: () -> Unit,
    backLabel: String = "Profile",
) {
    val colors = LocalAppColors.current
    val ack = rememberSaveAck()
    AppScreen(
        title = "Theme",
        lead = "Choose the colors used across the app.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SurfaceCard {
            AppThemes.forEach { theme ->
                val selected = theme.id == themeId
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(if (selected) 2.dp else 1.dp, if (selected) colors.brand else colors.border, RoundedCornerShape(16.dp))
                        .clickable {
                            val error = saveProfileKeeping(store, themeId, onSave, nextTheme = theme.id)
                            ack.mark(error == null)
                        }
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
            SavedHint(ack.visible)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StartingWeightMenu(
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
