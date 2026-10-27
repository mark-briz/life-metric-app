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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = if (showFab) 88.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (onBack != null) {
                Row(
                    Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .clickable(onClick = onBack)
                        .semantics { contentDescription = "Back to $backLabel" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.brandDark)
                    Spacer(Modifier.width(6.dp))
                    Text(backLabel, color = colors.brandDark, fontWeight = FontWeight.SemiBold)
                }
            }
            Column {
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
            FloatingActionButton(
                onClick = onFab,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
                containerColor = colors.brand,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = fabLabel)
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
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = LocalAppColors.current
    Box(
        modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = colors.brandDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
fun TextLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = label,
) {
    val colors = LocalAppColors.current
    Text(
        label,
        color = colors.brandDark,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            },
    )
}

class SaveAck {
    var visible by mutableStateOf(false)
    var token by mutableLongStateOf(0L)

    fun mark(ok: Boolean) {
        if (ok) {
            visible = true
            token = System.nanoTime()
        } else {
            visible = false
        }
    }
}

@Composable
fun rememberSaveAck(): SaveAck {
    val ack = remember { SaveAck() }
    LaunchedEffect(ack.token) {
        if (ack.token == 0L) return@LaunchedEffect
        delay(1600)
        ack.visible = false
    }
    return ack
}

