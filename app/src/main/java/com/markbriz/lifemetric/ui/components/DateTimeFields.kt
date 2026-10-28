package com.markbriz.lifemetric.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.markbriz.lifemetric.domain.formatDate
import com.markbriz.lifemetric.domain.formatTime
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun DateTimeFields(
    date: String,
    time: String,
    onDate: (String) -> Unit,
    onTime: (String) -> Unit,
    dateError: String? = null,
    timeError: String? = null,
    showTime: Boolean = true,
) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .semantics { contentDescription = if (date.isBlank()) "Date" else "Date, ${formatDate(date)}" },
            onClick = {
                val current = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() }
                DatePickerDialog(context, { _, year, month, day ->
                    onDate("%04d-%02d-%02d".format(year, month + 1, day))
                }, current.year, current.monthValue - 1, current.dayOfMonth).show()
            },
        ) { Text(if (date.isBlank()) "Date" else formatDate(date)) }
        if (showTime) {
            TimeField(time, onTime, Modifier.weight(1f))
        }
    }
    if (dateError != null || timeError != null) {
        Text(listOfNotNull(dateError, timeError).joinToString(" "))
    }
}

@Composable
fun TimeField(
    time: String,
    onTime: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Time",
) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics { contentDescription = if (time.isBlank()) label else "$label, ${formatTime(time)}" },
        onClick = {
            val current = runCatching { LocalTime.parse(time) }.getOrElse { LocalTime.now() }
            TimePickerDialog(context, { _, hour, minute ->
                onTime("%02d:%02d".format(hour, minute))
            }, current.hour, current.minute, false).show()
        },
    ) { Text(if (time.isBlank()) label else formatTime(time)) }
}
