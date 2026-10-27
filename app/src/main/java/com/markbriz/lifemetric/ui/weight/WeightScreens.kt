package com.markbriz.lifemetric.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.chronologicalWeights
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.domain.formatWeight
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.domain.nowTime
import com.markbriz.lifemetric.domain.recentWeights
import com.markbriz.lifemetric.domain.roundToTenth
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.domain.weightGoalProgress
import kotlin.math.roundToInt
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.BmiGauge
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.HistoryRow
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.components.WeightChart
import com.markbriz.lifemetric.ui.theme.LocalAppColors

@Composable
fun WeightScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditHeight: () -> Unit,
    onEditGoal: () -> Unit,
) {
    val colors = LocalAppColors.current
    val latest = latestWeight(store.weightRecords)
    val recent = recentWeights(store.weightRecords, 30)
    var pending by remember { mutableStateOf<WeightRecord?>(null) }
    val height = store.profile.heightInches
    val bmi = if (latest != null && height != null) calculateBmi(latest.weight, height) else Double.NaN
    val goalBmi = if (height != null && store.profile.weightGoalLb != null) calculateBmi(store.profile.weightGoalLb, height) else Double.NaN

    AppScreen(
        eyebrow = "Body weight",
        title = "Weight",
        lead = "Track scale readings over time.",
        showFab = true,
        fabLabel = "Record weight",
        onFab = onAdd,
    ) {
        if (latest == null) {
            EmptyState(
                "No weights recorded",
                "Record your first scale reading to start the history chart.",
                "Record weight",
                onAdd,
            )
        } else {
            SurfaceCard {
                Text("LAST RECORDED WEIGHT", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
                Text("${formatWeight(latest.weight)} lb", color = colors.ink, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                Text("${formatDate(latest.date)} · ${formatTime(latest.time)}", color = colors.inkMuted)
            }
        }
        if (store.weightRecords.isNotEmpty()) {
            SectionTitle("History")
            SurfaceCard { WeightChart(chronologicalWeights(store.weightRecords), store.profile.weightGoalLb) }
        }
        SectionTitle("Weight target")
        WeightTargetSection(store, onEditGoal)
        if (latest != null) {
            SectionTitle("Body Mass Index")
            if (height != null) {
                SurfaceCard { BmiGauge(bmi, goalBmi) }
            } else {
                EmptyState(
                    "Height needed for BMI",
                    "Add your height so BMI can be calculated from the latest weight.",
                    "Set height",
                    onEditHeight,
                )
            }
        }
        if (recent.isNotEmpty()) {
            val previousWeightById = remember(store.weightRecords) {
                val chronological = chronologicalWeights(store.weightRecords)
                chronological.mapIndexedNotNull { index, record ->
                    if (index == 0) null else record.id to chronological[index - 1].weight
                }.toMap()
            }
            SectionTitle("Recent readings")
            recent.forEach { record ->
                HistoryRow(
                    primary = "${formatWeight(record.weight)} lb",
                    secondary = "${formatDate(record.date)} · ${formatTime(record.time)}",
                    onClick = { onEdit(record.id) },
                    trailing = { WeightChangeArrow(record.weight, previousWeightById[record.id]) },
                    overflowLabel = "Delete",
                    onOverflow = { pending = record },
                )
            }
        }
    }
    pending?.let { record ->
        ConfirmDeleteDialog(
            title = "Delete this reading?",
            body = "Delete the ${formatDate(record.date)} reading of ${formatWeight(record.weight)} lb?",
            onConfirm = { onDelete(record.id); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
private fun WeightChangeArrow(weight: Double, previous: Double?) {
    if (previous == null) return
    val colors = LocalAppColors.current
    val delta = weight - previous
    val (icon, description, tint) = when {
        delta > 0 -> Triple(Icons.Default.ArrowUpward, "Weight increased", colors.danger)
        delta < 0 -> Triple(Icons.Default.ArrowDownward, "Weight decreased", colors.success)
        else -> Triple(Icons.Default.Remove, "No change", colors.ink)
    }
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = tint,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun WeightTargetSection(store: FitnessStore, onEditGoal: () -> Unit) {
    val colors = LocalAppColors.current
    val startExists = store.weightRecords.any { it.id == store.profile.weightGoalStartId }
    if (!startExists) {
        EmptyState(
            "Add a starting weight goal",
            "Choose a recorded scale reading as the start of this goal.",
            "Choose starting weight",
            onEditGoal,
        )
        return
    }
    val progress = weightGoalProgress(store.weightRecords, store.profile.weightGoalStartId, store.profile.weightGoalLb)
    if (progress == null) {
        EmptyState(
            "Add a target weight",
            "Set a target weight to see progress from your starting reading.",
            "Set target weight",
            onEditGoal,
        )
        return
    }
    SurfaceCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Column {
                Text("START", color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text("${formatWeight(progress.startLb)} lb", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Text(
                "${progress.percentComplete.roundToInt()}%",
                color = colors.brandDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("TARGET", color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text("${formatWeight(progress.targetLb)} lb", color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
        LinearProgressIndicator(
            progress = { (progress.percentComplete / 100.0).toFloat() },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(999.dp)),
            color = colors.brand,
            trackColor = colors.surfaceMuted,
        )
        Text("Percent complete", color = colors.inkFaint, fontSize = 12.sp)
        HorizontalDivider(color = colors.border)
        GoalStatRow("Target to lose", "${formatWeight(progress.targetToLoseLb)} lb")
        HorizontalDivider(color = colors.border)
        GoalStatRow("Weight lost", "${formatWeight(progress.weightLostLb)} lb")
        TextLink("Change goal", onClick = onEditGoal)
    }
}

@Composable
private fun GoalStatRow(label: String, value: String) {
    val colors = LocalAppColors.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = colors.inkMuted, fontSize = 14.sp)
        Text(value, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun WeightFormScreen(
    existing: WeightRecord?,
    suggested: Double,
    missing: Boolean,
    onBack: () -> Unit,
    onSave: (date: String, time: String, weight: Double) -> Unit,
) {
    if (missing) {
        AppScreen(title = "Reading not found", lead = "That weight reading may have been deleted.", onBack = onBack, backLabel = "Weight") {}
        return
    }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: todayIso()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time ?: nowTime()) }
    var weight by remember(existing?.id) { mutableStateOf(formatWeight(existing?.weight ?: suggested)) }
    var error by remember { mutableStateOf<String?>(null) }
    val isEdit = existing != null
    AppScreen(
        eyebrow = "Weight / ${if (isEdit) "Edit" else "New"}",
        title = if (isEdit) "Edit weight" else "Record weight",
        lead = "Date and time default to now. Adjust weight by 0.1 lb.",
        onBack = onBack,
        backLabel = "Weight",
    ) {
        SurfaceCard {
            DateTimeFields(date, time, { date = it }, { time = it })
            Text("Weight (lb)", color = LocalAppColors.current.inkMuted, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                GhostButton(
                    "−",
                    onClick = {
                        val current = weight.toDoubleOrNull() ?: 0.0
                        weight = formatWeight(roundToTenth((current - 0.1).coerceAtLeast(0.0)))
                    },
                    contentDescription = "Decrease weight",
                )
                LabeledField(
                    label = "lb",
                    value = weight,
                    onValueChange = { weight = it },
                    modifier = Modifier.weight(1f),
                    error = error,
                    keyboardType = KeyboardType.Decimal,
                )
                GhostButton(
                    "+",
                    onClick = {
                        val current = weight.toDoubleOrNull() ?: 0.0
                        weight = formatWeight(roundToTenth(current + 0.1))
                    },
                    contentDescription = "Increase weight",
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Cancel", onBack, Modifier.weight(1f))
                PrimaryButton(if (isEdit) "Save changes" else "Save weight", modifier = Modifier.weight(1f), onClick = {
                    val parsed = weight.toDoubleOrNull()
                    error = when {
                        date.isBlank() -> "Date is required."
                        time.isBlank() -> "Time is required."
                        parsed == null -> "Enter a weight value."
                        parsed < 0 -> "Weight must be 0 or greater."
                        else -> null
                    }
                    if (error == null && parsed != null) onSave(date, time, roundToTenth(parsed))
                })
            }
        }
    }
}