@Composable
fun SavedHint(visible: Boolean) {
    if (!visible) return
    Text(
        "Saved",
        color = LocalAppColors.current.success,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
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
fun EmptyState(title: String, description: String, actionLabel: String, onAction: () -> Unit) {
    EmptyState(title, description) { PrimaryButton(actionLabel, onClick = onAction) }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = LocalAppColors.current.ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
}

@Composable
fun SettingsRow(title: String, description: String, onClick: () -> Unit, value: String? = null) {
    val colors = LocalAppColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .shadow(1.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
            .semantics { role = Role.Button },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(description, color = colors.inkMuted, fontSize = 13.sp, lineHeight = 18.sp)
        }
        if (value != null) {
            Text(
                value,
                color = colors.inkMuted,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.inkFaint)
    }
}

@Composable
fun HistoryRow(
    primary: String,
    secondary: String,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    overflowLabel: String? = null,
    onOverflow: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    var menuOpen by remember { mutableStateOf(false) }
    SurfaceCard(Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(primary, color = colors.ink, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(secondary, color = colors.inkMuted, fontSize = 13.sp)
            }
            if (trailing != null) {
                Box(Modifier.padding(end = 4.dp)) { trailing() }
            }
            if (overflowLabel != null && onOverflow != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More actions for $primary", tint = colors.inkMuted)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(overflowLabel) },
                            onClick = {
                                menuOpen = false
                                onOverflow()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BpBadge(status: BpStatus, compact: Boolean = false) {
    var showGuide by remember { mutableStateOf(false) }
    val color = Color(status.color)
    Box(
        Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clickable { showGuide = true }
            .semantics {
                role = Role.Button
                contentDescription = "${status.label}. Show blood pressure category descriptions."
            },
        contentAlignment = Alignment.Center,
    ) {
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
    if (showGuide) {
        BloodPressureCategoriesDialog(selected = status, onDismiss = { showGuide = false })
    }
}

@Composable
fun BloodPressureCategoriesDialog(selected: BpStatus, onDismiss: () -> Unit) {
    val colors = LocalAppColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blood pressure categories") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "American Heart Association adult categories, as systolic over diastolic in mm Hg. If the two numbers fall in different categories, the reading is classified in the higher one. This is not a diagnosis.",
                    color = colors.inkMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                BpStatus.explained.forEach { category ->
                    CategoryGuideRow(category, highlighted = category == selected)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
private fun CategoryGuideRow(category: BpStatus, highlighted: Boolean) {
    val colors = LocalAppColors.current
    val tint = Color(category.color)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlighted) tint.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(category.label, color = tint, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(category.range, color = colors.ink, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(category.description, color = colors.inkMuted, fontSize = 13.sp, lineHeight = 18.sp)
    }
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
        140f to 180f to Color(0xFFEF4444),
        180f to 200f to Color(0xFF991B1B),
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
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        ChartLegendSwatch(colors.brand, "Systolic")
        ChartLegendSwatch(colors.accent, "Diastolic")
        ChartLegendSwatch(colors.brand.copy(alpha = 0.45f), "Average")
    }
}

@Composable
fun BloodPressureZoneChart(systolic: Int, diastolic: Int) {
    val colors = LocalAppColors.current
    val status = bloodPressureStatus(systolic, diastolic)
    Text("Category chart", color = colors.inkMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(248.dp)
            .semantics {
                contentDescription = "Blood pressure category chart. Latest reading $systolic over $diastolic is ${status.label}."
            },
    ) {
        val padL = 56.dp.toPx()
        val padR = 8.dp.toPx()
        val padT = 8.dp.toPx()
        val padB = 34.dp.toPx()
        val xMin = 40f
        val xMax = 120f
        val yMin = 70f
        val yMax = 180f
        val plotW = (size.width - padL - padR).coerceAtLeast(1f)
        val plotH = (size.height - padT - padB).coerceAtLeast(1f)
        fun xOf(dia: Float) = padL + ((dia.coerceIn(xMin, xMax) - xMin) / (xMax - xMin)) * plotW
        fun yOf(sys: Float) = padT + ((yMax - sys.coerceIn(yMin, yMax)) / (yMax - yMin)) * plotH
        fun zone(dia0: Float, dia1: Float, sys0: Float, sys1: Float, color: Color) {
            val left = xOf(dia0)
            val top = yOf(sys1)
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size((xOf(dia1) - left).coerceAtLeast(1f), (yOf(sys0) - top).coerceAtLeast(1f)),
            )
        }
        zone(xMin, xMax, yMin, yMax, Color(0xFFEF4444))
        zone(40f, 90f, 70f, 140f, Color(0xFFF9A8D4))
        zone(40f, 80f, 70f, 130f, Color(0xFFFACC15))
        zone(40f, 80f, 70f, 120f, Color(0xFF4ADE80))
        zone(40f, 80f, 120f, 130f, Color(0xFFFACC15))
        drawRect(
            color = colors.ink,
            topLeft = Offset(xOf(xMin), yOf(yMax)),
            size = Size(plotW, plotH),
            style = Stroke(1.5.dp.toPx()),
        )
        val mark = Offset(xOf(diastolic.toFloat()), yOf(systolic.toFloat()))
        val dash = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))
        drawLine(
            color = colors.ink,
            start = Offset(mark.x, mark.y),
            end = Offset(mark.x, yOf(yMin)),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = dash,
        )
        drawLine(
            color = colors.ink,
            start = Offset(xOf(xMin), mark.y),
            end = Offset(mark.x, mark.y),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = dash,
        )
        drawCircle(Color.White, 7.dp.toPx(), mark)
        drawCircle(colors.ink, 4.5.dp.toPx(), mark)
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val axis = android.graphics.Paint().apply {
                isAntiAlias = true
                color = colors.inkMuted.toArgb()
                textSize = 10.sp.toPx()
            }
            val metrics = axis.fontMetrics
            axis.textAlign = android.graphics.Paint.Align.RIGHT
            listOf(70 to "70", 90 to "90", 110 to "110", 130 to "130", 150 to "150", 180 to "180+").forEach { (tick, label) ->
                val tickY = yOf(tick.toFloat()) - (metrics.ascent + metrics.descent) / 2f
                native.drawText(label, padL - 6.dp.toPx(), tickY, axis)
            }
            val xBaseline = size.height - 4.dp.toPx()
            listOf(40 to "40", 60 to "60", 80 to "80", 90 to "90", 120 to "120+").forEach { (tick, label) ->
                axis.textAlign = when (tick) {
                    40 -> android.graphics.Paint.Align.LEFT
                    120 -> android.graphics.Paint.Align.RIGHT
                    else -> android.graphics.Paint.Align.CENTER
                }
                native.drawText(label, xOf(tick.toFloat()), xBaseline, axis)
            }
            axis.textAlign = android.graphics.Paint.Align.CENTER
            native.save()
            native.rotate(-90f, 12.dp.toPx(), padT + plotH / 2f)
            native.drawText("Systolic", 12.dp.toPx(), padT + plotH / 2f, axis)
            native.restore()
            val zonePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }
            fun zoneLabel(text: String, dia: Float, sys: Float, color: Color, sizeSp: Float) {
                zonePaint.color = color.toArgb()
                zonePaint.textSize = sizeSp.sp.toPx()
                val font = zonePaint.fontMetrics
                native.drawText(text, xOf(dia), yOf(sys) - (font.ascent + font.descent) / 2f, zonePaint)
            }
            zoneLabel("Normal", 60f, 95f, Color(0xFF14532D), 12f)
            zoneLabel("Elevated", 60f, 125f, Color(0xFF713F12), 9f)
            zoneLabel("Stage 1 Hypertension", 65f, 137f, Color(0xFF9F1239), 10f)
            zoneLabel("Stage 2 Hypertension", 85f, 158f, Color.White, 11f)
        }
    }
    Text(
        "Diastolic · latest reading ${systolic}/${diastolic}",
        color = colors.inkMuted,
        fontSize = 12.sp,
    )
}

@Composable
private fun ChartLegendSwatch(color: Color, label: String) {
    val colors = LocalAppColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, color = colors.inkMuted, fontSize = 12.sp)
    }
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
    Text("Sessions", color = colors.inkMuted, fontSize = 12.sp)
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val maxCount = data.maxOf { it.count }.coerceAtLeast(1)
        val padL = 22.dp.toPx()
        val gap = 8.dp.toPx()
        val plotWidth = (size.width - padL).coerceAtLeast(1f)
        val barWidth = ((plotWidth - gap * (data.size - 1)) / data.size).coerceAtLeast(4f)
        val innerHeight = size.height - 8.dp.toPx()
        fun barX(index: Int) = padL + index * (barWidth + gap)
        val yTicks = listOf(0, maxCount)
        yTicks.forEach { tick ->
            val tickY = size.height - (tick.toFloat() / maxCount) * innerHeight
            drawLine(colors.border, Offset(padL, tickY), Offset(size.width, tickY), 1.dp.toPx())
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
                val tickY = size.height - (tick.toFloat() / maxCount) * innerHeight - (metrics.ascent + metrics.descent) / 2f
                canvas.nativeCanvas.drawText(tick.toString(), padL - 6.dp.toPx(), tickY, paint)
            }
        }
        data.forEachIndexed { index, bucket ->
            val barHeight = (bucket.count.toFloat() / maxCount) * innerHeight
            drawRoundRect(
                color = if (bucket.count == 0) colors.border else colors.brand,
                topLeft = Offset(barX(index), size.height - barHeight),
                size = Size(barWidth, barHeight.coerceAtLeast(if (bucket.count == 0) 2f else 6f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            )
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        data.forEach { bucket ->
            Text(bucket.label, color = colors.inkFaint, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
fun statusOf(reading: BloodPressureReading): BpStatus = bloodPressureStatus(reading.systolic, reading.diastolic)

val fieldShape = FieldShape
