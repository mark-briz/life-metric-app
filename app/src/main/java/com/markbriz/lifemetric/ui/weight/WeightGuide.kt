package com.markbriz.lifemetric.ui.weight

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.theme.LocalAppColors

private val WEIGH_STEPS = listOf(
    "Weigh first thing in the morning, after using the restroom and before breakfast.",
    "Wear the same clothes, or none. Use the same scale on a hard, flat floor.",
    "Stand still and record the number you see, to the nearest 0.1 lb.",
    "Weigh most days if you can. Skipping a day is fine — the trend fills gaps.",
    "Trust the 7-day trend, not one high or low morning. Food, water, and salt move the scale by a few pounds.",
)

@Composable
fun WeightHowToScreen(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    AppScreen(
        eyebrow = "Weight",
        title = "How to weigh",
        lead = "Same conditions each day make the trend meaningful. This is not a diagnosis.",
        onBack = onBack,
        backLabel = "Weight",
    ) {
        SurfaceCard {
            WEIGH_STEPS.forEachIndexed { index, step ->
                Text(
                    "${index + 1}. $step",
                    color = colors.ink,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
            }
        }
        SurfaceCard {
            Text("Why the trend matters", color = colors.ink, fontWeight = FontWeight.Bold)
            Text(
                "A single reading is mostly water and whatever is in your gut. The 7-day trend is the number to compare week to week.",
                color = colors.inkMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
    }
}
