package com.example.data

import androidx.annotation.Keep
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppLanguageManager
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

@Keep
@IgnoreExtraProperties
data class DynamicCategoryItem(
    @DocumentId val id: String = "",
    val code: String = "",
    val labelMs: String = "",
    val labelEn: String = "",
    val sortOrder: Int = 0,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCode(): String = code.ifBlank { id }

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) {
            labelEn.ifBlank { labelMs }
        } else {
            labelMs.ifBlank { labelEn }
        }
}

fun generateUniqueCategoryCode(rawLabel: String, existingCodes: Set<String>): String {
    val sanitized = rawLabel
        .trim()
        .uppercase(Locale.US)
        .replace(Regex("[^A-Z0-9]+"), "_")
        .trim('_')
        .take(34)
        .ifBlank { "KATEGORI" }
    val normalizedExisting = existingCodes.map { it.uppercase(Locale.US) }.toSet()
    if (sanitized !in normalizedExisting) return sanitized
    var counter = 2
    while (counter <= 999) {
        val candidate = "${sanitized.take(32)}_$counter"
        if (candidate !in normalizedExisting) return candidate
        counter++
    }
    return "KAT_${System.currentTimeMillis().toString().takeLast(6)}"
}

fun resolveNewsCategoryLabel(
    code: String,
    categories: List<DynamicCategoryItem>,
    lang: AppLanguage = AppLanguageManager.current()
): String {
    val dynamicMatch = categories.firstOrNull { it.resolvedCode().equals(code, ignoreCase = true) }
    if (dynamicMatch != null) return dynamicMatch.localizedLabel(lang)
    val enumMatch = NewsCategory.entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    if (enumMatch != null) return enumMatch.localizedLabel(lang)
    return code.replace('_', ' ').trim().ifBlank { NewsCategory.WARTA.localizedLabel(lang) }
}

fun resolveScheduleCategoryLabel(
    code: String,
    categories: List<DynamicCategoryItem>,
    lang: AppLanguage = AppLanguageManager.current()
): String {
    val dynamicMatch = categories.firstOrNull { it.resolvedCode().equals(code, ignoreCase = true) }
    if (dynamicMatch != null) return dynamicMatch.localizedLabel(lang)
    val enumMatch = ScheduleCategory.entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    if (enumMatch != null) return enumMatch.localizedLabel(lang)
    return code.replace('_', ' ').trim().ifBlank { ScheduleCategory.IBADAH.localizedLabel(lang) }
}

enum class NewsCategory(val code: String, val label: String, val labelEn: String) {
    WARTA("WARTA", "Warta Jemaat", "Church Bulletin"),
    PENGUMUMAN("PENGUMUMAN", "Pengumuman", "Announcements"),
    RENUNGAN("RENUNGAN", "Renungan", "Devotional"),
    PELAYANAN("PELAYANAN", "Pelayanan", "Ministry"),
    PEMUDA("PEMUDA", "Belia & Pemuda", "Youth");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

    companion object {
        fun fromCode(raw: String?): NewsCategory =
            entries.firstOrNull { it.code == raw } ?: WARTA

        fun defaultCategoryItems(): List<DynamicCategoryItem> =
            entries.mapIndexed { index, item ->
                DynamicCategoryItem(
                    id = item.code,
                    code = item.code,
                    labelMs = item.label,
                    labelEn = item.labelEn,
                    sortOrder = index
                )
            }
    }
}

enum class ScheduleCategory(val code: String, val label: String, val labelEn: String) {
    IBADAH("IBADAH", "Ibadah Raya", "Sunday Worship"),
    DOA("DOA", "Persekutuan Doa", "Prayer Fellowship"),
    PEMUDA("PEMUDA", "Belia & Remaja", "Youth & Teens"),
    SEKOLAH_MINGGU("SEKOLAH_MINGGU", "Sekolah Minggu", "Sunday School"),
    PERSEKUTUAN("PERSEKUTUAN", "Persekutuan", "Fellowship"),
    RAPAT("RAPAT", "Mesyuarat Pelayanan", "Ministry Meeting");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

