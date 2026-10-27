package com.markbriz.lifemetric.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun appTextFieldColors() = with(LocalAppColors.current) {
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = ink,
        unfocusedTextColor = ink,
        disabledTextColor = inkMuted,
        errorTextColor = ink,
        focusedContainerColor = surface,
        unfocusedContainerColor = surfaceMuted,
        disabledContainerColor = surfaceMuted,
        errorContainerColor = surfaceMuted,
        focusedLabelColor = brand,
        unfocusedLabelColor = inkMuted,
        disabledLabelColor = inkFaint,
        errorLabelColor = danger,
        focusedBorderColor = brand,
        unfocusedBorderColor = border,
        disabledBorderColor = border,
        errorBorderColor = danger,
        cursorColor = brand,
        errorCursorColor = danger,
        focusedPlaceholderColor = inkFaint,
        unfocusedPlaceholderColor = inkFaint,
        disabledPlaceholderColor = inkFaint,
        focusedTrailingIconColor = inkMuted,
        unfocusedTrailingIconColor = inkMuted,
        disabledTrailingIconColor = inkFaint,
        focusedSupportingTextColor = inkMuted,
        unfocusedSupportingTextColor = inkMuted,
        disabledSupportingTextColor = inkFaint,
        errorSupportingTextColor = danger,
    )
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val colors = LocalAppColors.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val scope = rememberCoroutineScope()
    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoView)
                .onFocusChanged { focus ->
                    if (focus.isFocused) {
                        scope.launch {
                            delay(280)
                            bringIntoView.bringIntoView()
                        }
                    }
                },
            label = { Text(label) },
            isError = error != null,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = fieldShape,
            colors = appTextFieldColors(),
        )
        val helper = error ?: hint
        if (helper != null) {
            Text(helper, color = if (error != null) colors.danger else colors.inkMuted, fontSize = 12.sp)
        }
    }
}
