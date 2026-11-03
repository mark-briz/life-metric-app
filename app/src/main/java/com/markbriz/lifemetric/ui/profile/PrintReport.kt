package com.markbriz.lifemetric.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.text.TextPaint
import com.markbriz.lifemetric.domain.BloodPressurePrintTable
import com.markbriz.lifemetric.domain.WeightChangeDirection
import com.markbriz.lifemetric.domain.WeightPrintTable
import com.markbriz.lifemetric.domain.WorkoutPrintTable

/** Print canvas units are points (1/72"). Do not scale by screen density. */
private const val PRINT_TEXT_PT = 12f
private const val PRINT_ROW_PT = 16f
private const val PRINT_HEADER_PT = 16f
private const val PRINT_WORKOUT_TEXT_PT = 9f
private const val PRINT_WORKOUT_ROW_PT = 18f
private const val PRINT_WORKOUT_HEADER_PT = 28f

/** Opens the system print sheet for a paginated text history. */
fun printTextReport(context: Context, jobName: String, body: String) {
    printDocument(context, jobName) { TextPrintAdapter(context.applicationContext, jobName, body) }
}

fun printWeightTable(context: Context, jobName: String, table: WeightPrintTable) {
    printDocument(context, jobName) { WeightTablePrintAdapter(context.applicationContext, jobName, table) }
}

fun printBloodPressureTable(context: Context, jobName: String, table: BloodPressurePrintTable) {
    printDocument(context, jobName) { BloodPressureTablePrintAdapter(context.applicationContext, jobName, table) }
}

fun printWorkoutTable(context: Context, jobName: String, table: WorkoutPrintTable) {
    printDocument(context, jobName) { WorkoutTablePrintAdapter(context.applicationContext, jobName, table) }
}

private fun printDocument(context: Context, jobName: String, adapter: () -> PrintDocumentAdapter) {
    val manager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    manager.print(
        jobName,
        adapter(),
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .build(),
    )
}

private class TextPrintAdapter(
    private val context: Context,
    private val jobName: String,
    private val body: String,
) : PrintDocumentAdapter() {
    private var document: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        document?.close()
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val pdf = PrintedPdfDocument(context, newAttributes)
        document = pdf
        val pageCount = layoutPages(pdf)
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount)
                .build(),
            true,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val pdf = document
        if (pdf == null || cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }
        destination.fileDescriptor.let { fd ->
            java.io.FileOutputStream(fd).use { pdf.writeTo(it) }
        }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

    override fun onFinish() {
        document?.close()
        document = null
    }

    private fun layoutPages(pdf: PrintedPdfDocument): Int {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.SANS_SERIF
        }
        var pageIndex = 0
        var page = pdf.startPage(pageIndex)
        val margin = 36f
        val left = margin
        val top = margin
        val maxWidth = (page.canvas.width - 2 * margin).coerceAtLeast(120f)
        val bottom = page.canvas.height - margin
        val lineHeight = paint.fontSpacing
        val lines = body.split('\n').flatMap { wrapLine(it, paint, maxWidth) }
        var y = top
        lines.forEach { line ->
            if (y + lineHeight > bottom) {
                pdf.finishPage(page)
                pageIndex += 1
                page = pdf.startPage(pageIndex)
                y = top
            }
            page.canvas.drawText(line, left, y + paint.textSize, paint)
            y += lineHeight
        }
        pdf.finishPage(page)
        return pageIndex + 1
    }
}

