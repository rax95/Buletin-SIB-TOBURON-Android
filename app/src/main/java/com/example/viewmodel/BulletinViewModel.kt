package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminMember
import com.example.data.BulletinAlert
import com.example.data.BulletinRepository
import com.example.data.ChurchSchedule
import com.example.data.CoverPreset
import com.example.data.FellowshipGroup
import com.example.data.GalleryCategory
import com.example.data.GalleryPhoto
import com.example.data.NewsArticle
import com.example.data.NewsCategory
import com.example.data.ScheduleCategory
import com.example.data.UiState
import com.example.data.UserBookmark
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BulletinViewModel(
    private val repository: BulletinRepository,
    val currentUserId: String?,
    val userEmail: String?,
    val userDisplayName: String?,
    val emailVerified: Boolean
) : ViewModel() {

    val isGuest: Boolean = currentUserId.isNullOrBlank()

    val isBootstrappedAdmin: Boolean =
        !isGuest && emailVerified && userEmail?.equals(BulletinRepository.BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _liveAlerts = MutableSharedFlow<BulletinAlert>(extraBufferCapacity = 8)
    val liveAlerts: SharedFlow<BulletinAlert> = _liveAlerts.asSharedFlow()

    private val seenNotificationEventIds = mutableSetOf<String>()
    private var notificationStreamHydrated = false

    init {
        viewModelScope.launch {
            repository.observeNotificationEvents(currentUserId)
                .catch { error ->
                    unwrapOrRethrow("notification_events", error)
                }
                .collect { events ->
                    if (!notificationStreamHydrated) {
                        seenNotificationEventIds.addAll(events.map { it.id })
                        notificationStreamHydrated = true
                    } else {
                        val newlyPublished = events.filter { it.id.isNotBlank() && seenNotificationEventIds.add(it.id) }
                        newlyPublished.forEach { event ->
                            // Skip pinging the publishing admin's own active device
                            if (currentUserId == null || event.authorId != currentUserId) {
                                _liveAlerts.tryEmit(
                                    BulletinAlert(
                                        eventId = event.id,
                                        type = event.eventType,
                                        itemId = event.targetId,
                                        title = event.title,
                                        body = event.body,
                                        authorId = event.authorId
                                    )
                                )
                            }
                        }
                    }
                }
        }
    }

    // Search & filter UI states
    private val _newsSearchQuery = MutableStateFlow("")
    val newsSearchQuery: StateFlow<String> = _newsSearchQuery.asStateFlow()

    private val _selectedNewsCategory = MutableStateFlow<NewsCategory?>(null)
    val selectedNewsCategory: StateFlow<NewsCategory?> = _selectedNewsCategory.asStateFlow()

    private val _scheduleSearchQuery = MutableStateFlow("")
    val scheduleSearchQuery: StateFlow<String> = _scheduleSearchQuery.asStateFlow()

    private val _selectedScheduleCategory = MutableStateFlow<ScheduleCategory?>(null)
    val selectedScheduleCategory: StateFlow<ScheduleCategory?> = _selectedScheduleCategory.asStateFlow()

    private val _selectedGalleryCategory = MutableStateFlow<GalleryCategory?>(null)
    val selectedGalleryCategory: StateFlow<GalleryCategory?> = _selectedGalleryCategory.asStateFlow()

    // Live admin privilege stream
    val isAdmin: StateFlow<Boolean> = repository
        .observeAdminAccess(currentUserId, userEmail, emailVerified)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = isBootstrappedAdmin
        )

    // Real-time News stream with two-tier Flow error unwrapping
    val newsState: StateFlow<UiState<List<NewsArticle>>> = isAdmin
        .flatMapLatest { admin ->
            repository.observeNews(currentUserId, admin, isBootstrappedAdmin)
                .map<List<NewsArticle>, UiState<List<NewsArticle>>> { UiState.Success(it) }
                .catch { error ->
                    emit(toErrorState("Gagal memuat berita terkini", error))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Real-time Schedules stream
    val schedulesState: StateFlow<UiState<List<ChurchSchedule>>> = isAdmin
        .flatMapLatest { admin ->
            repository.observeSchedules(currentUserId, admin, isBootstrappedAdmin)
                .map<List<ChurchSchedule>, UiState<List<ChurchSchedule>>> { UiState.Success(it) }
                .catch { error ->
                    emit(toErrorState("Gagal memuat jadwal kegiatan", error))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Real-time Gallery stream
    val galleryState: StateFlow<UiState<List<GalleryPhoto>>> = isAdmin
        .flatMapLatest { admin ->
            repository.observeGallery(currentUserId, admin, isBootstrappedAdmin)
                .map<List<GalleryPhoto>, UiState<List<GalleryPhoto>>> { UiState.Success(it) }
                .catch { error ->
                    emit(toErrorState("Gagal memuat galeri foto", error))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // User bookmarks stream
    val bookmarks: StateFlow<List<UserBookmark>> = repository
        .observeUserBookmarks(currentUserId)
        .catch { error ->
            unwrapOrRethrow("bookmarks", error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    // User liked photo IDs stream
    val likedPhotoIds: StateFlow<Set<String>> = repository
        .observeLikedPhotoIds(currentUserId)
        .catch { error ->
            unwrapOrRethrow("liked_photos", error)
            emit(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet()
        )

    // User private profile stream
    val userProfile: StateFlow<UserProfile?> = repository
        .observeUserProfile(currentUserId)
        .catch { error ->
            unwrapOrRethrow("user_profile", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    // Delegated admins stream (only queried when user is admin)
    val adminMembers: StateFlow<List<AdminMember>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeAdminMembers(currentUserId, isBootstrappedAdmin)
                .catch { error ->
                    unwrapOrRethrow("admins", error)
                    emit(emptyList())
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    // --- FILTER ACTIONS ---

    fun setNewsSearchQuery(query: String) {
        _newsSearchQuery.value = query
    }

    fun selectNewsCategory(category: NewsCategory?) {
        _selectedNewsCategory.value = category
    }

    fun setScheduleSearchQuery(query: String) {
        _scheduleSearchQuery.value = query
    }

    fun selectScheduleCategory(category: ScheduleCategory?) {
        _selectedScheduleCategory.value = category
    }

    fun selectGalleryCategory(category: GalleryCategory?) {
        _selectedGalleryCategory.value = category
    }

    // --- MUTATIONS ---

    fun saveNews(
        existingId: String?,
        title: String,
        summary: String,
        content: String,
        category: NewsCategory,
        authorName: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        pinned: Boolean,
        published: Boolean,
        onComplete: () -> Unit = {}
    ) {
        val previouslyDraft = existingId != null &&
            (newsState.value as? UiState.Success)?.data
                ?.firstOrNull { it.id == existingId }?.published == false
        val willNotify = published && (existingId == null || previouslyDraft)
        val message = when {
            willNotify -> "Berita dipublikasikan & notifikasi dikirim ke jemaat."
            existingId == null -> "Draf berita berhasil disimpan."
            else -> "Berita berhasil diperbarui."
        }
        runMutation(
            successMessage = message,
            onComplete = onComplete
        ) {
            if (existingId == null) {
                repository.createNews(
                    title, summary, content, category, authorName,
                    coverPreset, imageUrl, imageBase64, pinned, published
                )
            } else {
                repository.updateNews(
                    existingId, title, summary, content, category, authorName,
                    coverPreset, imageUrl, imageBase64, pinned, published,
                    notifyOnPublish = previouslyDraft && published
                )
            }
        }
    }

    fun deleteNews(newsId: String) {
        runMutation("Berita telah dihapus.") {
            repository.deleteNews(newsId)
        }
    }

    fun saveSchedule(
        existingId: String?,
        title: String,
        description: String,
        category: ScheduleCategory,
        dateLabel: String,
        timeLabel: String,
        location: String,
        speaker: String,
        sortOrder: Long,
        recurring: Boolean,
        published: Boolean,
        onComplete: () -> Unit = {}
    ) {
        val previouslyDraft = existingId != null &&
            (schedulesState.value as? UiState.Success)?.data
                ?.firstOrNull { it.id == existingId }?.published == false
        val willNotify = published && (existingId == null || previouslyDraft)
        val message = when {
            willNotify -> "Jadwal dipublikasikan & notifikasi dikirim ke jemaat."
            existingId == null -> "Draf jadwal kegiatan berhasil disimpan."
            else -> "Jadwal kegiatan berhasil diperbarui."
        }
        runMutation(
            successMessage = message,
            onComplete = onComplete
        ) {
            if (existingId == null) {
                repository.createSchedule(
                    title, description, category, dateLabel, timeLabel,
                    location, speaker, sortOrder, recurring, published
                )
            } else {
                repository.updateSchedule(
                    existingId, title, description, category, dateLabel, timeLabel,
                    location, speaker, sortOrder, recurring, published,
                    notifyOnPublish = previouslyDraft && published
                )
            }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        runMutation("Jadwal kegiatan telah dihapus.") {
            repository.deleteSchedule(scheduleId)
        }
    }

    fun saveGalleryPhoto(
        existingId: String?,
        title: String,
        caption: String,
        category: GalleryCategory,
        eventDate: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        photographerName: String,
        published: Boolean,
        onComplete: () -> Unit = {}
    ) {
        runMutation(
            successMessage = if (existingId == null) "Foto berhasil ditambahkan ke galeri real-time." else "Info foto berhasil diperbarui.",
            onComplete = onComplete
        ) {
            if (existingId == null) {
                repository.createGalleryPhoto(
                    title, caption, category, eventDate, coverPreset,
                    imageUrl, imageBase64, photographerName, published
                )
            } else {
                repository.updateGalleryPhoto(
                    existingId, title, caption, category, eventDate, coverPreset,
                    imageUrl, imageBase64, photographerName, published
                )
            }
        }
    }

    fun deleteGalleryPhoto(photoId: String) {
        runMutation("Foto telah dihapus dari galeri.") {
            repository.deleteGalleryPhoto(photoId)
        }
    }

    fun togglePhotoLike(photoId: String) {
        if (isGuest) {
            _snackbarMessage.tryEmit("Masuk dengan akun Google di tab Akun untuk memberi apresiasi foto.")
            return
        }
        val alreadyLiked = likedPhotoIds.value.contains(photoId)
        viewModelScope.launch {
            val result = repository.togglePhotoLike(photoId, alreadyLiked)
            result.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Gagal memperbarui apresiasi foto.")
            }
        }
    }

    fun toggleBookmark(itemId: String, itemType: String, title: String) {
        if (isGuest) {
            _snackbarMessage.tryEmit("Masuk dengan akun Google di tab Akun untuk menyimpan warta & jadwal.")
            return
        }
        val alreadyBookmarked = bookmarks.value.any { it.itemId == itemId && it.itemType == itemType }
        viewModelScope.launch {
            val result = repository.toggleBookmark(itemId, itemType, title, alreadyBookmarked)
            result.onSuccess {
                _snackbarMessage.tryEmit(
                    if (alreadyBookmarked) "Dihapus dari daftar tersimpan."
                    else "Disimpan ke daftar pribadi Anda."
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit(err.localizedMessage ?: "Gagal memperbarui simpanan.")
            }
        }
    }

    fun saveProfile(displayName: String, fellowshipGroup: FellowshipGroup) {
        runMutation("Profil jemaat berhasil disimpan.") {
            repository.saveUserProfile(displayName, fellowshipGroup, userProfile.value)
        }
    }

    fun addAdminMember(targetUid: String, displayName: String, roleTitle: String, onComplete: () -> Unit = {}) {
        runMutation("Pengurus admin berhasil ditambahkan.", onComplete) {
            repository.addAdminMember(targetUid, displayName, roleTitle)
        }
    }

    fun removeAdminMember(targetUid: String) {
        runMutation("Akses admin pengurus telah dicabut.") {
            repository.removeAdminMember(targetUid)
        }
    }

    fun seedStarterData() {
        runMutation("Konten awal Buletin SIB Toburon berhasil dimuat dan disinkronkan!") {
            repository.seedStarterBulletinData(userDisplayName ?: "Sekretariat SIB Toburon")
        }
    }

    fun syncFcmToken(token: String, newsEnabled: Boolean, schedulesEnabled: Boolean) {
        if (isGuest || token.isBlank()) return
        viewModelScope.launch {
            repository.saveFcmToken(token, newsEnabled, schedulesEnabled)
                .onFailure { err ->
                    Log.w(TAG, "Sinkronisasi token FCM gagal: ${err.message}")
                }
        }
    }

    private fun runMutation(
        successMessage: String,
        onComplete: () -> Unit = {},
        block: suspend () -> Result<Unit>
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = block()
                result.onSuccess {
                    _snackbarMessage.tryEmit(successMessage)
                    onComplete()
                }.onFailure { err ->
                    _snackbarMessage.tryEmit(err.localizedMessage ?: "Terjadi kesalahan saat menyimpan data.")
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    private fun toErrorState(fallback: String, error: Throwable): UiState.Error {
        val rootError = unwrapOrRethrow(fallback, error)
        return UiState.Error(rootError.localizedMessage ?: fallback)
    }

    private fun unwrapOrRethrow(contextLabel: String, error: Throwable): Throwable {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Stream error ($contextLabel): ${rootError.message}", rootError)
        return rootError
    }

    companion object {
        private const val TAG = "BulletinVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
