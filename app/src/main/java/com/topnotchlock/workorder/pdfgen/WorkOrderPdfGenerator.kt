package com.topnotchlock.workorder.pdfgen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.topnotchlock.workorder.R
import com.topnotchlock.workorder.data.ACTION_REQUIRED_ITEMS
import com.topnotchlock.workorder.data.WorkOrder
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a [WorkOrder] onto a single US Letter (8.5" x 11") PDF page, matching
 * the Top Notch Lock paper template layout. If the content would spill onto a
 * second page at normal size (e.g. a very long PROBLEM REPORTED description),
 * font size, line spacing, and margins are shrunk step by step until
 * everything fits on one page.
 */
object WorkOrderPdfGenerator {

    // US Letter at 72pt/inch.
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792

    private const val DEFAULT_COMPANY_NAME = "TOP NOTCH LOCK"
    private const val DEFAULT_COMPANY_PHONE = "(800)-381-7033"

    private sealed class Block {
        data object Logo : Block()
        data class Title(val text: String) : Block()
        data class SectionHeader(val text: String) : Block()
        data class BodyLine(val text: String) : Block()
        data class ChecklistItem(val text: String) : Block()
        data class RuledLine(val label: String = "") : Block()
        data class Spacer(val weight: Float = 1f) : Block()
    }

    private data class Sizes(
        val titlePt: Float,
        val headerPt: Float,
        val bodyPt: Float,
        val lineSpacingMult: Float,
        val sectionGap: Float,
        val margin: Float,
        val logoWidthPt: Float
    )

    private val BASE_SIZES = Sizes(
        titlePt = 17f, headerPt = 13f, bodyPt = 11f,
        lineSpacingMult = 1.18f, sectionGap = 12f, margin = 36f,
        logoWidthPt = 145f
    )
    private const val MIN_SCALE = 0.55f
    private const val SCALE_STEP = 0.04f

    fun generate(context: Context, workOrder: WorkOrder, outputFile: File): File {
        val logoBitmap = runCatching {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_full)
        }.getOrNull()
        val logoAspect = if (logoBitmap != null && logoBitmap.width > 0) {
            logoBitmap.height.toFloat() / logoBitmap.width.toFloat()
        } else {
            0.7f
        }

        val blocks = buildBlocks(workOrder, hasLogo = logoBitmap != null)
        val document = PdfDocument()

        var scale = 1f
        var chosen: Pair<Sizes, Float>? = null
        while (scale >= MIN_SCALE) {
            val sizes = scaled(BASE_SIZES, scale)
            val contentWidth = PAGE_WIDTH - 2 * sizes.margin
            val height = measureTotalHeight(blocks, sizes, contentWidth, logoAspect)
            val maxHeight = PAGE_HEIGHT - 2 * sizes.margin
            if (height <= maxHeight) {
                chosen = sizes to height
                break
            }
            scale -= SCALE_STEP
        }
        // Even at the floor scale, render with whatever we've got - better a slightly
        // tight page than a missing one. This should not happen for realistic content.
        val finalSizes = chosen?.first ?: scaled(BASE_SIZES, MIN_SCALE)

