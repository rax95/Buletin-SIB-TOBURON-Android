package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminMember
import com.example.data.AppVersionConfig
import com.example.data.BaptismStatus
import com.example.data.BulletinAlert
import com.example.data.BulletinRepository
import com.example.data.ChurchMemberDetail
import com.example.data.ChurchMemberPublic
import com.example.data.ChurchSchedule
import com.example.data.CoverPreset
import com.example.data.FellowshipGroup
import com.example.data.GalleryCategory
import com.example.data.GalleryComment
import com.example.data.GalleryPhoto
import com.example.data.MaritalStatus
import com.example.data.MemberGender
import com.example.data.NewsArticle
import com.example.data.NewsCategory
import com.example.data.NewsComment
import com.example.data.ScheduleCategory
import com.example.data.UiState
import com.example.data.UserBookmark
import com.example.data.UserProfile
import com.example.notifications.BulletinNotificationManager
import com.example.ui.i18n.AppLanguageManager
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

    private val _unreadNewMemberCount = MutableStateFlow(0)
    val unreadNewMemberCount: StateFlow<Int> = _unreadNewMemberCount.asStateFlow()

    private val seenNotificationEventIds = mutableSetOf<String>()

    // Live admin privilege stream
    val isAdmin: StateFlow<Boolean> = repository
        .observeAdminAccess(currentUserId, userEmail, emailVerified)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = isBootstrappedAdmin
        )

    init {
        if (!isGuest && !currentUserId.isNullOrBlank()) {
            viewModelScope.launch {
                repository.recordSignedInUser(userDisplayName, userEmail)
                    .onFailure { err ->
                        Log.w(TAG, "Rekod log masuk pengguna Google gagal: ${err.message}")
                    }
            }
        }
        viewModelScope.launch {
            isAdmin.flatMapLatest { admin ->
                var hydratedForStream = false
                repository.observeNotificationEvents(currentUserId, admin)
                    .map { events ->
                        val result = if (!hydratedForStream) {
                            seenNotificationEventIds.addAll(events.map { it.id })
                            hydratedForStream = true
                            emptyList()
                        } else {
                            events.filter { it.id.isNotBlank() && seenNotificationEventIds.add(it.id) }
                        }
                        admin to result
                    }
                    .catch { error ->
                        unwrapOrRethrow("notification_events", error)
                    }
            }.collect { (admin, newlyPublished) ->
                newlyPublished.forEach { event ->
                    val isMemberRegistration = event.eventType.equals(
                        BulletinNotificationManager.TYPE_MEMBER_REGISTERED,
                        ignoreCase = true
                    )
                    if (isMemberRegistration) {
                        if (admin) {
                            _unreadNewMemberCount.value = _unreadNewMemberCount.value + 1
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
                    } else if (currentUserId == null || event.authorId != currentUserId) {
                        // Skip pinging the publishing admin's own active device for NEWS/SCHEDULE
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

    private val _memberSearchQuery = MutableStateFlow("")
    val memberSearchQuery: StateFlow<String> = _memberSearchQuery.asStateFlow()

    private val _selectedMemberDetail = MutableStateFlow<ChurchMemberDetail?>(null)
    val selectedMemberDetail: StateFlow<ChurchMemberDetail?> = _selectedMemberDetail.asStateFlow()

    private val _isLoadingMemberDetail = MutableStateFlow(false)
    val isLoadingMemberDetail: StateFlow<Boolean> = _isLoadingMemberDetail.asStateFlow()

    private val _activeGalleryPhotoId = MutableStateFlow<String?>(null)
    private val _isPostingComment = MutableStateFlow(false)
    val isPostingComment: StateFlow<Boolean> = _isPostingComment.asStateFlow()

    private val _activeNewsArticleId = MutableStateFlow<String?>(null)
    private val _isPostingNewsComment = MutableStateFlow(false)
    val isPostingNewsComment: StateFlow<Boolean> = _isPostingNewsComment.asStateFlow()

    // Real-time News stream with two-tier Flow error unwrapping
    val newsState: StateFlow<UiState<List<NewsArticle>>> = isAdmin
        .flatMapLatest { admin ->
            repository.observeNews(currentUserId, admin, isBootstrappedAdmin)
                .map<List<NewsArticle>, UiState<List<NewsArticle>>> { UiState.Success(it) }
                .catch { error ->
                    emit(toErrorState(AppLanguageManager.tr("Gagal memuat berita terkini", "Failed to load latest news"), error))
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
                    emit(toErrorState(AppLanguageManager.tr("Gagal memuat jadual kegiatan", "Failed to load activity schedules"), error))
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
                    emit(toErrorState(AppLanguageManager.tr("Gagal memuat galeri foto", "Failed to load photo gallery"), error))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Real-time App Version Config (/app_config/version) for automatic in-app update notice
    val appVersionConfig: StateFlow<AppVersionConfig?> = repository.observeAppVersionConfig(currentUserId)
        .catch { error ->
            unwrapOrRethrow("appVersionConfig", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    // Real-time Gallery Comments stream for the currently opened gallery post
    val activePhotoComments: StateFlow<UiState<List<GalleryComment>>> = _activeGalleryPhotoId
        .flatMapLatest { photoId ->
            if (photoId.isNullOrBlank()) {
                flowOf<UiState<List<GalleryComment>>>(UiState.Success(emptyList()))
            } else {
                repository.observeGalleryComments(photoId, currentUserId)
                    .map<List<GalleryComment>, UiState<List<GalleryComment>>> { UiState.Success(it) }
                    .catch { error ->
                        emit(
                            toErrorState(
                                AppLanguageManager.tr("Gagal memuat komen foto", "Failed to load photo comments"),
                                error
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // Real-time News Comments stream for the currently opened news article
    val activeNewsComments: StateFlow<UiState<List<NewsComment>>> = _activeNewsArticleId
        .flatMapLatest { newsId ->
            if (newsId.isNullOrBlank()) {
                flowOf<UiState<List<NewsComment>>>(UiState.Success(emptyList()))
            } else {
                repository.observeNewsComments(newsId, currentUserId)
                    .map<List<NewsComment>, UiState<List<NewsComment>>> { UiState.Success(it) }
                    .catch { error ->
                        emit(
                            toErrorState(
                                AppLanguageManager.tr("Gagal memuat komen berita", "Failed to load news comments"),
                                error
                            )
                        )
                    }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // Real-time Church Members directory stream (Admin-only tab)
    val membersState: StateFlow<UiState<List<ChurchMemberPublic>>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf<UiState<List<ChurchMemberPublic>>>(UiState.Success(emptyList()))
            else repository.observeChurchMembers(currentUserId)
                .map<List<ChurchMemberPublic>, UiState<List<ChurchMemberPublic>>> { UiState.Success(it) }
                .catch { error ->
                    emit(toErrorState(AppLanguageManager.tr("Gagal memuat senarai ahli jemaat", "Failed to load congregation members list"), error))
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Signed-in Jemaat's own linked full member record (matched by verified Google email)
    val ownMemberDetail: StateFlow<ChurchMemberDetail?> = repository
        .observeOwnChurchMemberDetail(currentUserId, userEmail)
        .catch { error ->
            unwrapOrRethrow("own_member_detail", error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
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

    // Signed-in Google users stream (strictly admin-only)
    val registeredUsers: StateFlow<List<UserProfile>> = isAdmin
        .flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeRegisteredUsers(currentUserId)
                .catch { error ->
                    unwrapOrRethrow("registered_users", error)
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

    fun setMemberSearchQuery(query: String) {
        _memberSearchQuery.value = query
    }

    fun clearUnreadNewMemberCount() {
        _unreadNewMemberCount.value = 0
    }

    fun loadMemberDetail(memberId: String) {
        if (!isAdmin.value) return
        viewModelScope.launch {
            _isLoadingMemberDetail.value = true
            try {
                val result = repository.getChurchMemberDetail(memberId)
                result.onSuccess { detail ->
                    if (detail != null) {
                        _selectedMemberDetail.value = detail
                    } else {
                        _snackbarMessage.tryEmit(
                            AppLanguageManager.tr(
                                "Maklumat lengkap ahli tidak dijumpai.",
                                "Full member details were not found."
                            )
                        )
                    }
                }.onFailure { err ->
                    _snackbarMessage.tryEmit(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Gagal memuat maklumat ahli.",
                            "Failed to load member details."
                        )
                    )
                }
            } finally {
                _isLoadingMemberDetail.value = false
            }
        }
    }

    fun clearSelectedMemberDetail() {
        _selectedMemberDetail.value = null
    }

    fun prepareMembersForExport(onReady: (List<ChurchMemberDetail>) -> Unit) {
        if (!isAdmin.value) return
        val memberIds = (membersState.value as? UiState.Success)?.data?.map { it.id }.orEmpty()
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = repository.getAllChurchMemberDetails(memberIds, isBootstrappedAdmin)
                result.onSuccess { details ->
                    if (details.isEmpty()) {
                        _snackbarMessage.tryEmit(
                            AppLanguageManager.tr(
                                "Tiada senarai ahli untuk dieksport.",
                                "No members available to export."
                            )
                        )
                    } else {
                        onReady(details)
                    }
                }.onFailure { err ->
                    _snackbarMessage.tryEmit(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Gagal menyediakan data eksport ahli.",
                            "Failed to prepare member export data."
                        )
                    )
                }
            } finally {
                _isBusy.value = false
            }
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.tryEmit(message)
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
            willNotify -> AppLanguageManager.tr(
                "Berita diterbitkan & notifikasi dihantar kepada jemaat.",
                "News published & notification sent to the congregation."
            )
            existingId == null -> AppLanguageManager.tr(
                "Draf berita berjaya disimpan.",
                "News draft saved successfully."
            )
            else -> AppLanguageManager.tr(
                "Berita berjaya dikemas kini.",
                "News updated successfully."
            )
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
        runMutation(AppLanguageManager.tr("Berita telah dipadam.", "News item deleted.")) {
            repository.deleteNews(newsId)
        }
    }

    fun observeCommentsForNews(newsId: String?) {
        _activeNewsArticleId.value = newsId
    }

    fun postNewsComment(
        newsId: String,
        text: String,
        parentCommentId: String? = null,
        replyToAuthorName: String? = null,
        onComplete: () -> Unit = {}
    ) {
        if (isGuest) {
            _snackbarMessage.tryEmit(
                AppLanguageManager.tr(
                    "Log masuk dengan akaun Google di tab Akaun untuk menulis komen atau membalas.",
                    "Sign in with Google in the Account tab to comment or reply."
                )
            )
            return
        }
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        val resolvedAuthor = userProfile.value?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: userDisplayName?.trim()?.takeIf { it.isNotBlank() }
            ?: userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Jemaat SIB Toburon"

        viewModelScope.launch {
            _isPostingNewsComment.value = true
            try {
                val result = repository.addNewsComment(
                    newsId = newsId,
                    authorName = resolvedAuthor,
                    text = cleanText,
                    parentCommentId = parentCommentId,
                    replyToAuthorName = replyToAuthorName
                )
                result.onSuccess {
                    onComplete()
                }.onFailure { err ->
                    _snackbarMessage.tryEmit(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Gagal menghantar komen.",
                            "Failed to post comment."
                        )
                    )
                }
            } finally {
                _isPostingNewsComment.value = false
            }
        }
    }

    fun deleteNewsComment(newsId: String, commentId: String) {
        if (isGuest) return
        viewModelScope.launch {
            val result = repository.deleteNewsComment(newsId, commentId)
            result.onSuccess {
                _snackbarMessage.tryEmit(
                    AppLanguageManager.tr("Komen telah dipadam.", "Comment deleted.")
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit(
                    err.localizedMessage ?: AppLanguageManager.tr(
                        "Gagal memadam komen.",
                        "Failed to delete comment."
                    )
                )
            }
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
        recurring: Boolean,
        published: Boolean,
        onComplete: () -> Unit = {}
    ) {
        val existingSchedule = existingId?.let { id ->
            (schedulesState.value as? UiState.Success)?.data?.firstOrNull { it.id == id }
        }
        val previouslyDraft = existingSchedule?.published == false
        val resolvedSortOrder = existingSchedule?.sortOrder?.coerceIn(0L, 9_999_999_999_999L)
            ?: System.currentTimeMillis().coerceIn(0L, 9_999_999_999_999L)
        val willNotify = published && (existingId == null || previouslyDraft)
        val message = when {
            willNotify -> AppLanguageManager.tr(
                "Jadual diterbitkan & notifikasi dihantar kepada jemaat.",
                "Schedule published & notification sent to the congregation."
            )
            existingId == null -> AppLanguageManager.tr(
                "Draf jadual kegiatan berjaya disimpan.",
                "Activity schedule draft saved."
            )
            else -> AppLanguageManager.tr(
                "Jadual kegiatan berjaya dikemas kini.",
                "Activity schedule updated successfully."
            )
        }
        runMutation(
            successMessage = message,
            onComplete = onComplete
        ) {
            if (existingId == null) {
                repository.createSchedule(
                    title, description, category, dateLabel, timeLabel,
                    location, speaker, resolvedSortOrder, recurring, published
                )
            } else {
                repository.updateSchedule(
                    existingId, title, description, category, dateLabel, timeLabel,
                    location, speaker, resolvedSortOrder, recurring, published,
                    notifyOnPublish = previouslyDraft && published
                )
            }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        runMutation(AppLanguageManager.tr("Jadual kegiatan telah dipadam.", "Activity schedule deleted.")) {
            repository.deleteSchedule(scheduleId)
        }
    }

    fun toggleScheduleFeaturedInNews(schedule: ChurchSchedule) {
        if (!isAdmin.value) return
        val nextFeatured = !schedule.featuredInNews
        val message = if (nextFeatured) {
            AppLanguageManager.tr(
                "Kegiatan ditambah ke menu utama Berita (Penanda Penting).",
                "Activity pinned to the News home feed (Important Highlight)."
            )
        } else {
            AppLanguageManager.tr(
                "Kegiatan dikeluarkan daripada menu utama Berita.",
                "Activity removed from the News home feed."
            )
        }
        runMutation(message) {
            repository.setScheduleFeaturedInNews(schedule.id, nextFeatured)
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
        extraImagesBase64: List<String> = emptyList(),
        photographerName: String,
        published: Boolean,
        onComplete: () -> Unit = {}
    ) {
        val totalCount = (if (!imageBase64.isNullOrBlank()) 1 else 0) + extraImagesBase64.count { it.isNotBlank() }
        runMutation(
            successMessage = if (existingId == null) {
                if (totalCount > 1) {
                    AppLanguageManager.tr(
                        "$totalCount foto berjaya dimuat naik ke galeri masa nyata.",
                        "$totalCount photos uploaded to the live gallery."
                    )
                } else {
                    AppLanguageManager.tr(
                        "Foto berjaya ditambah ke galeri masa nyata.",
                        "Photo added to the live gallery."
                    )
                }
            } else {
                AppLanguageManager.tr(
                    "Maklumat foto berjaya dikemas kini.",
                    "Photo details updated successfully."
                )
            },
            onComplete = onComplete
        ) {
            if (existingId == null) {
                repository.createGalleryPhoto(
                    title, caption, category, eventDate, coverPreset,
                    imageUrl, imageBase64, extraImagesBase64, photographerName, published
                )
            } else {
                repository.updateGalleryPhoto(
                    existingId, title, caption, category, eventDate, coverPreset,
                    imageUrl, imageBase64, extraImagesBase64, photographerName, published
                )
            }
        }
    }

    fun deleteGalleryPhoto(photoId: String) {
        runMutation(AppLanguageManager.tr("Foto telah dipadam daripada galeri.", "Photo deleted from gallery.")) {
            repository.deleteGalleryPhoto(photoId)
        }
    }

    fun togglePhotoLike(photoId: String) {
        if (isGuest) {
            _snackbarMessage.tryEmit(
                AppLanguageManager.tr(
                    "Log masuk dengan akaun Google di tab Akaun untuk memberi tanda suka pada foto.",
                    "Sign in with Google in the Account tab to like photos."
                )
            )
            return
        }
        val alreadyLiked = likedPhotoIds.value.contains(photoId)
        viewModelScope.launch {
            val result = repository.togglePhotoLike(photoId, alreadyLiked)
            result.onFailure { err ->
                _snackbarMessage.tryEmit(
                    err.localizedMessage ?: AppLanguageManager.tr(
                        "Gagal mengemas kini tanda suka foto.",
                        "Failed to update photo like."
                    )
                )
            }
        }
    }

    fun observeCommentsForPhoto(photoId: String?) {
        _activeGalleryPhotoId.value = photoId
    }

    fun postGalleryComment(
        photoId: String,
        text: String,
        parentCommentId: String? = null,
        replyToAuthorName: String? = null,
        onComplete: () -> Unit = {}
    ) {
        if (isGuest) {
            _snackbarMessage.tryEmit(
                AppLanguageManager.tr(
                    "Log masuk dengan akaun Google di tab Akaun untuk menulis komen atau membalas.",
                    "Sign in with Google in the Account tab to comment or reply."
                )
            )
            return
        }
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return
        val resolvedAuthor = userProfile.value?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: userDisplayName?.trim()?.takeIf { it.isNotBlank() }
            ?: userEmail?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Jemaat SIB Toburon"

        viewModelScope.launch {
            _isPostingComment.value = true
            try {
                val result = repository.addGalleryComment(
                    photoId = photoId,
                    authorName = resolvedAuthor,
                    text = cleanText,
                    parentCommentId = parentCommentId,
                    replyToAuthorName = replyToAuthorName
                )
                result.onSuccess {
                    onComplete()
                }.onFailure { err ->
                    _snackbarMessage.tryEmit(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Gagal menghantar komen.",
                            "Failed to post comment."
                        )
                    )
                }
            } finally {
                _isPostingComment.value = false
            }
        }
    }

    fun deleteGalleryComment(photoId: String, commentId: String) {
        if (isGuest) return
        viewModelScope.launch {
            val result = repository.deleteGalleryComment(photoId, commentId)
            result.onSuccess {
                _snackbarMessage.tryEmit(
                    AppLanguageManager.tr("Komen telah dipadam.", "Comment deleted.")
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit(
                    err.localizedMessage ?: AppLanguageManager.tr(
                        "Gagal memadam komen.",
                        "Failed to delete comment."
                    )
                )
            }
        }
    }

    fun toggleBookmark(itemId: String, itemType: String, title: String) {
        if (isGuest) {
            _snackbarMessage.tryEmit(
                AppLanguageManager.tr(
                    "Log masuk dengan akaun Google di tab Akaun untuk menyimpan warta & jadual.",
                    "Sign in with Google in the Account tab to bookmark news & schedules."
                )
            )
            return
        }
        val alreadyBookmarked = bookmarks.value.any { it.itemId == itemId && it.itemType == itemType }
        viewModelScope.launch {
            val result = repository.toggleBookmark(itemId, itemType, title, alreadyBookmarked)
            result.onSuccess {
                _snackbarMessage.tryEmit(
                    if (alreadyBookmarked) {
                        AppLanguageManager.tr(
                            "Dibuang daripada senarai tersimpan.",
                            "Removed from your saved list."
                        )
                    } else {
                        AppLanguageManager.tr(
                            "Disimpan ke senarai peribadi anda.",
                            "Saved to your personal bookmarks."
                        )
                    }
                )
            }.onFailure { err ->
                _snackbarMessage.tryEmit(
                    err.localizedMessage ?: AppLanguageManager.tr(
                        "Gagal mengemas kini senarai simpanan.",
                        "Failed to update bookmarks."
                    )
                )
            }
        }
    }

    fun saveProfile(
        displayName: String,
        fellowshipGroup: FellowshipGroup,
        onComplete: () -> Unit = {}
    ) {
        runMutation(
            successMessage = AppLanguageManager.tr("Profil jemaat berjaya disimpan.", "Member profile saved successfully."),
            onComplete = onComplete
        ) {
            repository.saveUserProfile(displayName, fellowshipGroup, userProfile.value, userEmail)
        }
    }

    fun updateProfilePhoto(
        photoBase64: String?,
        onComplete: () -> Unit = {}
    ) {
        val message = if (photoBase64.isNullOrBlank()) {
            AppLanguageManager.tr("Gambar profil telah dipadam.", "Profile photo removed.")
        } else {
            AppLanguageManager.tr("Gambar profil berjaya dikemas kini.", "Profile photo updated successfully.")
        }
        viewModelScope.launch {
            _isBusy.value = true
            try {
                val result = repository.updateUserProfilePhoto(
                    photoBase64 = photoBase64,
                    existingProfile = userProfile.value,
                    defaultDisplayName = userDisplayName,
                    userEmail = userEmail
                )
                result.onSuccess {
                    _snackbarMessage.tryEmit(message)
                }.onFailure { err ->
                    _snackbarMessage.tryEmit(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Ralat berlaku semasa menyimpan gambar profil.",
                            "An error occurred while saving profile photo."
                        )
                    )
                }
            } finally {
                _isBusy.value = false
                onComplete()
            }
        }
    }

    fun addAdminMember(targetUid: String, displayName: String, roleTitle: String, onComplete: () -> Unit = {}) {
        runMutation(
            AppLanguageManager.tr("Pengurus admin berjaya ditambah.", "Admin member added successfully."),
            onComplete
        ) {
            repository.addAdminMember(targetUid, displayName, roleTitle)
        }
    }

    fun removeAdminMember(targetUid: String) {
        runMutation(AppLanguageManager.tr("Akses admin pengurus telah ditarik balik.", "Admin access revoked.")) {
            repository.removeAdminMember(targetUid)
        }
    }

    fun saveChurchMember(
        existingId: String?,
        fullName: String,
        myKadNumber: String?,
        gender: MemberGender,
        birthDateIso: String,
        baptismStatus: BaptismStatus,
        denomination: String,
        maritalStatus: MaritalStatus,
        spouseName: String?,
        sibMembershipNumber: String?,
        passportPhotoBase64: String? = null,
        memberEmail: String? = null,
        onComplete: () -> Unit = {}
    ) {
        val cleanMyKad = myKadNumber?.trim()?.takeIf { it.isNotEmpty() }
        val cleanDenom = if (baptismStatus == BaptismStatus.DIBAPTIS) denomination.trim() else ""
        val cleanSpouse = if (maritalStatus == MaritalStatus.BERKAHWIN) {
            spouseName?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val cleanSibNo = sibMembershipNumber?.trim()?.takeIf { it.isNotEmpty() }
        val cleanPhoto = passportPhotoBase64?.trim()?.takeIf { it.isNotEmpty() }
        val cleanEmail = memberEmail?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        runMutation(
            successMessage = if (existingId == null) {
                AppLanguageManager.tr("Ahli jemaat berjaya didaftarkan.", "Congregation member registered successfully.")
            } else {
                AppLanguageManager.tr("Maklumat ahli berjaya dikemas kini.", "Member information updated successfully.")
            },
            onComplete = {
                if (existingId != null && _selectedMemberDetail.value?.id == existingId) {
                    _selectedMemberDetail.value = _selectedMemberDetail.value?.copy(
                        fullName = fullName.trim(),
                        myKadNumber = cleanMyKad,
                        gender = gender.code,
                        birthDate = birthDateIso.trim(),
                        baptismStatus = baptismStatus.code,
                        denomination = cleanDenom,
                        maritalStatus = maritalStatus.code,
                        spouseName = cleanSpouse,
                        sibMembershipNumber = cleanSibNo,
                        passportPhotoBase64 = cleanPhoto,
                        memberEmail = cleanEmail
                    )
                }
                onComplete()
            }
        ) {
            if (existingId == null) {
                repository.createChurchMember(
                    fullName = fullName,
                    myKadNumber = cleanMyKad,
                    gender = gender,
                    birthDateIso = birthDateIso,
                    baptismStatus = baptismStatus,
                    denomination = cleanDenom,
                    maritalStatus = maritalStatus,
                    spouseName = cleanSpouse,
                    sibMembershipNumber = cleanSibNo,
                    passportPhotoBase64 = cleanPhoto,
                    memberEmail = cleanEmail
                )
            } else {
                repository.updateChurchMember(
                    memberId = existingId,
                    fullName = fullName,
                    myKadNumber = cleanMyKad,
                    gender = gender,
                    birthDateIso = birthDateIso,
                    baptismStatus = baptismStatus,
                    denomination = cleanDenom,
                    maritalStatus = maritalStatus,
                    spouseName = cleanSpouse,
                    sibMembershipNumber = cleanSibNo,
                    passportPhotoBase64 = cleanPhoto,
                    memberEmail = cleanEmail
                )
            }
        }
    }

    fun saveOwnChurchMember(
        fullName: String,
        myKadNumber: String?,
        gender: MemberGender,
        birthDateIso: String,
        baptismStatus: BaptismStatus,
        denomination: String,
        maritalStatus: MaritalStatus,
        spouseName: String?,
        sibMembershipNumber: String?,
        passportPhotoBase64: String? = null,
        onComplete: () -> Unit = {}
    ) {
        val verifiedEmail = userEmail?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (isGuest || verifiedEmail == null) {
            _snackbarMessage.tryEmit(
                AppLanguageManager.tr(
                    "Sila log masuk dengan akaun Google untuk mengisi maklumat ahli.",
                    "Please sign in with Google to fill in your member details."
                )
            )
            return
        }
        val existingOwnId = ownMemberDetail.value?.id
        val cleanMyKad = myKadNumber?.trim()?.takeIf { it.isNotEmpty() }
        val cleanDenom = if (baptismStatus == BaptismStatus.DIBAPTIS) denomination.trim() else ""
        val cleanSpouse = if (maritalStatus == MaritalStatus.BERKAHWIN) {
            spouseName?.trim()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val cleanSibNo = sibMembershipNumber?.trim()?.takeIf { it.isNotEmpty() }
        val cleanPhoto = passportPhotoBase64?.trim()?.takeIf { it.isNotEmpty() }

        runMutation(
            successMessage = if (existingOwnId == null) {
                AppLanguageManager.tr(
                    "Maklumat ahli berjaya didaftarkan! Kad Ahli dikemas kini & Admin telah dimaklumkan.",
                    "Member details registered! Your Member Card is updated & Admin has been notified."
                )
            } else {
                AppLanguageManager.tr(
                    "Maklumat Kad Ahli anda berjaya dikemas kini.",
                    "Your Member Card details have been updated."
                )
            },
            onComplete = onComplete
        ) {
            if (existingOwnId == null) {
                repository.registerOwnChurchMember(
                    fullName = fullName,
                    myKadNumber = cleanMyKad,
                    gender = gender,
                    birthDateIso = birthDateIso,
                    baptismStatus = baptismStatus,
                    denomination = cleanDenom,
                    maritalStatus = maritalStatus,
                    spouseName = cleanSpouse,
                    sibMembershipNumber = cleanSibNo,
                    passportPhotoBase64 = cleanPhoto,
                    verifiedMemberEmail = verifiedEmail
                )
            } else {
                repository.updateChurchMember(
                    memberId = existingOwnId,
                    fullName = fullName,
                    myKadNumber = cleanMyKad,
                    gender = gender,
                    birthDateIso = birthDateIso,
                    baptismStatus = baptismStatus,
                    denomination = cleanDenom,
                    maritalStatus = maritalStatus,
                    spouseName = cleanSpouse,
                    sibMembershipNumber = cleanSibNo,
                    passportPhotoBase64 = cleanPhoto,
                    memberEmail = verifiedEmail
                )
            }
        }
    }

    fun deleteChurchMember(memberId: String, onComplete: () -> Unit = {}) {
        runMutation(
            successMessage = AppLanguageManager.tr(
                "Ahli jemaat telah dipadam daripada senarai.",
                "Member removed from the directory."
            ),
            onComplete = {
                if (_selectedMemberDetail.value?.id == memberId) {
                    _selectedMemberDetail.value = null
                }
                onComplete()
            }
        ) {
            repository.deleteChurchMember(memberId)
        }
    }

    fun saveAppVersionConfig(
        latestVersionCode: Int,
        latestVersionName: String,
        releaseNotes: String,
        updateUrl: String,
        forceUpdate: Boolean,
        onComplete: () -> Unit = {}
    ) {
        if (!isAdmin.value) return
        runMutation(
            successMessage = AppLanguageManager.tr(
                "Tetapan versi aplikasi v$latestVersionName (Kod $latestVersionCode) berjaya dikemas kini.",
                "App version config v$latestVersionName (Build $latestVersionCode) updated."
            ),
            onComplete = onComplete
        ) {
            repository.saveAppVersionConfig(
                latestVersionCode = latestVersionCode,
                latestVersionName = latestVersionName,
                releaseNotes = releaseNotes,
                updateUrl = updateUrl,
                forceUpdate = forceUpdate
            )
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
                    _snackbarMessage.tryEmit(
                        err.localizedMessage ?: AppLanguageManager.tr(
                            "Ralat berlaku semasa menyimpan data.",
                            "An error occurred while saving data."
                        )
                    )
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