private class WeightTablePrintAdapter(
    private val context: Context,
    private val jobName: String,
    private val table: WeightPrintTable,
) : PrintDocumentAdapter() {
    private var document: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        document?.close()
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val pdf = PrintedPdfDocument(context, newAttributes)
        document = pdf
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(layoutPages(pdf))
                .build(),
            true,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val pdf = document
        if (pdf == null || cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }
        java.io.FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

    override fun onFinish() {
        document?.close()
        document = null
    }

    private fun layoutPages(pdf: PrintedPdfDocument): Int {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.SANS_SERIF
        }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.SANS_SERIF
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val headerFill = Paint().apply { color = 0xFFE2E8F0.toInt() }
        val stripeFill = Paint().apply { color = 0xFFF8FAFC.toInt() }
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            style = Paint.Style.FILL
        }

        var pageIndex = 0
        var page = pdf.startPage(pageIndex)
        val margin = 36f
        val left = margin
        val top = margin
        val right = page.canvas.width - margin
        val bottom = page.canvas.height - margin
        val tableWidth = right - left
        val dateW = tableWidth * 0.24f
        val timeW = tableWidth * 0.20f
        val weightW = tableWidth * 0.36f
        val changeW = tableWidth - dateW - timeW - weightW
        val rowH = PRINT_ROW_PT
        val headerH = PRINT_HEADER_PT

        fun newPage() {
            pdf.finishPage(page)
            pageIndex += 1
            page = pdf.startPage(pageIndex)
        }

        fun drawTableHeader(canvas: Canvas, y: Float) {
            canvas.drawRect(left, y, right, y + headerH, headerFill)
            val textY = y + headerH * 0.68f
            canvas.drawText("Date", left + 8f, textY, headerPaint)
            canvas.drawText("Time", left + dateW + 8f, textY, headerPaint)
            canvas.drawText("Weight", left + dateW + timeW + 8f, textY, headerPaint)
            canvas.drawText("Change", left + dateW + timeW + weightW + 8f, textY, headerPaint)
            canvas.drawRect(left, y, right, y + headerH, linePaint)
        }

        var y = top
        page.canvas.drawText(table.title, left, y + titlePaint.textSize, titlePaint)
        y += titlePaint.fontSpacing + 4f
        table.lines.forEach { line ->
            page.canvas.drawText(line, left, y + metaPaint.textSize, metaPaint)
            y += metaPaint.fontSpacing
        }
        y += 10f
        drawTableHeader(page.canvas, y)
        y += headerH

        table.rows.forEachIndexed { index, row ->
            if (y + rowH > bottom) {
                newPage()
                y = top
                drawTableHeader(page.canvas, y)
                y += headerH
            }
            val canvas = page.canvas
            if (index % 2 == 1) canvas.drawRect(left, y, right, y + rowH, stripeFill)
            val textY = y + rowH * 0.68f
            canvas.drawText(row.date, left + 8f, textY, cellPaint)
            canvas.drawText(row.time, left + dateW + 8f, textY, cellPaint)
            canvas.drawText(row.weight, left + dateW + timeW + 8f, textY, cellPaint)
            val iconCx = left + dateW + timeW + weightW + changeW / 2f
            val iconCy = y + rowH / 2f
            drawChangeIcon(canvas, iconPaint, iconCx, iconCy, 4.5f, row.direction)
            canvas.drawLine(left, y + rowH, right, y + rowH, linePaint)
            canvas.drawLine(left, y, left, y + rowH, linePaint)
            canvas.drawLine(left + dateW, y, left + dateW, y + rowH, linePaint)
            canvas.drawLine(left + dateW + timeW, y, left + dateW + timeW, y + rowH, linePaint)
            canvas.drawLine(left + dateW + timeW + weightW, y, left + dateW + timeW + weightW, y + rowH, linePaint)
            canvas.drawLine(right, y, right, y + rowH, linePaint)
            y += rowH
        }
        pdf.finishPage(page)
        return pageIndex + 1
    }
}