    companion object {
        fun fromCode(raw: String?): ScheduleCategory =
            entries.firstOrNull { it.code == raw } ?: IBADAH

        fun defaultCategoryItems(): List<DynamicCategoryItem> =
            entries.mapIndexed { index, item ->
                DynamicCategoryItem(
                    id = item.code,
                    code = item.code,
                    labelMs = item.label,
                    labelEn = item.labelEn,
                    sortOrder = index
                )
            }
    }
}

enum class GalleryCategory(val code: String, val label: String, val labelEn: String) {
    IBADAH("IBADAH", "Ibadah", "Worship"),
    PEMUDA("PEMUDA", "Belia", "Youth"),
    SEKOLAH_MINGGU("SEKOLAH_MINGGU", "Sekolah Minggu", "Sunday School"),
    PELAYANAN("PELAYANAN", "Pelayanan", "Ministry"),
    UMUM("UMUM", "Kebersamaan", "Fellowship");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

    companion object {
        fun fromCode(raw: String?): GalleryCategory =
            entries.firstOrNull { it.code == raw } ?: UMUM
    }
}

enum class CoverPreset(val code: String, val label: String, val labelEn: String) {
    SANCTUARY("SANCTUARY", "Bangunan SIB Toburon", "SIB Toburon Sanctuary"),
    WORSHIP("WORSHIP", "Pujian & Penyembahan", "Praise & Worship"),
    FELLOWSHIP("FELLOWSHIP", "Persekutuan Jemaat", "Congregation Fellowship"),
    CUSTOM("CUSTOM", "Foto Tersuai", "Custom Photo");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

    companion object {
        fun fromCode(raw: String?): CoverPreset =
            entries.firstOrNull { it.code == raw } ?: SANCTUARY
    }
}

