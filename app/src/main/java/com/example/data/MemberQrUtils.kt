package com.example.data

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

data class ParsedMemberQr(
    val memberId: String?,
    val email: String?,
    val uid: String?
)

object MemberQrUtils {
    private const val QR_SCHEME = "sibtoburon"
    private const val QR_HOST = "member"

    /**
     * Builds a compact, privacy-safe URI payload for a Member Card QR code.
     * Avoids exposing raw MyKad or sensitive personal details in plaintext;
     * when scanned by an Admin inside the app, the app loads the live full
     * ChurchMemberDetail record from Firestore securely.
     */
    fun buildMemberQrPayload(
        memberId: String?,
        email: String? = null,
        uid: String? = null,
        sibNumber: String? = null
    ): String {
        val builder = Uri.Builder()
            .scheme(QR_SCHEME)
            .authority(QR_HOST)

        val cleanMemberId = memberId?.trim().orEmpty()
        if (cleanMemberId.isNotEmpty()) {
            builder.appendQueryParameter("id", cleanMemberId)
        }
        val cleanSib = sibNumber?.trim().orEmpty()
        if (cleanSib.isNotEmpty()) {
            builder.appendQueryParameter("sib", cleanSib)
        }
        if (cleanMemberId.isEmpty()) {
            val cleanEmail = email?.trim()?.lowercase().orEmpty()
            if (cleanEmail.isNotEmpty()) {
                builder.appendQueryParameter("email", cleanEmail)
            }
            val cleanUid = uid?.trim().orEmpty()
            if (cleanUid.isNotEmpty()) {
                builder.appendQueryParameter("uid", cleanUid)
            }
        }
        return builder.build().toString()
    }

    /**
     * Parses a scanned QR string into a [ParsedMemberQr] if it is a valid
     * SIB Toburon Member Card QR payload, or returns null for unrecognized QR codes.
     */
    fun parseMemberQrPayload(rawValue: String?): ParsedMemberQr? {
        val trimmed = rawValue?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val uri = runCatching { trimmed.toUri() }.getOrNull() ?: return null
        if (!uri.scheme.equals(QR_SCHEME, ignoreCase = true)) return null
        if (!uri.host.equals(QR_HOST, ignoreCase = true)) return null

        val memberId = uri.getQueryParameter("id")?.trim()?.takeIf { it.isNotEmpty() }
        val email = uri.getQueryParameter("email")?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val uid = uri.getQueryParameter("uid")?.trim()?.takeIf { it.isNotEmpty() }

        if (memberId == null && email == null && uid == null) return null
        return ParsedMemberQr(memberId = memberId, email = email, uid = uid)
    }

    /**
     * Generates a crisp, high-contrast QR Code [ImageBitmap] using ZXing Core.
     */
    fun generateQrCodeBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = Color.rgb(16, 46, 38),
        backgroundColor: Int = Color.WHITE
    ): ImageBitmap? = generateQrBitmapRaw(
        content = content,
        sizePx = sizePx,
        foregroundColor = foregroundColor,
        backgroundColor = backgroundColor
    )?.asImageBitmap()

    /**
     * Generates a raw Android [Bitmap] QR Code suitable for both Compose display and PNG sharing.
     */
    fun generateQrBitmapRaw(
        content: String,
        sizePx: Int = 720,
        foregroundColor: Int = Color.rgb(16, 46, 38),
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank() || sizePx <= 0) return null
        return runCatching {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 2
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val width = matrix.width
            val height = matrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (matrix[x, y]) foregroundColor else backgroundColor
                }
            }
            val bitmap = createBitmap(width, height)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        }.getOrNull()
    }

    /**
     * Generates a cryptographically random 24-character URL-safe access token for web registration links.
     */
    fun generateRegistrationAccessToken(length: Int = 24): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        val random = java.security.SecureRandom()
        return buildString(length) {
            repeat(length) {
                append(alphabet[random.nextInt(alphabet.length)])
            }
        }
    }
}
