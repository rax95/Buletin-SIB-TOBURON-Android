package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class EncodedPhoto(
    val base64: String,
    val sizeKb: Int,
    val width: Int,
    val height: Int
)

object ImageUtils {
    private const val INITIAL_MAX_DIMENSION_PX = 960
    private const val MIN_DIMENSION_PX = 360
    private const val MAX_BASE64_LENGTH = 240_000

    /**
     * Decodes, auto-orients (EXIF), downscales, and compresses a device photo selected via
     * Android Photo Picker (PickVisualMedia) or Document Picker (OpenDocument) into a JPEG
     * Base64 string guaranteed to fit comfortably within Firestore security rule bounds (<= 260KB).
     */
    suspend fun encodeUriToBase64(context: Context, uri: Uri): Result<EncodedPhoto> =
        withContext(Dispatchers.IO) {
            try {
                val decoded = decodeBitmapSafely(context, uri)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Format gambar tidak didukung atau file tidak dapat dibaca.")
                    )

                var workingBitmap = scaleBitmapDown(decoded, INITIAL_MAX_DIMENSION_PX)
                if (workingBitmap != decoded) {
                    decoded.recycle()
                }

                var encodedResult: EncodedPhoto? = null
                var currentMaxDim = INITIAL_MAX_DIMENSION_PX

                while (encodedResult == null && currentMaxDim >= MIN_DIMENSION_PX) {
                    var quality = 82
                    while (quality >= 34) {
                        val out = ByteArrayOutputStream()
                        val compressedOk = workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                        if (!compressedOk) {
                            workingBitmap.recycle()
                            return@withContext Result.failure(
                                IllegalStateException("Gagal mengompresi gambar ke format JPEG.")
                            )
                        }
                        val bytes = out.toByteArray()
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        if (b64.length <= MAX_BASE64_LENGTH) {
                            encodedResult = EncodedPhoto(
                                base64 = b64,
                                sizeKb = (bytes.size / 1024).coerceAtLeast(1),
                                width = workingBitmap.width,
                                height = workingBitmap.height
                            )
                            break
                        }
                        quality -= 12
                    }

                    if (encodedResult == null) {
                        val nextDim = (currentMaxDim * 0.75f).toInt()
                        if (nextDim < MIN_DIMENSION_PX || nextDim >= currentMaxDim) {
                            break
                        }
                        currentMaxDim = nextDim
                        val smaller = scaleBitmapDown(workingBitmap, currentMaxDim)
                        if (smaller != workingBitmap) {
                            workingBitmap.recycle()
                            workingBitmap = smaller
                        }
                    }
                }

                workingBitmap.recycle()

                if (encodedResult != null) {
                    Result.success(encodedResult)
                } else {
                    Result.failure(
                        IllegalArgumentException("Ukuran gambar masih terlalu besar setelah kompresi. Coba pilih foto lain.")
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException("Gagal membaca foto dari perangkat: ${e.localizedMessage ?: "Kesalahan tidak diketahui"}", e)
                )
            }
        }

    private fun decodeBitmapSafely(context: Context, uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    // Software allocator is mandatory so Bitmap.compress(JPEG) never crashes
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val width = info.size.width
                    val height = info.size.height
                    val maxSide = maxOf(width, height)
                    if (maxSide > INITIAL_MAX_DIMENSION_PX) {
                        val scale = INITIAL_MAX_DIMENSION_PX.toFloat() / maxSide.toFloat()
                        val targetW = (width * scale).toInt().coerceAtLeast(1)
                        val targetH = (height * scale).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            } catch (_: Exception) {
                // Fall back to BitmapFactory stream decoding for providers that don't support ImageDecoder
            }
        }

        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        if (rawBytes.isEmpty()) return null

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
        val outW = boundsOptions.outWidth
        val outH = boundsOptions.outHeight
        if (outW <= 0 || outH <= 0) return null

        var sampleSize = 1
        val maxSide = maxOf(outW, outH)
        while (maxSide / sampleSize > INITIAL_MAX_DIMENSION_PX * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
    }

    fun decodeBase64ToImageBitmap(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return try {
            val cleaned = base64.substringAfter("base64,", base64).trim()
            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
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