        val page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create())
        if (logoBitmap != null) {
            drawWatermark(page.canvas, logoBitmap, logoAspect)
        }
        drawBlocks(page.canvas, blocks, finalSizes, logoBitmap, logoAspect)
        document.finishPage(page)

        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
        logoBitmap?.recycle()
        return outputFile
    }

    /** Large, faded copy of the logo centered on the page, behind everything else. */
    private fun drawWatermark(canvas: Canvas, bitmap: Bitmap, aspect: Float) {
        val width = PAGE_WIDTH * 0.62f
        val height = width * aspect
        val left = (PAGE_WIDTH - width) / 2f
        val top = (PAGE_HEIGHT - height) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { alpha = 26 }
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + width, top + height), paint)
    }

    private fun buildBlocks(wo: WorkOrder, hasLogo: Boolean): List<Block> {
        val list = mutableListOf<Block>()
        val companyName = wo.companyName.ifBlank { DEFAULT_COMPANY_NAME }
        val companyPhone = wo.companyPhone.ifBlank { DEFAULT_COMPANY_PHONE }

        if (hasLogo) {
            list += Block.Logo
            list += Block.Spacer(0.3f)
        }
        list += Block.Title("$companyName $companyPhone")
        list += Block.Spacer(0.6f)
        list += Block.BodyLine("WORK ORDER #: ${wo.woNumber}    -    DATE: ${wo.date}")
        list += Block.Spacer()

        list += Block.SectionHeader("SITE INFORMATION:")
        list += Block.BodyLine("Site Name and/or Site ID: ${wo.siteName};${wo.siteId}")
        list += Block.BodyLine("Address: ${wo.address}")
        list += Block.BodyLine("Contact: ${wo.contact}    -    Phone: ${wo.phone}")
        list += Block.Spacer()

        list += Block.SectionHeader("JOB DETAILS:")
        list += Block.BodyLine("Arrive By: ${wo.arriveBy}")
        list += Block.BodyLine("Complete By: ${wo.completeBy}")
        list += Block.Spacer()

        list += Block.SectionHeader("PROBLEM REPORTED:")
        list += Block.BodyLine(wo.problem.ifBlank { " " })
        list += Block.Spacer()

        list += Block.SectionHeader("ACTION REQUIRED: (MUST CHECK EACH ONE)")
        ACTION_REQUIRED_ITEMS.forEach { list += Block.ChecklistItem(it) }
        list += Block.Spacer()

        list += Block.SectionHeader("DESCRIPTION OF WORK PERFORMED:")
        repeat(4) { list += Block.RuledLine() }
        list += Block.Spacer()

        list += Block.SectionHeader("SIGNATURES:")
        list += Block.RuledLine("Technician: _________________________        Date: _______")
        list += Block.RuledLine("Time In: _______        Time Out: _______")
        list += Block.RuledLine("Manager on Duty: _________________________")
        return list
    }

    private fun scaled(base: Sizes, scale: Float): Sizes = Sizes(
        titlePt = base.titlePt * scale,
        headerPt = base.headerPt * scale,
        bodyPt = base.bodyPt * scale,
        lineSpacingMult = base.lineSpacingMult,
        sectionGap = base.sectionGap * scale,
        margin = if (scale < 0.7f) base.margin * 0.75f else base.margin,
        logoWidthPt = base.logoWidthPt * scale
    )

    private fun paintFor(block: Block, sizes: Sizes): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        when (block) {
            is Block.Title -> { textSize = sizes.titlePt; isFakeBoldText = true }
            is Block.SectionHeader -> { textSize = sizes.headerPt; isFakeBoldText = true }
            else -> { textSize = sizes.bodyPt; isFakeBoldText = false }
        }
    }

    private fun textOf(block: Block): String = when (block) {
        is Block.Title -> block.text
        is Block.SectionHeader -> block.text
        is Block.BodyLine -> block.text
        is Block.ChecklistItem -> block.text
        is Block.RuledLine -> block.label
        is Block.Logo -> ""
        is Block.Spacer -> ""
    }

    /** Extra left indent reserved for the hand-drawn checkbox square. */
    private fun checklistIndent(sizes: Sizes): Float = sizes.bodyPt * 1.7f

    private fun layoutFor(block: Block, sizes: Sizes, width: Int): StaticLayout {
        val paint = paintFor(block, sizes)
        val text = textOf(block)
        val effectiveWidth = if (block is Block.ChecklistItem) {
            (width - checklistIndent(sizes)).toInt().coerceAtLeast(1)
        } else width
        val builder = StaticLayout.Builder.obtain(text, 0, text.length, paint, effectiveWidth)
            .setLineSpacing(0f, sizes.lineSpacingMult)
            .setIncludePad(false)
        if (block is Block.Title) builder.setAlignment(Layout.Alignment.ALIGN_CENTER)
        return builder.build()
    }

    private fun measureTotalHeight(blocks: List<Block>, sizes: Sizes, contentWidth: Float, logoAspect: Float): Float {
        var total = 0f
        val width = contentWidth.toInt().coerceAtLeast(1)
        blocks.forEach { block ->
            total += when (block) {
                is Block.Logo -> sizes.logoWidthPt * logoAspect
                is Block.Spacer -> sizes.sectionGap * block.weight
                is Block.RuledLine -> layoutFor(block, sizes, width).height.toFloat() + sizes.bodyPt * 0.6f
                else -> layoutFor(block, sizes, width).height.toFloat()
            }
        }
        return total
    }

    private fun drawBlocks(
        canvas: Canvas,
        blocks: List<Block>,
        sizes: Sizes,
        logoBitmap: Bitmap?,
        logoAspect: Float
    ) {
        val width = (PAGE_WIDTH - 2 * sizes.margin).toInt().coerceAtLeast(1)
        var y = sizes.margin

        blocks.forEach { block ->
            when (block) {
                is Block.Logo -> {
                    val logoHeight = sizes.logoWidthPt * logoAspect
                    if (logoBitmap != null) {
                        val left = (PAGE_WIDTH - sizes.logoWidthPt) / 2f
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                        canvas.drawBitmap(
                            logoBitmap, null,
                            RectF(left, y, left + sizes.logoWidthPt, y + logoHeight),
                            paint
                        )
                    }
                    y += logoHeight
                }
                is Block.Spacer -> {
                    y += sizes.sectionGap * block.weight
                }
                is Block.RuledLine -> {
                    val layout = layoutFor(block, sizes, width)
                    canvas.save()
                    canvas.translate(sizes.margin, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height.toFloat()
                    // Underline rule for blank "write here" lines.
                    if (block.label.isBlank()) {
                        val lineY = y - layout.height * 0.15f
                        val rulePaint = Paint().apply { color = Color.BLACK; strokeWidth = 1f }
                        canvas.drawLine(sizes.margin, lineY, sizes.margin + width, lineY, rulePaint)
                    }
                    y += sizes.bodyPt * 0.6f
                }
                is Block.ChecklistItem -> {
                    val indent = checklistIndent(sizes)
                    val layout = layoutFor(block, sizes, width)
                    val boxSize = sizes.bodyPt * 0.85f
                    val boxPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE
                        strokeWidth = 1.2f
                    }
                    val boxTop = y + (layout.getLineBaseline(0) - boxSize) * 0.75f
                    canvas.drawRect(sizes.margin, boxTop, sizes.margin + boxSize, boxTop + boxSize, boxPaint)

                    canvas.save()
                    canvas.translate(sizes.margin + indent, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height.toFloat()
                }
                else -> {
                    val layout = layoutFor(block, sizes, width)
                    canvas.save()
                    canvas.translate(sizes.margin, y)
                    layout.draw(canvas)
                    canvas.restore()
                    y += layout.height.toFloat()
                }
            }
        }
    }
}
