package com.markbriz.lifemetric.ui.workout

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.theme.LocalAppColors

private val LOG_STEPS = listOf(
    "Pick a plan, then log every working set, segment, or block.",
    "Use last time as a starting point, not a prescription.",
    "Rest 2–3 minutes on hard lifts. Stop if pain is sharp.",
    "Talk to a clinician about a program. This app is a log, not coaching.",
)

@Composable
fun WorkoutHowToScreen(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    AppScreen(
        title = "How to log",
        lead = "A complete session is more useful than a perfect one. This is not a training prescription.",
        onBack = onBack,
        backLabel = "Workouts",
    ) {
        SurfaceCard {
            LOG_STEPS.forEachIndexed { index, step ->
                Text(
                    "${index + 1}. $step",
                    color = colors.ink,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
            }
        }
        SurfaceCard {
            Text("Why last time matters", color = colors.ink, fontWeight = FontWeight.Bold)
            Text(
                "Progressive overload is repeating a lift and adding a little when it feels ready. Same load two sessions in a row is still useful data.",
                color = colors.inkMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
