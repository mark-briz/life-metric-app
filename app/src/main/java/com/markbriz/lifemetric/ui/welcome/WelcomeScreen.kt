package com.markbriz.lifemetric.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.ui.components.GradientBackground
import com.markbriz.lifemetric.ui.components.PrimaryButton
import com.markbriz.lifemetric.ui.legal.DISCLAIMER
import com.markbriz.lifemetric.ui.theme.LocalAppColors

/**
 * Shown once, on first launch. There is no account or password; the phone's lock screen is the
 * only gate. The user must read and accept the health disclaimer before tracking anything.
 */
@Composable
fun WelcomeScreen(onAccept: () -> Unit) {
    val colors = LocalAppColors.current
    var accepted by remember { mutableStateOf(false) }
    var pageLaidOut by remember { mutableStateOf(false) }
    val pageScroll = rememberScrollState()
    val disclaimerRead = pageLaidOut &&
        (pageScroll.maxValue == 0 || pageScroll.value >= pageScroll.maxValue - 48)

    GradientBackground {
        Column(
            Modifier
                .verticalScroll(pageScroll)
                .onGloballyPositioned { pageLaidOut = true }
                .padding(horizontal = 20.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("LIFE METRIC", color = colors.brandDark, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            Text("Move with\nintention.", color = colors.ink, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 44.sp)
            Text(
                "Track workouts, vitals, and habits in one colorful command center. Everything stays on this phone, protected by your device lock.",
                color = colors.inkMuted,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surface)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Before you start", color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Please read the disclaimer. No account or password is needed.", color = colors.inkMuted)
                Text("Disclaimer", color = colors.ink, fontWeight = FontWeight.Bold)
                Text(
                    DISCLAIMER,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceMuted, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    color = colors.inkMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .toggleable(
                            value = accepted,
                            enabled = disclaimerRead,
                            role = Role.Checkbox,
                            onValueChange = { accepted = it },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = accepted,
                        onCheckedChange = null,
                        enabled = disclaimerRead,
                    )
                    Text("I have read and accept the Health and Fitness Disclaimer", color = colors.ink, fontSize = 14.sp)
                }
                if (!disclaimerRead) {
                    Text("Scroll to the bottom of the disclaimer to enable this checkbox.", color = colors.inkMuted, fontSize = 12.sp)
                }
                PrimaryButton(label = "Get started", enabled = accepted, onClick = onAccept)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