private class BloodPressureTablePrintAdapter(
    private val context: Context,
    private val jobName: String,
    private val table: BloodPressurePrintTable,
) : PrintDocumentAdapter() {
    private var document: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        document?.close()
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val pdf = PrintedPdfDocument(context, newAttributes)
        document = pdf
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(layoutPages(pdf))
                .build(),
            true,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val pdf = document
        if (pdf == null || cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }
        java.io.FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

    override fun onFinish() {
        document?.close()
        document = null
    }

    private fun layoutPages(pdf: PrintedPdfDocument): Int {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.SANS_SERIF
        }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.SANS_SERIF
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val headerFill = Paint().apply { color = 0xFFE2E8F0.toInt() }
        val stripeFill = Paint().apply { color = 0xFFF8FAFC.toInt() }

        var pageIndex = 0
        var page = pdf.startPage(pageIndex)
        val margin = 36f
        val left = margin
        val top = margin
        val right = page.canvas.width - margin
        val bottom = page.canvas.height - margin
        val tableWidth = right - left
        val dateW = tableWidth * 0.16f
        val amPressureW = tableWidth * 0.22f
        val amHeartW = tableWidth * 0.16f
        val pmPressureW = tableWidth * 0.22f
        val pmHeartW = tableWidth - dateW - amPressureW - amHeartW - pmPressureW
        val xDate = left
        val xAmPressure = xDate + dateW
        val xAmHeart = xAmPressure + amPressureW
        val xPmPressure = xAmHeart + amHeartW
        val xPmHeart = xPmPressure + pmPressureW
        val rowH = PRINT_ROW_PT
        val groupH = PRINT_HEADER_PT
        val headerH = PRINT_HEADER_PT * 2f

        fun newPage() {
            pdf.finishPage(page)
            pageIndex += 1
            page = pdf.startPage(pageIndex)
        }

        fun headerBaseline(top: Float, height: Float): Float = top + height * 0.68f

        fun drawCentered(canvas: Canvas, text: String, x: Float, width: Float, textY: Float) {
            val textWidth = headerPaint.measureText(text)
            canvas.drawText(text, x + (width - textWidth) / 2f, textY, headerPaint)
        }

        fun drawTableHeader(canvas: Canvas, y: Float) {
            canvas.drawRect(left, y, right, y + headerH, headerFill)
            canvas.drawText("Date", left + 8f, headerBaseline(y, headerH), headerPaint)
            drawCentered(canvas, "Morning", xAmPressure, amPressureW + amHeartW, headerBaseline(y, groupH))
            drawCentered(canvas, "Evening", xPmPressure, pmPressureW + pmHeartW, headerBaseline(y, groupH))
            val colY = y + groupH
            canvas.drawText("Blood pressure", xAmPressure + 8f, headerBaseline(colY, groupH), headerPaint)
            canvas.drawText("Heart rate", xAmHeart + 8f, headerBaseline(colY, groupH), headerPaint)
            canvas.drawText("Blood pressure", xPmPressure + 8f, headerBaseline(colY, groupH), headerPaint)
            canvas.drawText("Heart rate", xPmHeart + 8f, headerBaseline(colY, groupH), headerPaint)
            canvas.drawLine(left, colY, right, colY, linePaint)
            canvas.drawRect(left, y, right, y + headerH, linePaint)
            canvas.drawLine(xAmPressure, y, xAmPressure, y + headerH, linePaint)
            canvas.drawLine(xAmHeart, colY, xAmHeart, y + headerH, linePaint)
            canvas.drawLine(xPmPressure, y, xPmPressure, y + headerH, linePaint)
            canvas.drawLine(xPmHeart, colY, xPmHeart, y + headerH, linePaint)
        }

        var y = top
        page.canvas.drawText(table.title, left, y + titlePaint.textSize, titlePaint)
        y += titlePaint.fontSpacing + 4f
        table.lines.forEach { line ->
            page.canvas.drawText(line, left, y + metaPaint.textSize, metaPaint)
            y += metaPaint.fontSpacing
        }
        y += 10f
        drawTableHeader(page.canvas, y)
        y += headerH

        table.rows.forEachIndexed { index, row ->
            if (y + rowH > bottom) {
                newPage()
                y = top
                drawTableHeader(page.canvas, y)
                y += headerH
            }
            val canvas = page.canvas
            if (index % 2 == 1) canvas.drawRect(left, y, right, y + rowH, stripeFill)
            val textY = y + rowH * 0.68f
            canvas.drawText(row.date, left + 8f, textY, cellPaint)
            canvas.drawText(row.amPressure, xAmPressure + 8f, textY, cellPaint)
            canvas.drawText(row.amHeartRate, xAmHeart + 8f, textY, cellPaint)
            canvas.drawText(row.pmPressure, xPmPressure + 8f, textY, cellPaint)
            canvas.drawText(row.pmHeartRate, xPmHeart + 8f, textY, cellPaint)
            canvas.drawLine(left, y + rowH, right, y + rowH, linePaint)
            canvas.drawLine(left, y, left, y + rowH, linePaint)
            canvas.drawLine(xAmPressure, y, xAmPressure, y + rowH, linePaint)
            canvas.drawLine(xAmHeart, y, xAmHeart, y + rowH, linePaint)
            canvas.drawLine(xPmPressure, y, xPmPressure, y + rowH, linePaint)
            canvas.drawLine(xPmHeart, y, xPmHeart, y + rowH, linePaint)
            canvas.drawLine(right, y, right, y + rowH, linePaint)
            y += rowH
        }
        pdf.finishPage(page)
        return pageIndex + 1
    }
}

