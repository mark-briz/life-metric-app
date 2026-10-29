package com.markbriz.lifemetric.ui.bp

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.ui.components.AppScreen
import com.markbriz.lifemetric.ui.components.SurfaceCard
import com.markbriz.lifemetric.ui.components.TextLink
import com.markbriz.lifemetric.ui.theme.LocalAppColors

private const val VALIDATE_BP_URL = "https://www.validatebp.org"

private val MEASURE_STEPS = listOf(
    "Avoid smoking, caffeine, or exercise for 30 minutes. Empty your bladder.",
    "Rest quietly for at least 5 minutes. Don't talk or use the phone.",
    "Sit with your back supported and feet flat on the floor, legs uncrossed.",
    "Rest your arm on a table so the middle of the cuff is at heart level.",
    "Place the cuff on bare skin, just above the bend of the elbow. Don't measure over clothes.",
    "Take two readings, one minute apart, at the same times each day — morning before medication and evening.",
    "Record both numbers, and pulse if your cuff shows it.",
)

@Composable
fun BloodPressureMeasureScreen(onBack: () -> Unit) {
    val colors = LocalAppColors.current
    val uriHandler = LocalUriHandler.current
    AppScreen(
        title = "How to measure",
        lead = "American Heart Association steps for a home reading. This is not a diagnosis.",
        onBack = onBack,
        backLabel = "Blood pressure",
    ) {
        SurfaceCard {
            MEASURE_STEPS.forEachIndexed { index, step ->
                Text(
                    "${index + 1}. $step",
                    color = colors.ink,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
            }
        }
        SurfaceCard {
            Text("Use a validated cuff", color = colors.ink, fontWeight = FontWeight.Bold)
            Text(
                "Use an automatic, upper-arm cuff that has been validated. Wrist and finger monitors are less reliable.",
                color = colors.inkMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            TextLink(
                "Find validated devices at validatebp.org",
                onClick = { runCatching { uriHandler.openUri(VALIDATE_BP_URL) } },
                contentDescription = "Open validatebp.org",
            )
        }
    }
}
