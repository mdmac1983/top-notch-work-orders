package com.topnotchlock.workorder.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads text out of a vendor's PDF request. Most vendor emails export a
 * "digital" PDF (built from a template/email, not a scan), so we first try a
 * fast, free, fully-offline text-layer extraction with PdfBox. Only if that
 * comes back empty (a scanned/photographed PDF with no real text layer) do we
 * fall back to rendering each page as an image and running the same on-device
 * OCR used for screenshots.
 */
object PdfTextExtractor {

    private const val MIN_USABLE_TEXT_LENGTH = 20

    suspend fun extract(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val directText = extractTextLayer(context, uri)
        if (directText.trim().length >= MIN_USABLE_TEXT_LENGTH) {
            return@withContext directText
        }
        // Fall back to OCR on each rendered page (scanned/photographed PDF).
        extractViaOcr(context, uri)
    }

    private fun extractTextLayer(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                PDDocument.load(input).use { document ->
                    PDFTextStripper().getText(document)
                }
            } ?: ""
        }.getOrDefault("")
    }

    private suspend fun extractViaOcr(context: Context, uri: Uri): String {
        val tempFile = copyToCache(context, uri) ?: return ""
        return try {
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val builder = StringBuilder()
                    for (pageIndex in 0 until renderer.pageCount) {
                        renderer.openPage(pageIndex).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width * 2,
                                page.height * 2,
                                Bitmap.Config.ARGB_8888
                            )
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val pageText = ImageOcr.recognizeText(bitmap)
                            builder.append(pageText).append("\n")
                            bitmap.recycle()
                        }
                    }
                    builder.toString()
                }
            }
        } catch (e: Exception) {
            ""
        } finally {
            tempFile.delete()
        }
    }

    private fun copyToCache(context: Context, uri: Uri): File? = runCatching {
        val file = File.createTempFile("vendor_pdf_", ".pdf", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        file
    }.getOrNull()
}