private class WorkoutTablePrintAdapter(
    private val context: Context,
    private val jobName: String,
    private val table: WorkoutPrintTable,
) : PrintDocumentAdapter() {
    private var document: PrintedPdfDocument? = null

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        document?.close()
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        val pdf = PrintedPdfDocument(context, newAttributes)
        document = pdf
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder("$jobName.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(layoutPages(pdf))
                .build(),
            true,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        val pdf = document
        if (pdf == null || cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }
        java.io.FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
    }

    override fun onFinish() {
        document?.close()
        document = null
    }

    private fun layoutPages(pdf: PrintedPdfDocument): Int {
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF334155.toInt()
            textSize = PRINT_TEXT_PT
            typeface = Typeface.SANS_SERIF
        }
        val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_WORKOUT_TEXT_PT
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF111111.toInt()
            textSize = PRINT_WORKOUT_TEXT_PT
            typeface = Typeface.SANS_SERIF
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF94A3B8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
        }
        val headerFill = Paint().apply { color = 0xFFE2E8F0.toInt() }
        val stripeFill = Paint().apply { color = 0xFFF8FAFC.toInt() }

        var pageIndex = 0
        var page = pdf.startPage(pageIndex)
        val margin = 36f
        val left = margin
        val top = margin
        val right = page.canvas.width - margin
        val bottom = page.canvas.height - margin
        val tableWidth = right - left
        val dateW = tableWidth * 0.14f
        val categoryCount = table.columns.size.coerceAtLeast(1)
        val categoryW = (tableWidth - dateW) / categoryCount
        val rowH = PRINT_WORKOUT_ROW_PT
        val headerH = PRINT_WORKOUT_HEADER_PT

        fun newPage() {
            pdf.finishPage(page)
            pageIndex += 1
            page = pdf.startPage(pageIndex)
        }

        fun categoryLeft(index: Int): Float = left + dateW + categoryW * index

        fun drawWrappedCentered(canvas: Canvas, text: String, x: Float, width: Float, topY: Float, paint: TextPaint) {
            val lines = wrapLine(text, paint, width - 6f)
            val lineHeight = paint.fontSpacing
            var y = topY + paint.textSize
            lines.forEach { line ->
                val textWidth = paint.measureText(line)
                canvas.drawText(line, x + (width - textWidth) / 2f, y, paint)
                y += lineHeight
            }
        }

        fun drawTableHeader(canvas: Canvas, y: Float) {
            canvas.drawRect(left, y, right, y + headerH, headerFill)
            canvas.drawText("Date", left + 6f, y + headerH * 0.62f, headerPaint)
            table.columns.forEachIndexed { index, label ->
                drawWrappedCentered(canvas, label, categoryLeft(index), categoryW, y + 4f, headerPaint)
            }
            canvas.drawRect(left, y, right, y + headerH, linePaint)
            canvas.drawLine(left + dateW, y, left + dateW, y + headerH, linePaint)
            for (i in 1 until categoryCount) {
                val x = categoryLeft(i)
                canvas.drawLine(x, y, x, y + headerH, linePaint)
            }
        }

        var y = top
        page.canvas.drawText(table.title, left, y + titlePaint.textSize, titlePaint)
        y += titlePaint.fontSpacing + 4f
        table.lines.forEach { line ->
            page.canvas.drawText(line, left, y + metaPaint.textSize, metaPaint)
            y += metaPaint.fontSpacing
        }
        y += 10f
        drawTableHeader(page.canvas, y)
        y += headerH

        table.rows.forEachIndexed { index, row ->
            if (y + rowH > bottom) {
                newPage()
                y = top
                drawTableHeader(page.canvas, y)
                y += headerH
            }
            val canvas = page.canvas
            if (index % 2 == 1) canvas.drawRect(left, y, right, y + rowH, stripeFill)
            val textY = y + rowH * 0.68f
            canvas.drawText(row.date, left + 6f, textY, cellPaint)
            row.cells.forEachIndexed { cellIndex, value ->
                if (value.isNotEmpty()) {
                    val x = categoryLeft(cellIndex)
                    val textWidth = cellPaint.measureText(value)
                    canvas.drawText(value, x + (categoryW - textWidth) / 2f, textY, cellPaint)
                }
            }
            canvas.drawLine(left, y + rowH, right, y + rowH, linePaint)
            canvas.drawLine(left, y, left, y + rowH, linePaint)
            canvas.drawLine(left + dateW, y, left + dateW, y + rowH, linePaint)
            for (i in 1 until categoryCount) {
                val x = categoryLeft(i)
                canvas.drawLine(x, y, x, y + rowH, linePaint)
            }
            canvas.drawLine(right, y, right, y + rowH, linePaint)
            y += rowH
        }
        pdf.finishPage(page)
        return pageIndex + 1
    }
}

