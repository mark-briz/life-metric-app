package com.markbriz.lifemetric.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.ui.theme.LocalAppColors

/**
 * Hero label and big value. Each area's hero (hub and Home card) starts with this, followed by
 * one status line and when it was recorded. The label never changes meaning.
 */
@Composable
fun HeroHeader(label: String, value: String, valueSize: TextUnit = 32.sp) {
    val colors = LocalAppColors.current
    Text(
        label.uppercase(),
        color = colors.inkMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.semantics { heading() },
    )
    Text(value, color = colors.ink, fontSize = valueSize, fontWeight = FontWeight.ExtraBold, lineHeight = valueSize * 1.15f)
}

/** A hub hero: [HeroHeader] plus status lines from [content], in a card. */
@Composable
fun HeroCard(
    label: String,
    value: String,
    valueSize: TextUnit = 32.sp,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    SurfaceCard {
        HeroHeader(label, value, valueSize)
        content()
    }
}

/** One option in a [SegmentedChoice]; [description] is what TalkBack reads, including the unit. */
data class Segment<T>(val value: T, val label: String, val description: String = label)

/** Single-choice segmented control, optionally preceded by a unit label such as "Readings". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedChoice(
    segments: List<Segment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    unitLabel: String? = null,
) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (unitLabel != null) {
            Text(unitLabel, color = colors.inkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            segments.forEachIndexed { index, segment ->
                SegmentedButton(
                    selected = segment.value == selected,
                    onClick = { onSelect(segment.value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = segments.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.brandSoft,
                        activeContentColor = colors.selectedText,
                        activeBorderColor = colors.brand,
                        inactiveContainerColor = colors.surface,
                        inactiveContentColor = colors.ink,
                        inactiveBorderColor = colors.border,
                    ),
                    modifier = Modifier.semantics { contentDescription = segment.description },
                ) {
                    Text(segment.label, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
                }
            }
        }
    }
}

/** An applied history filter, shown as a removable chip above the list. */
data class ActiveFilter(val label: String, val onRemove: () -> Unit)

/** Filter button plus the active filters as dismissible chips, with Clear all. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterBar(
    active: List<ActiveFilter>,
    onOpen: () -> Unit,
    onClearAll: () -> Unit,
    openerFocus: FocusRequester? = null,
) {
    val colors = LocalAppColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        AssistChip(
            onClick = onOpen,
            label = { Text(if (active.isEmpty()) "Filter" else "Filter · ${active.size}", fontWeight = FontWeight.SemiBold) },
            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = colors.surface,
                labelColor = colors.ink,
                leadingIconContentColor = colors.brandDark,
                trailingIconContentColor = colors.inkMuted,
            ),
            border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = colors.border),
            modifier = Modifier
                .then(if (openerFocus != null) Modifier.focusRequester(openerFocus) else Modifier)
                .semantics {
                    contentDescription = if (active.isEmpty()) "Filter history" else "Filter history, ${active.size} active"
                },
        )
        active.forEach { filter ->
            InputChip(
                selected = true,
                onClick = filter.onRemove,
                label = { Text(filter.label) },
                trailingIcon = {
                    Icon(Icons.Default.Close, contentDescription = "Remove filter ${filter.label}", Modifier.size(InputChipDefaults.IconSize))
                },
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = colors.brandSoft,
                    selectedLabelColor = colors.selectedText,
                    selectedTrailingIconColor = colors.selectedText,
                ),
            )
        }
        if (active.size > 1) {
            TextLink("Clear all", onClick = onClearAll, contentDescription = "Clear all filters")
        }
    }
}

/** Bottom sheet that holds a hub's history filters; changes apply as they are chosen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    resultSummary: String,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    returnFocus: FocusRequester? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    val sheetFocus = remember { FocusRequester() }
    val dismiss: () -> Unit = {
        onDismiss()
        returnFocus?.let { runCatching { it.requestFocus() } }
    }
    LaunchedEffect(Unit) {
        delay(80)
        runCatching { sheetFocus.requestFocus() }
    }
    ModalBottomSheet(
        onDismissRequest = dismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .focusRequester(sheetFocus)
                .focusable()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SheetTitle("Filter history", resultSummary)
            content()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Clear all", onClearAll, Modifier.weight(1f), contentDescription = "Clear all filters")
                PrimaryButton("Done", onClick = dismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}

/** A titled group of filter chips inside a [FilterSheet]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FilterSection(title: String, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = colors.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}
