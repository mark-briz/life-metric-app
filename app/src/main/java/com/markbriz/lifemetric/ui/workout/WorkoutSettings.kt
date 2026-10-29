package com.markbriz.lifemetric.ui.workout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.FitnessStore
import com.markbriz.lifemetric.domain.formatRestTimerSetting
import com.markbriz.lifemetric.domain.formatWorkoutReminder
import com.markbriz.lifemetric.domain.weightUnit
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.ChoiceChip
import com.markbriz.lifemetric.ui.components.SettingsRow
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.theme.LocalAppColors

private val REST_TIMER_OPTIONS = listOf(0, 60, 90, 120)

@Composable
fun WorkoutSettingsScreen(
    store: FitnessStore,
    onBack: () -> Unit,
    backLabel: String = "Workouts",
    onPlans: () -> Unit,
    onRestTimer: () -> Unit,
    onReminders: () -> Unit,
    onUnits: () -> Unit,
) {
    val profile = store.profile
    AppScreen(
        title = "Workout settings",
        lead = "Plans, rest timer, reminders, and units for workouts.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SettingsRow("Plans", "Create and edit fitness plans and their activities.", onPlans, "${store.plans.size}")
        SettingsRow(
            "Rest timer",
            "Countdown after each working set or block.",
            onRestTimer,
            formatRestTimerSetting(profile.workoutRestTimerSec),
        )
        SettingsRow(
            "Workout reminders",
            "Optional local morning reminder to log today's session.",
            onReminders,
            formatWorkoutReminder(profile.workoutMorningReminder, profile.workoutMorningReminderTime),
        )
        SettingsRow(
            "Units",
            "How lift and body weight are shown. Stored values stay in pounds.",
            onUnits,
            weightUnit(profile.weightDisplayKg),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RestTimerEditorScreen(
    restSec: Int,
    onSave: (Int) -> Unit,
    onBack: () -> Unit,
    backLabel: String = "Workout settings",
    onSaved: () -> Unit = {},
) {
    AppScreen(
        title = "Rest timer",
        lead = "Starts after you save a working set or block. It is a countdown, not coaching.",
        onBack = onBack,
        backLabel = backLabel,
    ) {
        SurfaceCard {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                REST_TIMER_OPTIONS.forEach { sec ->
                    ChoiceChip(formatRestTimerSetting(sec), selected = restSec == sec, onClick = {
                        onSave(sec)
                        onSaved()
                    })
                }
            }
            Text(
                "Warm-up sets do not start the timer. Skip rest any time from the workout screen.",
                color = LocalAppColors.current.inkMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}
