package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

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

