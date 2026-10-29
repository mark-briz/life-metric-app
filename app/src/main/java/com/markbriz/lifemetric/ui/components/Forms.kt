package com.markbriz.lifemetric.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markbriz.lifemetric.ui.theme.LocalAppColors

/** Delete offered from an edit form's overflow menu, confirmed before [onConfirm] runs. */
data class FormDelete(
    val title: String,
    val body: String,
    val onConfirm: () -> Unit,
    val label: String = "Delete",
)

/**
 * Add and edit form frame shared by every record: ✕ and Save in the top bar, Cancel and Save
 * pinned at the bottom, a discard confirm on ✕, Cancel, or system Back when [isDirty], and
 * Delete in the overflow menu when [delete] is set.
 */
@Composable
fun RecordFormScaffold(
    title: String,
    isDirty: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
    lead: String? = null,
    saveEnabled: Boolean = true,
    actions: List<MenuAction> = emptyList(),
    delete: FormDelete? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val requestClose: () -> Unit = { if (isDirty) confirmDiscard = true else onClose() }
    BackHandler(onBack = requestClose)
    AppScreen(
        title = title,
        lead = lead,
        onBack = requestClose,
        navIcon = NavIcon.Close,
        actions = actions + listOfNotNull(delete?.let { MenuAction(it.label, destructive = true) { confirmDelete = true } }),
        barAction = {
            TextButton(onClick = onSave, enabled = saveEnabled) {
                Text("Save", color = colors.brandDark, fontWeight = FontWeight.Bold)
            }
        },
        bottomBar = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton("Cancel", requestClose, Modifier.weight(1f))
                PrimaryButton("Save", onClick = onSave, modifier = Modifier.weight(1f), enabled = saveEnabled)
            }
        },
        content = content,
    )
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Discard changes?",
            body = "Your edits will not be saved.",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            destructive = true,
            onConfirm = {
                confirmDiscard = false
                onClose()
            },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (confirmDelete && delete != null) {
        ConfirmDialog(
            title = delete.title,
            body = delete.body,
            confirmLabel = delete.label,
            destructive = true,
            onConfirm = {
                confirmDelete = false
                delete.onConfirm()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
