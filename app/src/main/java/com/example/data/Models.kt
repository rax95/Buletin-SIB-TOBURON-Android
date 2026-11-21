package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class NewsCategory(val code: String, val label: String) {
    WARTA("WARTA", "Warta Jemaat"),
    PENGUMUMAN("PENGUMUMAN", "Pengumuman"),
    RENUNGAN("RENUNGAN", "Renungan"),
    PELAYANAN("PELAYANAN", "Pelayanan"),
    PEMUDA("PEMUDA", "Pemuda");

    companion object {
        fun fromCode(raw: String?): NewsCategory =
            entries.firstOrNull { it.code == raw } ?: WARTA
    }
}

enum class ScheduleCategory(val code: String, val label: String) {
    IBADAH("IBADAH", "Ibadah Raya"),
    DOA("DOA", "Persekutuan Doa"),
    PEMUDA("PEMUDA", "Pemuda & Remaja"),
    SEKOLAH_MINGGU("SEKOLAH_MINGGU", "Sekolah Minggu"),
    PERSEKUTUAN("PERSEKUTUAN", "Persekutuan"),
    RAPAT("RAPAT", "Rapat Pelayanan");

    companion object {
        fun fromCode(raw: String?): ScheduleCategory =
            entries.firstOrNull { it.code == raw } ?: IBADAH
    }
}

enum class GalleryCategory(val code: String, val label: String) {
    IBADAH("IBADAH", "Ibadah"),
    PEMUDA("PEMUDA", "Pemuda"),
    SEKOLAH_MINGGU("SEKOLAH_MINGGU", "Sekolah Minggu"),
    PELAYANAN("PELAYANAN", "Pelayanan"),
    UMUM("UMUM", "Kebersamaan");

    companion object {
        fun fromCode(raw: String?): GalleryCategory =
            entries.firstOrNull { it.code == raw } ?: UMUM
    }
}

enum class CoverPreset(val code: String, val label: String) {
    SANCTUARY("SANCTUARY", "Gedung SIB Toburon"),
    WORSHIP("WORSHIP", "Pujian & Penyembahan"),
    FELLOWSHIP("FELLOWSHIP", "Persekutuan Jemaat"),
    CUSTOM("CUSTOM", "Foto Kustom");

    companion object {
        fun fromCode(raw: String?): CoverPreset =
            entries.firstOrNull { it.code == raw } ?: SANCTUARY
    }
}

enum class FellowshipGroup(val code: String, val label: String) {
    UMUM("UMUM", "Jemaat Umum"),
    PEMUDA("PEMUDA", "Pemuda & Remaja (PPR)"),
    KAUM_IBU("KAUM_IBU", "Persekutuan Wanita (PW)"),
    KAUM_BAPAK("KAUM_BAPAK", "Persekutuan Pria (PP)"),
    SEKOLAH_MINGGU("SEKOLAH_MINGGU", "Sekolah Minggu"),
    PELAYAN("PELAYAN", "Pelayan & Majelis");

    companion object {
        fun fromCode(raw: String?): FellowshipGroup =
            entries.firstOrNull { it.code == raw } ?: UMUM
    }
}

@Keep
@IgnoreExtraProperties
data class NewsArticle(
    @DocumentId val id: String = "",
    val title: String = "",
    val summary: String = "",
    val content: String = "",
    val category: String = NewsCategory.WARTA.code,
    val authorId: String = "",
    val authorName: String = "",
    val coverPreset: String = CoverPreset.SANCTUARY.code,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val pinned: Boolean = false,
    val published: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCategory(): NewsCategory = NewsCategory.fromCode(category)
    fun resolvedCoverPreset(): CoverPreset = CoverPreset.fromCode(coverPreset)
}

@Keep
@IgnoreExtraProperties
data class ChurchSchedule(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = ScheduleCategory.IBADAH.code,
    val dateLabel: String = "",
    val timeLabel: String = "",
    val location: String = "",
    val speaker: String = "",
    val sortOrder: Long = 0L,
    val recurring: Boolean = false,
    val published: Boolean = true,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCategory(): ScheduleCategory = ScheduleCategory.fromCode(category)
}

@Keep
@IgnoreExtraProperties
data class GalleryPhoto(
    @DocumentId val id: String = "",
    val title: String = "",
    val caption: String = "",
    val category: String = GalleryCategory.UMUM.code,
    val eventDate: String = "",
    val coverPreset: String = CoverPreset.FELLOWSHIP.code,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val likesCount: Long = 0L,
    val published: Boolean = true,
    val authorId: String = "",
    val photographerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCategory(): GalleryCategory = GalleryCategory.fromCode(category)
    fun resolvedCoverPreset(): CoverPreset = CoverPreset.fromCode(coverPreset)
}

