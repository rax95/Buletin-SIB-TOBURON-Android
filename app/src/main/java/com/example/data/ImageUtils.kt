package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.example.ui.i18n.AppLanguageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

data class EncodedPhoto(
    val base64: String,
    val sizeKb: Int,
    val width: Int,
    val height: Int
)

object ImageUtils {
    const val MAX_GALLERY_PHOTOS_PER_UPLOAD = 8
    const val MAX_GALLERY_TOTAL_BASE64_LENGTH = 760_000
    private const val INITIAL_MAX_DIMENSION_PX = 960
    private const val MIN_DIMENSION_PX = 320
    private const val MAX_BASE64_LENGTH = 240_000

    fun createCameraTempImageFile(context: Context): File {
        val dir = File(context.cacheDir, "camera_photos").apply {
            if (!exists()) mkdirs()
        }
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    fun getCameraTempImageUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /**
     * Decodes, auto-orients (EXIF), downscales, and compresses a device photo selected via
     * Android Photo Picker (PickVisualMedia / PickMultipleVisualMedia) or Document Picker
     * into a JPEG Base64 string guaranteed to fit comfortably within Firestore bounds.
     */
    suspend fun encodeUriToBase64(
        context: Context,
        uri: Uri,
        maxBase64Length: Int = MAX_BASE64_LENGTH,
        initialMaxDimensionPx: Int = INITIAL_MAX_DIMENSION_PX
    ): Result<EncodedPhoto> =
        withContext(Dispatchers.IO) {
            try {
                val safeMaxBase64 = maxBase64Length.coerceIn(45_000, MAX_BASE64_LENGTH)
                val safeInitialDim = initialMaxDimensionPx.coerceIn(MIN_DIMENSION_PX, INITIAL_MAX_DIMENSION_PX)
                val decoded = decodeBitmapSafely(context, uri, safeInitialDim)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException(
                            AppLanguageManager.tr(
                                "Format gambar tidak disokong atau fail tidak dapat dibaca.",
                                "Unsupported image format or unreadable file."
                            )
                        )
                    )

                var workingBitmap = scaleBitmapDown(decoded, safeInitialDim)
                if (workingBitmap != decoded) {
                    decoded.recycle()
                }

                var encodedResult: EncodedPhoto? = null
                var currentMaxDim = safeInitialDim

                while (encodedResult == null && currentMaxDim >= MIN_DIMENSION_PX) {
                    var quality = 82
                    while (quality >= 30) {
                        val out = ByteArrayOutputStream()
                        val compressedOk = workingBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                        if (!compressedOk) {
                            workingBitmap.recycle()
                            return@withContext Result.failure(
                                IllegalStateException(
                                    AppLanguageManager.tr(
                                        "Gagal memampatkan gambar ke format JPEG.",
                                        "Failed to compress image to JPEG format."
                                    )
                                )
                            )
                        }
                        val bytes = out.toByteArray()
                        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        if (b64.length <= safeMaxBase64) {
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
                        IllegalArgumentException(
                            AppLanguageManager.tr(
                                "Saiz gambar masih terlalu besar selepas pemampatan. Sila pilih foto lain.",
                                "Image size is still too large after compression. Please pick another photo."
                            )
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(
                    IllegalStateException(
                        AppLanguageManager.tr(
                            "Gagal membaca foto daripada peranti: ${e.localizedMessage ?: "Ralat tidak diketahui"}",
                            "Failed to read photo from device: ${e.localizedMessage ?: "Unknown error"}"
                        ),
                        e
                    )
                )
            }
        }

    private fun decodeBitmapSafely(context: Context, uri: Uri, maxDimPx: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    // Software allocator is mandatory so Bitmap.compress(JPEG) never crashes
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val width = info.size.width
                    val height = info.size.height
                    val maxSide = maxOf(width, height)
                    if (maxSide > maxDimPx) {
                        val scale = maxDimPx.toFloat() / maxSide.toFloat()
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
        while (maxSide / sampleSize > maxDimPx * 2) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
            ?: return null
        return applyExifOrientation(rawBytes, bitmap)
    }

    private fun applyExifOrientation(rawBytes: ByteArray, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(rawBytes)).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }

        return runCatching {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        }.getOrDefault(bitmap)
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