private fun drawChangeIcon(
    canvas: Canvas,
    paint: Paint,
    cx: Float,
    cy: Float,
    size: Float,
    direction: WeightChangeDirection?,
) {
    when (direction) {
        WeightChangeDirection.Up -> {
            val path = Path()
            path.moveTo(cx, cy - size)
            path.lineTo(cx + size, cy + size * 0.7f)
            path.lineTo(cx - size, cy + size * 0.7f)
            path.close()
            canvas.drawPath(path, paint)
        }
        WeightChangeDirection.Down -> {
            val path = Path()
            path.moveTo(cx, cy + size)
            path.lineTo(cx + size, cy - size * 0.7f)
            path.lineTo(cx - size, cy - size * 0.7f)
            path.close()
            canvas.drawPath(path, paint)
        }
        WeightChangeDirection.Unchanged -> {
            paint.strokeWidth = size * 0.35f
            paint.style = Paint.Style.STROKE
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(cx - size, cy, cx + size, cy, paint)
            paint.style = Paint.Style.FILL
        }
        null -> Unit
    }
}

private fun wrapLine(text: String, paint: Paint, maxWidth: Float): List<String> {
    if (text.isEmpty()) return listOf("")
    val out = mutableListOf<String>()
    var remaining = text
    while (remaining.isNotEmpty()) {
        val count = paint.breakText(remaining, true, maxWidth, null)
        if (count <= 0) {
            out.add(remaining)
            break
        }
        out.add(remaining.substring(0, count).trimEnd())
        remaining = remaining.substring(count)
    }
    return out.ifEmpty { listOf("") }
}
