package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val MAX_DIMENSION_PX = 760
    private const val MAX_BASE64_LENGTH = 240_000

    /**
     * Compresses a photo selected via Android Photo Picker (PickVisualMedia) into a compact JPEG
     * Base64 string guaranteed to fit comfortably within Firestore security rule bounds (<= 260KB).
     */
    suspend fun encodeUriToBase64(context: Context, uri: Uri): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, boundsOptions)
                } ?: return@withContext Result.failure(IllegalArgumentException("Tidak dapat membuka foto."))

                var sampleSize = 1
                val maxSide = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                while (maxSide / sampleSize > MAX_DIMENSION_PX * 2) {
                    sampleSize *= 2
                }

                val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                val rawBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, decodeOptions)
                } ?: return@withContext Result.failure(IllegalArgumentException("Gagal membaca gambar."))

                val scaled = scaleBitmapDown(rawBitmap, MAX_DIMENSION_PX)
                if (scaled != rawBitmap) rawBitmap.recycle()

                var quality = 78
                var encoded: String
                do {
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    quality -= 12
                } while (encoded.length > MAX_BASE64_LENGTH && quality >= 30)

                scaled.recycle()

                if (encoded.length > MAX_BASE64_LENGTH) {
                    Result.failure(IllegalArgumentException("Ukuran foto terlalu besar. Pilih foto yang lebih kecil."))
                } else {
                    Result.success(encoded)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int
        if (ratio >= 1f) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }
}