enum class FellowshipGroup(val code: String, val label: String, val labelEn: String) {
    UMUM("UMUM", "Jemaat Umum", "General Congregation"),
    PEMUDA("PEMUDA", "Belia & Remaja (PMM)", "Youth & Teens"),
    KAUM_IBU("KAUM_IBU", "Persekutuan Wanita (PW)", "Women's Fellowship"),
    KAUM_BAPAK("KAUM_BAPAK", "Persekutuan Kaum Bapa (PKB)", "Men's Fellowship"),
    SEKOLAH_MINGGU("SEKOLAH_MINGGU", "Sekolah Minggu", "Sunday School"),
    PELAYAN("PELAYAN", "Pelayan & Majlis", "Ministers & Elders");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

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
data class NewsComment(
    @DocumentId val id: String = "",
    val newsId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val parentCommentId: String? = null,
    val replyToAuthorName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

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
    val featuredInNews: Boolean = false,
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
    val extraImagesBase64: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val published: Boolean = true,
    val authorId: String = "",
    val photographerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCategory(): GalleryCategory = GalleryCategory.fromCode(category)
    fun resolvedCoverPreset(): CoverPreset = CoverPreset.fromCode(coverPreset)

    fun allBase64Images(): List<String> = buildList {
        imageBase64?.takeIf { it.isNotBlank() }?.let { add(it) }
        extraImagesBase64.filter { it.isNotBlank() }.forEach { add(it) }
    }

    fun totalImageCount(): Int {
        val count = allBase64Images().size
        return if (count > 0) count else 1
    }
}

@Keep
@IgnoreExtraProperties
data class GalleryComment(
    @DocumentId val id: String = "",
    val photoId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    val parentCommentId: String? = null,
    val replyToAuthorName: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

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
    val email: String = "",
    val fellowshipGroup: String = FellowshipGroup.UMUM.code,
    val photoBase64: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val lastLoginAt: Timestamp? = null
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

@Keep
@IgnoreExtraProperties
data class AppVersionConfig(
    @DocumentId val id: String = "",
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val releaseNotes: String = "",
    val updateUrl: String = "",
    val forceUpdate: Boolean = false,
    val updatedBy: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isUpdateAvailable(currentVersionCode: Int): Boolean =
        latestVersionCode > currentVersionCode
}

enum class MemberGender(val code: String, val label: String, val labelEn: String) {
    LELAKI("LELAKI", "Lelaki", "Male"),
    PEREMPUAN("PEREMPUAN", "Perempuan", "Female");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

    companion object {
        fun fromCode(raw: String?): MemberGender =
            entries.firstOrNull { it.code == raw } ?: LELAKI
    }
}

enum class BaptismStatus(val code: String, val label: String, val labelEn: String) {
    DIBAPTIS("DIBAPTIS", "Sudah Dibaptis", "Baptized"),
    BELUM_DIBAPTIS("BELUM_DIBAPTIS", "Belum Dibaptis", "Not Yet Baptized");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

    companion object {
        fun fromCode(raw: String?): BaptismStatus =
            entries.firstOrNull { it.code == raw } ?: BELUM_DIBAPTIS
    }
}

enum class MaritalStatus(val code: String, val label: String, val labelEn: String) {
    BUJANG("BUJANG", "Bujang", "Single"),
    BERKAHWIN("BERKAHWIN", "Berkahwin", "Married"),
    BALU_DUDA("BALU_DUDA", "Balu / Duda", "Widowed"),
    BERCERAI("BERCERAI", "Bercerai", "Divorced");

    fun localizedLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        if (lang == AppLanguage.EN) labelEn else label

    companion object {
        fun fromCode(raw: String?): MaritalStatus =
            entries.firstOrNull { it.code == raw } ?: BUJANG
    }
}

@Keep
@IgnoreExtraProperties
data class ChurchMemberPublic(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val gender: String = "",
    val baptismStatus: String = "",
    val passportPhotoBase64: String? = null,
    val published: Boolean = true,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedGenderLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        MemberGender.entries.firstOrNull { it.code == gender }?.localizedLabel(lang)
            ?: if (lang == AppLanguage.EN) "Not yet updated" else "Belum dikemas kini"

    fun resolvedBaptismLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        BaptismStatus.entries.firstOrNull { it.code == baptismStatus }?.localizedLabel(lang)
            ?: if (lang == AppLanguage.EN) "Not yet updated" else "Belum dikemas kini"
}

@Keep
@IgnoreExtraProperties
data class ChurchMemberDetail(
    @DocumentId val id: String = "",
    val fullName: String = "",
    val myKadNumber: String? = null,
    val gender: String = MemberGender.LELAKI.code,
    val birthDate: String = "",
    val baptismStatus: String = BaptismStatus.BELUM_DIBAPTIS.code,
    val denomination: String = "",
    val maritalStatus: String = "",
    val spouseName: String? = null,
    val sibMembershipNumber: String? = null,
    val passportPhotoBase64: String? = null,
    val memberEmail: String? = null,
    val authorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedGender(): MemberGender = MemberGender.fromCode(gender)
    fun resolvedBaptismStatus(): BaptismStatus = BaptismStatus.fromCode(baptismStatus)
    fun resolvedMaritalStatus(): MaritalStatus = MaritalStatus.fromCode(maritalStatus)
    fun resolvedMaritalStatusLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        MaritalStatus.entries.firstOrNull { it.code == maritalStatus }?.localizedLabel(lang)
            ?: if (lang == AppLanguage.EN) "Not yet updated" else "Belum dikemas kini"

    fun resolvedSibMembershipLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        sibMembershipNumber?.trim()?.takeIf { it.isNotEmpty() }
            ?: if (lang == AppLanguage.EN) "Not yet assigned" else "Belum ada"

    fun resolvedSpouseFieldLabel(lang: AppLanguage = AppLanguageManager.current()): String =
        when (resolvedGender()) {
            MemberGender.PEREMPUAN -> if (lang == AppLanguage.EN) "Husband's Name" else "Nama Suami"
            MemberGender.LELAKI -> if (lang == AppLanguage.EN) "Wife's Name" else "Nama Isteri"
        }

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
fun formatBirthDateReadable(isoDate: String, lang: AppLanguage = AppLanguageManager.current()): String {
    val parts = isoDate.trim().split("-")
    if (parts.size != 3) return isoDate
    val year = parts[0].toIntOrNull() ?: return isoDate
    val month = parts[1].toIntOrNull() ?: return isoDate
    val day = parts[2].toIntOrNull() ?: return isoDate
    if (month !in 1..12 || day !in 1..31) return isoDate
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(year, month - 1, day, 0, 0, 0)
    }
    val fmt = SimpleDateFormat("dd MMM yyyy", lang.locale).apply {
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


