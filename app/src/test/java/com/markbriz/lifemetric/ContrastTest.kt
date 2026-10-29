package com.markbriz.lifemetric

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.markbriz.lifemetric.domain.BmiCategory
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.ui.theme.AppThemes
import com.markbriz.lifemetric.ui.theme.STATUS_TINT_ALPHA
import com.markbriz.lifemetric.ui.theme.contrastRatio
import com.markbriz.lifemetric.ui.theme.readableOn
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG 2.2 AA (1.4.3): text pairs the UI draws must reach 4.5:1 in every theme. */
class ContrastTest {
    @Test
    fun textPairsMeetAaInEveryTheme() {
        val failures = AppThemes.flatMap { t ->
            val pairs = buildList {
                add("ink on surface" to contrastRatio(t.ink, t.surface))
                add("inkMuted on surface" to contrastRatio(t.inkMuted, t.surface))
                add("inkFaint on surface" to contrastRatio(t.inkFaint, t.surface))
                add("inkMuted on surfaceMuted" to contrastRatio(t.inkMuted, t.surfaceMuted))
                add("link on surface" to contrastRatio(t.brandDark, t.surface))
                add("selected chip text" to contrastRatio(t.selectedText, t.brandSoft))
                add("accent text" to contrastRatio(t.accentText, t.surface))
                add("success text" to contrastRatio(t.successText, t.surface))
                add("danger text" to contrastRatio(t.danger, t.surface))
                add("button text" to contrastRatio(t.onBrandFill, t.brandFill))
                add("snackbar text" to contrastRatio(t.surface, t.ink))
                add("snackbar action" to contrastRatio(t.snackbarAction, t.ink))
                listOf(t.gradientTop, t.gradientMid, t.gradientBottom).forEachIndexed { i, g ->
                    add("ink on gradient $i" to contrastRatio(t.ink, g))
                    add("inkMuted on gradient $i" to contrastRatio(t.inkMuted, g))
                }
                BpStatus.entries.forEach { s ->
                    val hue = Color(s.color)
                    val pill = hue.copy(alpha = STATUS_TINT_ALPHA).compositeOver(t.surface)
                    add("badge ${s.name}" to contrastRatio(t.statusText(hue), pill))
                }
                BmiCategory.entries.forEach { c ->
                    add("BMI ${c.name}" to contrastRatio(t.hueText(Color(c.color)), t.surface))
                }
            }
            pairs.filter { it.second < 4.5 }.map { "${t.id}: ${it.first} = ${"%.2f".format(it.second)}" }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun readableOnKeepsPassingColorsAndFixesFailingOnes() {
        val teal = Color(0xFF0F766E)
        assertEquals(teal, teal.readableOn(Color.White))
        val orange = Color(0xFFF97316)
        assertTrue(contrastRatio(orange, Color.White) < 4.5)
        assertTrue(contrastRatio(orange.readableOn(Color.White), Color.White) >= 4.5)
        val red = Color(0xFF991B1B)
        assertTrue(contrastRatio(red.readableOn(Color(0xFF1E293B)), Color(0xFF1E293B)) >= 4.5)
    }
}
