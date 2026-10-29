package com.markbriz.lifemetric.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.domain.bloodPressureVisitSummary
import com.markbriz.lifemetric.domain.calculateBmi
import com.markbriz.lifemetric.domain.formatVisitSummaryLine
import com.markbriz.lifemetric.domain.formatWorkoutWeekLine
import com.markbriz.lifemetric.domain.homeWeightChartModel
import com.markbriz.lifemetric.domain.latestBloodPressure
import com.markbriz.lifemetric.domain.latestWeight
import com.markbriz.lifemetric.domain.latestWorkout
import com.markbriz.lifemetric.domain.recentBloodPressure
import com.markbriz.lifemetric.domain.workoutSessionsByWeek
import com.markbriz.lifemetric.domain.workoutWeekSummary
import com.markbriz.lifemetric.ui.bp.BloodPressureHero
import com.markbriz.lifemetric.ui.components.BloodPressureChart
import com.markbriz.lifemetric.ui.components.BmiGauge
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.SecondaryButton
import com.markbriz.lifemetric.ui.components.SectionTitle
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.WeightChart
import com.markbriz.lifemetric.ui.components.WorkoutBarChart
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import com.markbriz.lifemetric.ui.weight.WeightHero
import com.markbriz.lifemetric.ui.workout.WorkoutHero

@Composable
fun HomeScreen(
    store: FitnessStore,
    onOpenBp: () -> Unit,
    onOpenWeight: () -> Unit,
    onOpenWorkouts: () -> Unit,
    onAddBp: () -> Unit,
    onAddWeight: () -> Unit,
    onAddWorkout: () -> Unit,
    onSetHeight: () -> Unit,
) {
    val colors = LocalAppColors.current
    val useKg = store.profile.weightDisplayKg
    val latestBp = latestBloodPressure(store.bloodPressureReadings)
    val latestWeight = latestWeight(store.weightRecords)
    val lastWorkout = latestWorkout(store.workouts)
    val bpDone = latestBp != null
    val weightDone = latestWeight != null
    val workoutDone = lastWorkout != null

    AppScreen(title = "Dashboard") {
        if (!(bpDone && weightDone && workoutDone)) {
            SectionTitle("Get started")
            SurfaceCard {
                ChecklistItem("Record blood pressure", "Systolic and diastolic. Heart rate is optional.", bpDone, onAddBp)
                ChecklistItem("Record weight", "Add height too, to see BMI.", weightDone, onAddWeight)
                ChecklistItem("Log a workout", "Starter plans are ready.", workoutDone, onAddWorkout)
            }
        }

        AreaCard("Blood pressure", Icons.Default.MonitorHeart, onOpenBp, onAddBp, "Add reading") {
            if (latestBp == null) {
                EmptyLine("No readings yet. Log one to see your category and trend.")
            } else {
                BloodPressureHero(latestBp, store.bloodPressureReadings)
                bloodPressureVisitSummary(
                    store.bloodPressureReadings,
                    store.profile.bpTargetSystolic,
                    store.profile.bpTargetDiastolic,
                )?.let { summary ->
                    Text(formatVisitSummaryLine(summary), color = colors.ink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
                val recent = recentBloodPressure(store.bloodPressureReadings, 10)
                if (recent.size > 1) {
                    BloodPressureChart(recent, store.bloodPressureReadings, store.profile.bpMovingAverageReadings)
                }
            }
        }

        AreaCard("Weight", Icons.Default.Scale, onOpenWeight, onAddWeight, "Record weight") {
            if (latestWeight == null) {
                EmptyLine("No weights yet. Record one to start your trend.")
            } else {
                WeightHero(store.weightRecords, useKg)
                val chart = homeWeightChartModel(store.weightRecords)
                if (chart != null && chart.points.size > 1) {
                    WeightChart(chart, store.profile.weightGoalLb, useKg)
                }
                val height = store.profile.heightInches
                if (height != null) {
                    val goalBmi = store.profile.weightGoalLb?.let { calculateBmi(it, height) } ?: Double.NaN
                    BmiGauge(calculateBmi(latestWeight.weight, height), goalBmi, compact = true)
                } else {
                    EmptyLine("Set your height to see BMI.")
                    SecondaryButton("Set height", onClick = onSetHeight)
                }
            }
        }

        AreaCard("Workouts", Icons.Default.FitnessCenter, onOpenWorkouts, onAddWorkout, "Log workout") {
            if (lastWorkout == null) {
                EmptyLine("No workouts yet. Pick a plan, then add sets or time.")
            } else {
                WorkoutHero(store)
                Text(formatWorkoutWeekLine(workoutWeekSummary(store.workouts, store.plans)), color = colors.ink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                val weeks = workoutSessionsByWeek(store.workouts, 8)
                if (weeks.any { it.count > 0 }) WorkoutBarChart(weeks)
            }
        }
    }
}

/** One area: title row with a tonal "+ Log" button, then the hub's hero and one compact insight. Tapping opens the tab. */
@Composable
private fun AreaCard(
    title: String,
    icon: ImageVector,
    onOpen: () -> Unit,
    onLog: () -> Unit,
    logDescription: String,
    content: @Composable () -> Unit,
) {
    val colors = LocalAppColors.current
    SurfaceCard(Modifier.clickable(onClickLabel = "Open $title", role = Role.Button, onClick = onOpen)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = colors.brand)
            Text(
                title,
                color = colors.ink,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            FilledTonalButton(
                onClick = onLog,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.brandSoft, contentColor = colors.selectedText),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp).semantics { contentDescription = logDescription },
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Log", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
            }
        }
        content()
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, color = LocalAppColors.current.inkMuted, fontSize = 14.sp, lineHeight = 20.sp)
}

@Composable
private fun ChecklistItem(title: String, description: String, done: Boolean, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .then(if (done) Modifier else Modifier.clickable(onClickLabel = title, role = Role.Button, onClick = onClick))
            .semantics(mergeDescendants = true) { stateDescription = if (done) "Done" else "Not done" }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (done) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) colors.brand else colors.inkFaint,
        )
        Column(Modifier.weight(1f)) {
            Text(title, color = if (done) colors.inkMuted else colors.ink, fontWeight = FontWeight.SemiBold)
            Text(if (done) "Done" else description, color = colors.inkMuted, fontSize = 13.sp)
        }
    }
}
