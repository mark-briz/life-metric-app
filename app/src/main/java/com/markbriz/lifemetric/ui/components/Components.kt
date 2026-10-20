package com.markbriz.lifemetric.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markbriz.lifemetric.data.BloodPressureReading
import com.markbriz.lifemetric.data.WeightRecord
import com.markbriz.lifemetric.domain.BpStatus
import com.markbriz.lifemetric.domain.BmiCategory
import com.markbriz.lifemetric.domain.WeekBucket
import com.markbriz.lifemetric.domain.bloodPressureStatus
import com.markbriz.lifemetric.domain.bmiCategory
import com.markbriz.lifemetric.domain.chronologicalBp
import com.markbriz.lifemetric.domain.formatBmi
import com.markbriz.lifemetric.domain.shortDateLabel
import com.markbriz.lifemetric.domain.trailingMovingAverage
import com.markbriz.lifemetric.ui.theme.LocalAppColors
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val CardShape = RoundedCornerShape(24.dp)
private val FieldShape = RoundedCornerShape(16.dp)

@Composable
fun GradientBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.gradientTop, colors.gradientMid, colors.gradientBottom))),
    ) {
        content()
    }
}

@Composable
fun AppScreen(
    title: String,
    eyebrow: String? = null,
    lead: String? = null,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back",
    showFab: Boolean = false,
    fabLabel: String = "Add",
    onFab: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (onBack != null) {
                Row(
                    Modifier.clickable(onClick = onBack),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.brandDark)
                    Spacer(Modifier.width(6.dp))
                    Text(backLabel, color = colors.brandDark, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.padding(end = if (showFab) 64.dp else 0.dp)) {
                if (eyebrow != null) {
                    Text(
                        eyebrow.uppercase(),
                        color = colors.brandDark,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.1.sp,
                    )
                }
                Text(title, color = colors.ink, fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp)
                if (lead != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(lead, color = colors.inkMuted, fontSize = 16.sp, lineHeight = 22.sp)
                }
            }
            content()
        }
        if (showFab && onFab != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 28.dp, end = 16.dp)
                    .size(52.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(colors.brand)
                    .clickable(onClick = onFab),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Add, contentDescription = fabLabel, tint = Color.White)
            }
        }
    }
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier
            .fillMaxWidth()
            .shadow(2.dp, CardShape)
            .clip(CardShape)
            .background(colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
fun PrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false, enabled: Boolean = true) {
    val colors = LocalAppColors.current
    val active = enabled && !loading
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (active) colors.brand else colors.brand.copy(alpha = 0.45f))
            .clickable(enabled = active, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun GhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Box(
        modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.brandDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun DangerOutlineButton(label: String, onClick: () -> Unit, enabled: Boolean = true) {
    val colors = LocalAppColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, colors.danger, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.danger, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun EmptyState(title: String, description: String, action: (@Composable () -> Unit)? = null) {
    val colors = LocalAppColors.current
    SurfaceCard {
        Text(title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(description, color = colors.inkMuted, fontSize = 14.sp, lineHeight = 20.sp)
        if (action != null) {
            Spacer(Modifier.height(4.dp))
            action()
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = LocalAppColors.current.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
}

@Composable
fun SettingsRow(title: String, description: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, color = colors.inkMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.inkFaint)
    }
}

@Composable
fun BpBadge(status: BpStatus, compact: Boolean = false) {
    val color = Color(status.color)
    Text(
        status.label,
        color = color,
        fontSize = if (compact) 11.sp else 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(
                horizontal = if (compact) 8.dp else 10.dp,
                vertical = if (compact) 3.dp else 4.dp,
            ),
    )
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    body: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Single-button alert, equivalent to a plain `Alert.alert(title, body)` in the Expo app. */
@Composable
fun MessageDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
fun StatChip(label: String, value: String, hint: String?, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface.copy(alpha = 0.86f))
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        Text(label, color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 2)
        Text(
            text = value,
            color = colors.ink,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, maxFontSize = 18.sp),
        )
        if (hint != null) Text(hint, color = colors.inkFaint, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
fun BpGauge(systolic: Int, diastolic: Int, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val zones = listOf(
        80f to 120f to Color(0xFF22C55E),
        120f to 130f to Color(0xFFEAB308),
        130f to 140f to Color(0xFFF97316),
        140f to 200f to Color(0xFFEF4444),
    )
    Canvas(modifier.fillMaxWidth().height(180.dp)) {
        val stroke = 26.dp.toPx()
        val radius = min(size.width, size.height * 1.6f) * 0.38f
        val cx = size.width / 2f
        val cy = size.height * 0.86f
        val sweep = 180f / zones.size
        zones.forEachIndexed { index, zone ->
            drawArc(
                color = zone.second,
                startAngle = 180f + index * sweep,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Butt),
            )
        }
        val clamped = systolic.toFloat().coerceIn(80f, 200f)
        val zoneIndex = zones.indexOfFirst { clamped <= it.first.second }.let { if (it < 0) zones.lastIndex else it }
        val zone = zones[zoneIndex].first
        val span = (zone.second - zone.first).coerceAtLeast(1f)
        val t = ((clamped - zone.first) / span).coerceIn(0f, 1f)
        val angle = Math.toRadians((180.0 + zoneIndex * sweep + t * sweep))
        val needle = radius - stroke
        drawLine(
            color = colors.ink,
            start = Offset(cx, cy),
            end = Offset(cx + (cos(angle) * needle).toFloat(), cy + (sin(angle) * needle).toFloat()),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(colors.ink, 6.dp.toPx(), Offset(cx, cy))
    }
    Text(
        "$systolic / $diastolic",
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = colors.ink,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    )
}

@Composable
fun BmiGauge(bmi: Double, goalBmi: Double, compact: Boolean = false) {
    val colors = LocalAppColors.current
    val segments = listOf(
        BmiCategory.Under to (12f to 18.5f),
        BmiCategory.Healthy to (18.5f to 25f),
        BmiCategory.Over to (25f to 30f),
        BmiCategory.Obese to (30f to 35f),
    )
    val category = bmiCategory(bmi)
    Canvas(Modifier.fillMaxWidth().height(if (compact) 170.dp else 210.dp)) {
        val stroke = 26.dp.toPx()
        val radius = min(size.width * 0.34f, size.height * 0.42f)
        val cx = size.width / 2f
        val cy = size.height * 0.58f
        val start = 135f
        val total = 270f
        val span = 35f - 12f
        segments.forEach { (cat, range) ->
            val a0 = start + ((range.first - 12f) / span) * total
            val a1 = start + ((range.second - 12f) / span) * total
            drawArc(
                color = Color(cat.color),
                startAngle = a0,
                sweepAngle = a1 - a0,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(stroke, cap = StrokeCap.Butt),
            )
        }
        fun needle(value: Double, color: Color) {
            if (value.isNaN()) return
            val t = ((value.toFloat().coerceIn(12f, 35f) - 12f) / span)
            val angle = Math.toRadians((start + t * total).toDouble())
            val tip = radius - stroke / 2
            drawLine(
                color,
                Offset(cx, cy),
                Offset(cx + (cos(angle) * tip).toFloat(), cy + (sin(angle) * tip).toFloat()),
                4.dp.toPx(),
                StrokeCap.Round,
            )
        }
        needle(goalBmi, colors.accent)
        needle(bmi, colors.ink)
        drawCircle(colors.surface, 8.dp.toPx(), Offset(cx, cy))
        drawCircle(colors.ink, 5.dp.toPx(), Offset(cx, cy))
    }
    Text(
        formatBmi(bmi),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = colors.ink,
        fontWeight = FontWeight.ExtraBold,
        fontSize = if (compact) 28.sp else 36.sp,
    )
    if (category != null) {
        Text(
            "${category.label} · ${category.range}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color(category.color),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun BloodPressureChart(readings: List<BloodPressureReading>, all: List<BloodPressureReading>, window: Int) {
    val colors = LocalAppColors.current
    val chronological = chronologicalBp(readings)
    if (chronological.size < 2) return
    val full = chronologicalBp(all.ifEmpty { readings })
    val sysMa = trailingMovingAverage(full.map { it.systolic.toDouble() }, window)
    val diaMa = trailingMovingAverage(full.map { it.diastolic.toDouble() }, window)
    val ma = full.mapIndexed { index, reading -> reading.id to (sysMa[index] to diaMa[index]) }.toMap()
    val xLabelIndexes = if (chronological.size <= 4) {
        chronological.indices.toList()
    } else {
        listOf(0, chronological.lastIndex / 2, chronological.lastIndex).distinct()
    }
    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val padL = 44.dp.toPx()
        val padR = 12.dp.toPx()
        val padT = 16.dp.toPx()
        val padB = 32.dp.toPx()
        val values = chronological.flatMap {
            val avg = ma[it.id]
            listOf(it.systolic.toFloat(), it.diastolic.toFloat(), avg?.first?.toFloat() ?: it.systolic.toFloat(), avg?.second?.toFloat() ?: it.diastolic.toFloat())
        }
        val minV = values.min()
        val maxV = values.max()
        val span = (maxV - minV).coerceAtLeast(20f)
        val yMin = minV - span * 0.1f
        val yMax = maxV + span * 0.1f
        fun x(i: Int) = padL + (i.toFloat() / (chronological.lastIndex).coerceAtLeast(1)) * (size.width - padL - padR)
        fun y(v: Float) = padT + ((yMax - v) / (yMax - yMin)) * (size.height - padT - padB)
        fun pathOf(selector: (BloodPressureReading) -> Float): Path {
            val path = Path()
            chronological.forEachIndexed { index, reading ->
                val px = x(index)
                val py = y(selector(reading))
                if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            return path
        }
        val yTicks = listOf(yMin, (yMin + yMax) / 2f, yMax)
        yTicks.forEach { tick ->
            val tickY = y(tick)
            drawLine(colors.border, Offset(padL, tickY), Offset(size.width - padR, tickY), 1.dp.toPx())
        }
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = colors.inkMuted.toArgb()
                textSize = 11.sp.toPx()
            }
            val metrics = paint.fontMetrics
            paint.textAlign = android.graphics.Paint.Align.RIGHT
            yTicks.forEach { tick ->
                val tickY = y(tick) - (metrics.ascent + metrics.descent) / 2f
                canvas.nativeCanvas.drawText(tick.roundToInt().toString(), padL - 8.dp.toPx(), tickY, paint)
            }
            val xBaseline = size.height - 6.dp.toPx()
            xLabelIndexes.forEach { index ->
                paint.textAlign = when {
                    index == 0 -> android.graphics.Paint.Align.LEFT
                    index == chronological.lastIndex -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(shortDateLabel(chronological[index].date), x(index), xBaseline, paint)
            }
        }
        drawPath(pathOf { ma[it.id]?.first?.toFloat() ?: it.systolic.toFloat() }, colors.brand.copy(alpha = 0.45f), style = Stroke(3f, cap = StrokeCap.Round))
        drawPath(pathOf { ma[it.id]?.second?.toFloat() ?: it.diastolic.toFloat() }, colors.accent.copy(alpha = 0.45f), style = Stroke(3f, cap = StrokeCap.Round))
        drawPath(pathOf { it.systolic.toFloat() }, colors.brand, style = Stroke(4f, cap = StrokeCap.Round))
        drawPath(pathOf { it.diastolic.toFloat() }, colors.accent, style = Stroke(4f, cap = StrokeCap.Round))
        chronological.forEachIndexed { index, reading ->
            drawCircle(colors.brand, 4.dp.toPx(), Offset(x(index), y(reading.systolic.toFloat())))
            drawCircle(colors.accent, 4.dp.toPx(), Offset(x(index), y(reading.diastolic.toFloat())))
        }
    }
    Text("Systolic · diastolic · trailing average", color = colors.inkMuted, fontSize = 12.sp)
}

@Composable
fun WeightChart(records: List<WeightRecord>, goal: Double?) {
    val colors = LocalAppColors.current
    if (records.isEmpty()) return
    val xLabelIndexes = if (records.size <= 4) {
        records.indices.toList()
    } else {
        listOf(0, records.lastIndex / 2, records.lastIndex).distinct()
    }
    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val padL = 48.dp.toPx()
        val padR = 12.dp.toPx()
        val padT = 16.dp.toPx()
        val padB = 32.dp.toPx()
        val values = records.map { it.weight.toFloat() } + listOfNotNull(goal?.toFloat())
        val minV = values.min()
        val maxV = values.max()
        val span = (maxV - minV).coerceAtLeast(1f)
        var yMin = floor(minV - span * 0.15f)
        var yMax = ceil(maxV + span * 0.15f)
        if (yMax <= yMin) yMax = yMin + 2f
        if (((yMax - yMin).toInt() % 2) != 0) yMax += 1f
        fun x(i: Int) = if (records.size == 1) (padL + size.width - padR) / 2 else padL + i.toFloat() / records.lastIndex * (size.width - padL - padR)
        fun y(v: Float) = padT + ((yMax - v) / (yMax - yMin)) * (size.height - padT - padB)
        val yTicks = listOf(yMin, (yMin + yMax) / 2f, yMax)
        yTicks.forEach { tick ->
            val tickY = y(tick)
            drawLine(colors.border, Offset(padL, tickY), Offset(size.width - padR, tickY), 1.dp.toPx())
        }
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = colors.inkMuted.toArgb()
                textAlign = android.graphics.Paint.Align.RIGHT
                textSize = 11.sp.toPx()
            }
            val metrics = paint.fontMetrics
            yTicks.forEach { tick ->
                val tickY = y(tick) - (metrics.ascent + metrics.descent) / 2f
                canvas.nativeCanvas.drawText(tick.roundToInt().toString(), padL - 8.dp.toPx(), tickY, paint)
            }
            val xBaseline = size.height - 6.dp.toPx()
            xLabelIndexes.forEach { index ->
                paint.textAlign = when {
                    records.size == 1 -> android.graphics.Paint.Align.CENTER
                    index == 0 -> android.graphics.Paint.Align.LEFT
                    index == records.lastIndex -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                canvas.nativeCanvas.drawText(shortDateLabel(records[index].date), x(index), xBaseline, paint)
            }
        }
        if (goal != null) {
            drawLine(
                color = colors.success,
                start = Offset(padL, y(goal.toFloat())),
                end = Offset(size.width - padR, y(goal.toFloat())),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Butt,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 8.dp.toPx())),
            )
        }
        val path = Path()
        records.forEachIndexed { index, record ->
            val px = x(index)
            val py = y(record.weight.toFloat())
            if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, colors.brand, style = Stroke(4f, cap = StrokeCap.Round))
        records.forEachIndexed { index, record ->
            drawCircle(colors.brand, 4.5.dp.toPx(), Offset(x(index), y(record.weight.toFloat())))
        }
    }
}

@Composable
fun WorkoutBarChart(data: List<WeekBucket>) {
    val colors = LocalAppColors.current
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val maxCount = data.maxOf { it.count }.coerceAtLeast(1)
        val gap = 8.dp.toPx()
        val barWidth = ((size.width - gap * (data.size - 1)) / data.size).coerceAtLeast(4f)
        val innerHeight = size.height - 8.dp.toPx()
        data.forEachIndexed { index, bucket ->
            val barHeight = (bucket.count.toFloat() / maxCount) * innerHeight
            val x = index * (barWidth + gap)
            drawRoundRect(
                color = if (bucket.count == 0) colors.border else colors.brand,
                topLeft = Offset(x, size.height - barHeight),
                size = Size(barWidth, barHeight.coerceAtLeast(if (bucket.count == 0) 2f else 6f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        data.forEach { bucket ->
            Text(bucket.label, color = colors.inkFaint, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun statusOf(reading: BloodPressureReading): BpStatus = bloodPressureStatus(reading.systolic, reading.diastolic)

val fieldShape = FieldShape
