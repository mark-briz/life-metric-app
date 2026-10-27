package com.markbriz.lifemetric.ui.bp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
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
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatTime
import com.markbriz.lifemetric.domain.latestBloodPressure
import com.markbriz.lifemetric.domain.nowTime
import com.markbriz.lifemetric.domain.recentBloodPressure
import com.markbriz.lifemetric.domain.todayIso
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.BloodPressureChart
import com.markbriz.lifemetric.ui.components.BpBadge
import com.markbriz.lifemetric.ui.components.ConfirmDeleteDialog
import com.markbriz.lifemetric.ui.components.EmptyState
import com.markbriz.lifemetric.ui.components.GhostButton
import com.markbriz.lifemetric.ui.components.HistoryRow
import com.markbriz.lifemetric.ui.components.LabeledField
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.DateTimeFields
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.theme.LocalAppColors

@Composable
fun BloodPressureScreen(
    store: FitnessStore,
    onAdd: () -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEditBpWindow: () -> Unit,
) {
    val colors = LocalAppColors.current
    val latest = latestBloodPressure(store.bloodPressureReadings)
    val trend = recentBloodPressure(store.bloodPressureReadings, 10)
    val recent = store.bloodPressureReadings.sortedWith(
        compareByDescending<BloodPressureReading> { "${it.date}T${it.time}" }.thenByDescending { it.id },
    ).take(30)
    var pending by remember { mutableStateOf<BloodPressureReading?>(null) }

    AppScreen(
        eyebrow = "Vitals",
        title = "Blood pressure",
        lead = "Track systolic and diastolic readings over time.",
        showFab = true,
        fabLabel = "Add reading",
        onFab = onAdd,
    ) {
        if (latest == null) {
            EmptyState(
                "No blood pressure readings yet",
                "Add a reading with date, time, systolic, diastolic, and heart rate.",
                "Add your first reading",
                onAdd,
            )
        } else {
            val status = bloodPressureStatus(latest.systolic, latest.diastolic)
            SurfaceCard {
                Text("MOST RECENT READING", color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.1.sp)
                Text("${latest.systolic} / ${latest.diastolic} mmHg · ${latest.heartRate} bpm", color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                BpBadge(status)
                if (status == BpStatus.Severe) {
                    Text(
                        "Wait 1 minute and take it again. Call your health care professional if it stays this high. Call 911 if you have chest pain, shortness of breath, back pain, numbness, weakness, vision changes, or trouble speaking.",
                        color = colors.danger,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                    )
                }
                Text("${formatDate(latest.date)} · ${formatTime(latest.time)}", color = colors.inkMuted)
            }
        }
        if (store.bloodPressureReadings.isNotEmpty()) {
            SectionTitle("History")
            SurfaceCard {
                if (trend.size > 1) {
                    BloodPressureChart(trend, store.bloodPressureReadings, store.profile.bpMovingAverageReadings)
                } else {
                    Text("Add more readings to see a trend line.", color = colors.inkMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Average of last ${store.profile.bpMovingAverageReadings} readings",
                        color = colors.inkMuted,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextLink("Change", onClick = onEditBpWindow, contentDescription = "Change blood pressure average window")
                }
            }
            SectionTitle("Recent readings")
            recent.forEach { reading ->
                HistoryRow(
                    primary = "${reading.systolic} / ${reading.diastolic} mmHg · ${reading.heartRate} bpm",
                    secondary = "${formatDate(reading.date)} · ${formatTime(reading.time)}",
                    onClick = { onEdit(reading.id) },
                    trailing = { BpBadge(bloodPressureStatus(reading.systolic, reading.diastolic), compact = true) },
                    overflowLabel = "Delete",
                    onOverflow = { pending = reading },
                )
            }
        }
    }
    pending?.let { reading ->
        ConfirmDeleteDialog(
            title = "Delete this reading?",
            body = "Delete the ${formatDate(reading.date)} ${formatTime(reading.time)} reading of ${reading.systolic}/${reading.diastolic} mmHg, ${reading.heartRate} bpm?",
            onConfirm = { onDelete(reading.id); pending = null },
            onDismiss = { pending = null },
        )
    }
}

@Composable
fun BloodPressureFormScreen(
    existing: BloodPressureReading?,
    missing: Boolean,
    onBack: () -> Unit,
    onSave: (date: String, time: String, systolic: Int, diastolic: Int, heartRate: Int) -> Unit,
) {
    if (missing) {
        AppScreen(title = "Reading not found", lead = "That reading may have been deleted.", onBack = onBack, backLabel = "Blood pressure") {}
        return
    }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: todayIso()) }
    var time by remember(existing?.id) { mutableStateOf(existing?.time ?: nowTime()) }
    var systolic by remember(existing?.id) { mutableStateOf(existing?.systolic?.toString().orEmpty()) }
    var diastolic by remember(existing?.id) { mutableStateOf(existing?.diastolic?.toString().orEmpty()) }
    var heartRate by remember(existing?.id) { mutableStateOf(existing?.heartRate?.toString().orEmpty()) }
    var errors by remember { mutableStateOf(mapOf<String, String>()) }
    val isEdit = existing != null
    AppScreen(
        eyebrow = "Blood pressure / ${if (isEdit) "Edit" else "New"}",
        title = if (isEdit) "Edit reading" else "Add reading",
        lead = if (isEdit) "Update the date, time, pressure, and heart rate." else "Date and time default to now. Enter systolic over diastolic, then heart rate.",
        onBack = onBack,
        backLabel = "Blood pressure",
    ) {
        SurfaceCard {
            DateTimeFields(date, time, { date = it }, { time = it }, errors["date"], errors["time"])
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledField("Systolic", systolic, { systolic = it.filter(Char::isDigit) }, Modifier.weight(1f), errors["systolic"], keyboardType = KeyboardType.Number)
                LabeledField("Diastolic", diastolic, { diastolic = it.filter(Char::isDigit) }, Modifier.weight(1f), errors["diastolic"], keyboardType = KeyboardType.Number)
            }
            LabeledField("Heart rate (bpm)", heartRate, { heartRate = it.filter(Char::isDigit) }, error = errors["heartRate"], keyboardType = KeyboardType.Number)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Cancel", onBack, Modifier.weight(1f))
                PrimaryButton(if (isEdit) "Save changes" else "Save reading", modifier = Modifier.weight(1f), onClick = {
                    val next = mutableMapOf<String, String>()
                    if (date.isBlank()) next["date"] = "Date is required."
                    if (time.isBlank()) next["time"] = "Time is required."
                    val sys = systolic.toIntOrNull()
                    val dia = diastolic.toIntOrNull()
                    val hr = heartRate.toIntOrNull()
                    if (sys == null) next["systolic"] = "Enter a whole-number systolic reading."
                    else if (sys < 1) next["systolic"] = "Systolic must be at least 1."
                    if (dia == null) next["diastolic"] = "Enter a whole-number diastolic reading."
                    else if (dia < 1) next["diastolic"] = "Diastolic must be at least 1."
                    else if (sys != null && dia >= sys) next["diastolic"] = "Diastolic should be lower than systolic."
                    if (hr == null) next["heartRate"] = "Enter a whole-number heart rate."
                    else if (hr < 1) next["heartRate"] = "Heart rate must be at least 1."
                    errors = next
                    if (next.isEmpty() && sys != null && dia != null && hr != null) onSave(date, time, sys, dia, hr)
                })
            }
        }
    }
}
