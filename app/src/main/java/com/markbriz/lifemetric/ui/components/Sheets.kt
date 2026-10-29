package com.markbriz.lifemetric.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Modal bottom sheet for a small form. Swipe-down, scrim tap, system Back, and [requestClose]
 * ask before discarding when [isDirty] is true; [close] dismisses without asking (after Save).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormSheet(
    onDismiss: () -> Unit,
    isDirty: () -> Boolean = { false },
    discardBody: String = "Your edits will not be saved.",
    returnFocus: FocusRequester? = null,
    content: @Composable ColumnScope.(close: () -> Unit, requestClose: () -> Unit) -> Unit,
) {
    val colors = LocalAppColors.current
    val scope = rememberCoroutineScope()
    val dirtyNow by rememberUpdatedState(isDirty)
    var confirmDiscard by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    val sheetFocus = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target ->
            if (target == SheetValue.Hidden && !closing && dirtyNow()) {
                confirmDiscard = true
                false
            } else {
                true
            }
        },
    )
    val restoreFocus: () -> Unit = { returnFocus?.let { runCatching { it.requestFocus() } } }
    val close: () -> Unit = {
        closing = true
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            restoreFocus()
        }
    }
    LaunchedEffect(sheetState.currentValue) {
        if (sheetState.currentValue == SheetValue.Expanded) {
            delay(80)
            runCatching { sheetFocus.requestFocus() }
        }
    }
    val requestClose: () -> Unit = { if (dirtyNow()) confirmDiscard = true else close() }
    ModalBottomSheet(
        onDismissRequest = {
            if (!closing && dirtyNow()) {
                confirmDiscard = true
                scope.launch { sheetState.show() }
            } else {
                onDismiss()
                restoreFocus()
            }
        },
        sheetState = sheetState,
        containerColor = colors.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .focusRequester(sheetFocus)
                .focusable()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content(close, requestClose)
        }
    }
    if (confirmDiscard) {
        ConfirmDeleteDialog(
            title = "Discard changes?",
            body = discardBody,
            confirmLabel = "Discard",
            onConfirm = {
                confirmDiscard = false
                close()
            },
            onDismiss = { confirmDiscard = false },
            dismissLabel = "Keep editing",
        )
    }
}

@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    val colors = LocalAppColors.current
    Column {
        Text(title, color = colors.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        if (subtitle != null) Text(subtitle, color = colors.inkMuted, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** Collapsible group for optional fields. */
@Composable
fun AddDetails(
    expanded: Boolean,
    onToggle: () -> Unit,
    label: String = "Add details",
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onToggle)
            .semantics {
                role = Role.Button
                contentDescription = label
                stateDescription = if (expanded) "Expanded" else "Collapsed"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = colors.brandDark)
        Text(label, color = colors.brandDark, fontWeight = FontWeight.SemiBold)
    }
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
