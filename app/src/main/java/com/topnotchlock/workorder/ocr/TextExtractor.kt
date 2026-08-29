package com.topnotchlock.workorder.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Single entry point the UI calls no matter which of the three input modes was used. */
object TextExtractor {

    fun fromPastedText(text: String): String = text

    suspend fun fromImageUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = decodeBitmap(context, uri) ?: return@withContext ""
        val fixed = fixOrientation(context, uri, bitmap)
        val text = ImageOcr.recognizeText(fixed)
        if (fixed !== bitmap) bitmap.recycle()
        fixed.recycle()
        text
    }

    suspend fun fromPdfUri(context: Context, uri: Uri): String =
        PdfTextExtractor.extract(context, uri)

    /** Downsamples large photos so OCR + memory stay well-behaved on older phones. */
    private fun decodeBitmap(context: Context, uri: Uri, maxDimension: Int = 2600): Bitmap? {
        val resolver = context.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
        val (width, height) = boundsOptions.outWidth to boundsOptions.outHeight
        if (width <= 0 || height <= 0) return null

        var sampleSize = 1
        while (width / (sampleSize * 2) >= maxDimension || height / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
    }

    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
                ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotationDegrees == 0f) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun cleanupTempFile(file: File?) {
        file?.takeIf { it.exists() }?.delete()
    }
}