@Keep
@IgnoreExtraProperties
data class AdminMember(
    @DocumentId val id: String = "",
    val displayName: String = "",
    val roleTitle: String = "",
    val addedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val displayName: String = "",
    val fellowshipGroup: String = FellowshipGroup.UMUM.code,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedGroup(): FellowshipGroup = FellowshipGroup.fromCode(fellowshipGroup)
}

@Keep
@IgnoreExtraProperties
data class UserBookmark(
    @DocumentId val id: String = "",
    val itemId: String = "",
    val itemType: String = "NEWS",
    val title: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PhotoLike(
    @DocumentId val id: String = "",
    val photoId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationEvent(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val eventType: String = "NEWS",
    val targetId: String = "",
    val topic: String = "sib_toburon_news",
    val published: Boolean = true,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class BulletinAlert(
    val eventId: String,
    val type: String,
    val itemId: String,
    val title: String,
    val body: String,
    val authorId: String
)

enum class MemberGender(val code: String, val label: String) {
    LELAKI("LELAKI", "Lelaki"),
    PEREMPUAN("PEREMPUAN", "Perempuan");

    companion object {
        fun fromCode(raw: String?): MemberGender =
            entries.firstOrNull { it.code == raw } ?: LELAKI
    }
}

enum class BaptismStatus(val code: String, val label: String) {
    DIBAPTIS("DIBAPTIS", "Dibaptis"),
    BELUM_DIBAPTIS("BELUM_DIBAPTIS", "Belum Dibaptis");

    companion object {
        fun fromCode(raw: String?): BaptismStatus =
            entries.firstOrNull { it.code == raw } ?: BELUM_DIBAPTIS
    }
}

@Keep
@IgnoreExtraProperties
data class ChurchMemberPublic(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val gender: String = "",
    val baptismStatus: String = "",
    val published: Boolean = true,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedGenderLabel(): String =
        MemberGender.entries.firstOrNull { it.code == gender }?.label ?: "Belum dikemas kini"

    fun resolvedBaptismLabel(): String =
        BaptismStatus.entries.firstOrNull { it.code == baptismStatus }?.label ?: "Belum dikemas kini"
}

@Keep
@IgnoreExtraProperties
data class ChurchMemberDetail(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val gender: String = MemberGender.LELAKI.code,
    val birthDate: String = "",
    val baptismStatus: String = BaptismStatus.BELUM_DIBAPTIS.code,
    val denomination: String = "SIB",
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedGender(): MemberGender = MemberGender.fromCode(gender)
    fun resolvedBaptismStatus(): BaptismStatus = BaptismStatus.fromCode(baptismStatus)
    fun calculatedAge(): Int? = calculateAgeFromIsoDate(birthDate)
}

/**
 * Calculates age in completed years from an ISO date string ("YYYY-MM-DD").
 * Returns null if the string is invalid or in the future.
 */
fun calculateAgeFromIsoDate(isoDate: String): Int? {
    val parts = isoDate.trim().split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (year < 1900 || month !in 1..12 || day !in 1..31) return null

    val today = Calendar.getInstance()
    val birth = Calendar.getInstance().apply {
        isLenient = false
        try {
            set(year, month - 1, day, 0, 0, 0)
            time
        } catch (_: Exception) {
            return null
        }
    }
    if (birth.after(today)) return null

    var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val birthMonth = birth.get(Calendar.MONTH)
    if (todayMonth < birthMonth ||
        (todayMonth == birthMonth && today.get(Calendar.DAY_OF_MONTH) < birth.get(Calendar.DAY_OF_MONTH))
    ) {
        age -= 1
    }
    return age.coerceAtLeast(0)
}

/**
 * Formats an ISO date ("YYYY-MM-DD") into a readable date string (e.g. "14 Jun 1995").
 */
fun formatBirthDateReadable(isoDate: String): String {
    val parts = isoDate.trim().split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0].toIntOrNull() ?: return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    if (month !in 1..12 || day !in 1..31) return isoDate
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(year, month - 1, day, 0, 0, 0)
    }
    val fmt = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(cal.time)
}

fun epochMillisToIsoDate(utcMillis: Long): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    return String.format(
        Locale.US,
        "%04d-%02d-%02d",
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

fun isoDateToEpochMillis(isoDate: String): Long? {
    val parts = isoDate.trim().split("-")
    if (parts.size != 3) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (year < 1900 || month !in 1..12 || day !in 1..31) return null
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(year, month - 1, day, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}


