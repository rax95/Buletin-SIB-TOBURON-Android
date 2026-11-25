package com.example.data

import android.content.Context
import com.example.R
import com.example.notifications.BulletinNotificationManager
import com.example.ui.i18n.AppLanguageManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class BulletinRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException(
                AppLanguageManager.tr(
                    "Pengguna mesti log masuk dengan Google sebelum mengakses data.",
                    "You must sign in with Google before accessing this data."
                )
            )
        }
        return uid
    }

    private fun isBootstrappedAdminEmail(email: String?, emailVerified: Boolean): Boolean {
        return emailVerified && email?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true
    }

    // --- REAL-TIME STREAMS (snapshots() with ESTIMATE) ---

    fun observeAdminAccess(uid: String?, email: String?, emailVerified: Boolean): Flow<Boolean> {
        if (uid.isNullOrBlank()) return flowOf(false)
        if (isBootstrappedAdminEmail(email, emailVerified)) return flowOf(true)
        val path = "admins/$uid"
        return db.collection("admins").document(uid)
            .snapshots()
            .map { it.exists() }
            .catch { error ->
                val firestoreError = unwrapFirestoreException(error)
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                emit(false)
            }
    }

    fun observeNews(uid: String?, isAdmin: Boolean, isBootstrappedAdmin: Boolean): Flow<List<NewsArticle>> = flow {
        val path = "news"
        val sourceFlow: Flow<List<NewsArticle>> = when {
            !uid.isNullOrBlank() && isAdmin && isBootstrappedAdmin -> {
                db.collection("news").snapshots().map { snapshot ->
                    snapshot.toObjects(NewsArticle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            !uid.isNullOrBlank() && isAdmin -> {
                val publishedFlow = db.collection("news").whereEqualTo("published", true).snapshots().map {
                    it.toObjects(NewsArticle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val ownFlow = db.collection("news").whereEqualTo("authorId", uid).snapshots().map {
                    it.toObjects(NewsArticle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                combine(publishedFlow, ownFlow) { pub, own ->
                    (pub + own).associateBy { it.id }.values.toList()
                }
            }
            else -> {
                // Guest or standard Jemaat: strictly filtered to published == true
                db.collection("news").whereEqualTo("published", true).snapshots().map { snapshot ->
                    snapshot.toObjects(NewsArticle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
        }
        emitAll(
            sourceFlow
                .map { list ->
                    list.sortedWith(
                        compareByDescending<NewsArticle> { it.pinned }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeSchedules(uid: String?, isAdmin: Boolean, isBootstrappedAdmin: Boolean): Flow<List<ChurchSchedule>> = flow {
        val path = "schedules"
        val sourceFlow: Flow<List<ChurchSchedule>> = when {
            !uid.isNullOrBlank() && isAdmin && isBootstrappedAdmin -> {
                db.collection("schedules").snapshots().map { snapshot ->
                    snapshot.toObjects(ChurchSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            !uid.isNullOrBlank() && isAdmin -> {
                val publishedFlow = db.collection("schedules").whereEqualTo("published", true).snapshots().map {
                    it.toObjects(ChurchSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val ownFlow = db.collection("schedules").whereEqualTo("authorId", uid).snapshots().map {
                    it.toObjects(ChurchSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                combine(publishedFlow, ownFlow) { pub, own ->
                    (pub + own).associateBy { it.id }.values.toList()
                }
            }
            else -> {
                db.collection("schedules").whereEqualTo("published", true).snapshots().map { snapshot ->
                    snapshot.toObjects(ChurchSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
        }
        emitAll(
            sourceFlow
                .map { list ->
                    list.sortedWith(
                        compareByDescending<ChurchSchedule> { it.createdAt ?: Timestamp(0, 0) }
                            .thenByDescending { it.updatedAt ?: Timestamp(0, 0) }
                            .thenBy { it.title.lowercase() }
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeGallery(uid: String?, isAdmin: Boolean, isBootstrappedAdmin: Boolean): Flow<List<GalleryPhoto>> = flow {
        val path = "gallery_photos"
        val sourceFlow: Flow<List<GalleryPhoto>> = when {
            !uid.isNullOrBlank() && isAdmin && isBootstrappedAdmin -> {
                db.collection("gallery_photos").snapshots().map { snapshot ->
                    snapshot.toObjects(GalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            !uid.isNullOrBlank() && isAdmin -> {
                val publishedFlow = db.collection("gallery_photos").whereEqualTo("published", true).snapshots().map {
                    it.toObjects(GalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                val ownFlow = db.collection("gallery_photos").whereEqualTo("authorId", uid).snapshots().map {
                    it.toObjects(GalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                combine(publishedFlow, ownFlow) { pub, own ->
                    (pub + own).associateBy { it.id }.values.toList()
                }
            }
            else -> {
                db.collection("gallery_photos").whereEqualTo("published", true).snapshots().map { snapshot ->
                    snapshot.toObjects(GalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
        }
        emitAll(
            sourceFlow
                .map { list ->
                    list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserBookmarks(uid: String?): Flow<List<UserBookmark>> = flow {
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users/$uid/bookmarks"
        emitAll(
            db.collection("users").document(uid).collection("bookmarks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserBookmark::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeLikedPhotoIds(uid: String?): Flow<Set<String>> = flow {
        if (uid.isNullOrBlank()) {
            emit(emptySet())
            return@flow
        }
        val path = "users/$uid/liked_photos"
        emitAll(
            db.collection("users").document(uid).collection("liked_photos")
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.map { it.id }.toSet()
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserProfile(uid: String?): Flow<UserProfile?> = flow {
        if (uid.isNullOrBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeAdminMembers(uid: String?, isBootstrappedAdmin: Boolean): Flow<List<AdminMember>> = flow {
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "admins"
        val query = if (isBootstrappedAdmin) {
            db.collection("admins")
        } else {
            db.collection("admins").whereEqualTo("addedBy", uid)
        }
        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AdminMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeRegisteredUsers(uid: String?): Flow<List<UserProfile>> = flow {
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "users"
        emitAll(
            db.collection("users")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<UserProfile> {
                                (it.lastLoginAt ?: it.updatedAt ?: it.createdAt ?: Timestamp(0, 0)).seconds
                            }.thenBy { it.displayName.lowercase() }
                        )
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeNotificationEvents(uid: String?, isAdmin: Boolean = false): Flow<List<NotificationEvent>> = flow {
        val path = "notification_events"
        val query = if (isAdmin && !uid.isNullOrBlank()) {
            db.collection("notification_events")
        } else {
            db.collection("notification_events").whereEqualTo("published", true)
        }
        emitAll(
            query
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        NotificationEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun saveFcmToken(
        token: String,
        newsEnabled: Boolean,
        schedulesEnabled: Boolean
    ): Result<Unit> {
        var docPath = "users/fcm_tokens"
        return try {
            val uid = requireUserId()
            val cleanToken = token.trim()
            require(cleanToken.length in 10..512) {
                AppLanguageManager.tr("Token FCM tidak sah.", "Invalid FCM token.")
            }
            val tokenDocId = BulletinNotificationManager.buildTokenDocId(cleanToken)
            val docRef = db.collection("users")
                .document(uid)
                .collection("fcm_tokens")
                .document(tokenDocId)
            docPath = docRef.path
            docRef.set(
                mapOf(
                    "token" to cleanToken,
                    "newsEnabled" to newsEnabled,
                    "schedulesEnabled" to schedulesEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    private fun buildNotificationEventPayload(
        title: String,
        body: String,
        eventType: String,
        targetId: String,
        topic: String,
        authorId: String
    ): Map<String, Any> {
        val safeTitle = title.trim().take(160).let { if (it.length < 2) "SIB Toburon" else it }
        val safeBody = body.trim().take(320).let {
            if (it.length < 2) AppLanguageManager.tr("Kemas kini terbaharu tersedia.", "A new update is available.") else it
        }
        return mapOf(
            "title" to safeTitle,
            "body" to safeBody,
            "eventType" to eventType,
            "targetId" to targetId,
            "topic" to topic,
            "published" to true,
            "authorId" to authorId,
            "createdAt" to FieldValue.serverTimestamp()
        )
    }

    // --- NEWS MUTATIONS (ADMIN) ---

    suspend fun createNews(
        title: String,
        summary: String,
        content: String,
        categoryCode: String,
        authorName: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        pinned: Boolean,
        published: Boolean
    ): Result<Unit> {
        var docPath = "news"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanSummary = summary.trim()
            val cleanContent = content.trim()
            val cleanCategory = categoryCode.trim()
            val cleanAuthor = authorName.trim().ifBlank { "Admin SIB Toburon" }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }

            require(cleanCategory.matches(Regex("^[a-zA-Z0-9_\\-]{1,40}$"))) {
                AppLanguageManager.tr("Kategori berita tidak sah.", "Invalid news category.")
            }
            require(cleanTitle.length in 3..160) {
                AppLanguageManager.tr("Tajuk berita mestilah antara 3 hingga 160 aksara.", "News title must be 3 to 160 characters.")
            }
            require(cleanSummary.length in 5..320) {
                AppLanguageManager.tr("Ringkasan berita mestilah antara 5 hingga 320 aksara.", "News summary must be 5 to 320 characters.")
            }
            require(cleanContent.length in 10..5000) {
                AppLanguageManager.tr("Kandungan berita minimum 10 aksara (maks 5000).", "News content must be 10 to 5000 characters.")
            }
            if (cleanUrl != null) require(cleanUrl.length <= 1000) {
                AppLanguageManager.tr("URL gambar terlalu panjang.", "Image URL is too long.")
            }
            if (cleanBase64 != null) require(cleanBase64.length <= 260_000) {
                AppLanguageManager.tr("Saiz gambar terlalu besar.", "Image size is too large.")
            }

            val docRef = db.collection("news").document()
            docPath = docRef.path
            val payload = mapOf<String, Any?>(
                "title" to cleanTitle,
                "summary" to cleanSummary,
                "content" to cleanContent,
                "category" to cleanCategory,
                "authorId" to uid,
                "authorName" to cleanAuthor,
                "coverPreset" to coverPreset.code,
                "imageUrl" to cleanUrl,
                "imageBase64" to cleanBase64,
                "pinned" to pinned,
                "published" to published,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            if (published) {
                val batch = db.batch()
                val notifRef = db.collection("notification_events").document()
                batch.set(docRef, payload)
                batch.set(
                    notifRef,
                    buildNotificationEventPayload(
                        title = AppLanguageManager.tr("Berita Baharu: $cleanTitle", "Latest News: $cleanTitle"),
                        body = cleanSummary,
                        eventType = BulletinNotificationManager.TYPE_NEWS,
                        targetId = docRef.id,
                        topic = BulletinNotificationManager.TOPIC_NEWS,
                        authorId = uid
                    )
                )
                batch.commit().await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateNews(
        newsId: String,
        title: String,
        summary: String,
        content: String,
        categoryCode: String,
        authorName: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        pinned: Boolean,
        published: Boolean,
        notifyOnPublish: Boolean = false
    ): Result<Unit> {
        val docPath = "news/$newsId"
        return try {
            val uid = requireUserId()
            require(newsId.isNotBlank()) { AppLanguageManager.tr("ID berita tidak sah.", "Invalid news ID.") }
            val cleanTitle = title.trim()
            val cleanSummary = summary.trim()
            val cleanContent = content.trim()
            val cleanCategory = categoryCode.trim()
            val cleanAuthor = authorName.trim().ifBlank { "Admin SIB Toburon" }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }

            require(cleanCategory.matches(Regex("^[a-zA-Z0-9_\\-]{1,40}$"))) {
                AppLanguageManager.tr("Kategori berita tidak sah.", "Invalid news category.")
            }
            require(cleanTitle.length in 3..160) {
                AppLanguageManager.tr("Tajuk berita mestilah antara 3 hingga 160 aksara.", "News title must be 3 to 160 characters.")
            }
            require(cleanSummary.length in 5..320) {
                AppLanguageManager.tr("Ringkasan berita mestilah antara 5 hingga 320 aksara.", "News summary must be 5 to 320 characters.")
            }
            require(cleanContent.length in 10..5000) {
                AppLanguageManager.tr("Kandungan berita minimum 10 aksara (maks 5000).", "News content must be 10 to 5000 characters.")
            }
            if (cleanUrl != null) require(cleanUrl.length <= 1000) {
                AppLanguageManager.tr("URL gambar terlalu panjang.", "Image URL is too long.")
            }
            if (cleanBase64 != null) require(cleanBase64.length <= 260_000) {
                AppLanguageManager.tr("Saiz gambar terlalu besar.", "Image size is too large.")
            }

            val updates = mapOf<String, Any?>(
                "title" to cleanTitle,
                "summary" to cleanSummary,
                "content" to cleanContent,
                "category" to cleanCategory,
                "authorName" to cleanAuthor,
                "coverPreset" to coverPreset.code,
                "imageUrl" to cleanUrl,
                "imageBase64" to cleanBase64,
                "pinned" to pinned,
                "published" to published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection("news").document(newsId)
            if (published && notifyOnPublish) {
                val batch = db.batch()
                val notifRef = db.collection("notification_events").document()
                batch.update(docRef, updates)
                batch.set(
                    notifRef,
                    buildNotificationEventPayload(
                        title = AppLanguageManager.tr("Berita Baharu: $cleanTitle", "Latest News: $cleanTitle"),
                        body = cleanSummary,
                        eventType = BulletinNotificationManager.TYPE_NEWS,
                        targetId = newsId,
                        topic = BulletinNotificationManager.TOPIC_NEWS,
                        authorId = uid
                    )
                )
                batch.commit().await()
            } else {
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNews(newsId: String): Result<Unit> {
        val docPath = "news/$newsId"
        return try {
            requireUserId()
            require(newsId.isNotBlank()) { AppLanguageManager.tr("ID berita tidak sah.", "Invalid news ID.") }
            db.collection("news").document(newsId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeNewsComments(newsId: String, uid: String?): Flow<List<NewsComment>> = flow {
        if (newsId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "news/$newsId/comments"
        emitAll(
            db.collection("news")
                .document(newsId)
                .collection("comments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        NewsComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addNewsComment(
        newsId: String,
        authorName: String,
        text: String,
        parentCommentId: String? = null,
        replyToAuthorName: String? = null
    ): Result<Unit> {
        var docPath = "news/$newsId/comments"
        return try {
            val uid = requireUserId()
            require(newsId.isNotBlank()) {
                AppLanguageManager.tr("ID berita tidak sah.", "Invalid news ID.")
            }
            val cleanText = text.trim()
            val cleanAuthor = authorName.trim().ifBlank { "Jemaat SIB Toburon" }.take(80)
            val cleanParentId = parentCommentId?.trim()?.takeIf { it.isNotBlank() }
            val cleanReplyTo = replyToAuthorName?.trim()?.takeIf { it.isNotBlank() }?.take(80)

            require(cleanText.length in 1..500) {
                AppLanguageManager.tr(
                    "Komen mestilah antara 1 hingga 500 aksara.",
                    "Comment must be between 1 and 500 characters."
                )
            }

            val docRef = db.collection("news")
                .document(newsId)
                .collection("comments")
                .document()
            docPath = docRef.path

            val payload = mapOf<String, Any?>(
                "newsId" to newsId,
                "authorId" to uid,
                "authorName" to cleanAuthor,
                "text" to cleanText,
                "parentCommentId" to cleanParentId,
                "replyToAuthorName" to cleanReplyTo,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteNewsComment(newsId: String, commentId: String): Result<Unit> {
        val docPath = "news/$newsId/comments/$commentId"
        return try {
            requireUserId()
            require(newsId.isNotBlank() && commentId.isNotBlank()) {
                AppLanguageManager.tr("ID komen tidak sah.", "Invalid comment ID.")
            }
            db.collection("news")
                .document(newsId)
                .collection("comments")
                .document(commentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- SCHEDULE MUTATIONS (ADMIN) ---

    suspend fun createSchedule(
        title: String,
        description: String,
        categoryCode: String,
        dateLabel: String,
        timeLabel: String,
        location: String,
        speaker: String,
        sortOrder: Long,
        recurring: Boolean,
        published: Boolean
    ): Result<Unit> {
        var docPath = "schedules"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanCategory = categoryCode.trim()
            val cleanDate = dateLabel.trim()
            val cleanTime = timeLabel.trim()
            val cleanLocation = location.trim()
            val cleanSpeaker = speaker.trim()

            require(cleanCategory.matches(Regex("^[a-zA-Z0-9_\\-]{1,40}$"))) {
                AppLanguageManager.tr("Kategori jadual tidak sah.", "Invalid schedule category.")
            }
            require(cleanTitle.length in 3..140) {
                AppLanguageManager.tr("Nama kegiatan mestilah 3-140 aksara.", "Activity name must be 3-140 characters.")
            }
            require(cleanDesc.length in 1..1500) {
                AppLanguageManager.tr("Keterangan kegiatan wajib diisi (maks 1500 aksara).", "Activity description is required (max 1500 characters).")
            }
            require(cleanDate.length in 3..60) {
                AppLanguageManager.tr("Tarikh / hari wajib diisi (3-60 aksara).", "Date / day is required (3-60 characters).")
            }
            require(cleanTime.length in 3..40) {
                AppLanguageManager.tr("Masa kegiatan wajib diisi (3-40 aksara).", "Activity time is required (3-40 characters).")
            }
            require(cleanLocation.length in 2..120) {
                AppLanguageManager.tr("Lokasi kegiatan wajib diisi (2-120 aksara).", "Activity location is required (2-120 characters).")
            }
            require(cleanSpeaker.length <= 100) {
                AppLanguageManager.tr("Nama pengkhotbah/penyelaras maks 100 aksara.", "Speaker/coordinator name max 100 characters.")
            }
            require(sortOrder in 0L..9_999_999_999_999L) {
                AppLanguageManager.tr("Susunan jadual tidak sah.", "Invalid schedule sort order.")
            }

            val docRef = db.collection("schedules").document()
            docPath = docRef.path
            val payload = mapOf(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to cleanCategory,
                "dateLabel" to cleanDate,
                "timeLabel" to cleanTime,
                "location" to cleanLocation,
                "speaker" to cleanSpeaker,
                "sortOrder" to sortOrder,
                "recurring" to recurring,
                "published" to published,
                "authorId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (published) {
                val batch = db.batch()
                val notifRef = db.collection("notification_events").document()
                batch.set(docRef, payload)
                batch.set(
                    notifRef,
                    buildNotificationEventPayload(
                        title = AppLanguageManager.tr("Jadual Baharu: $cleanTitle", "New Schedule: $cleanTitle"),
                        body = "$cleanDate • $cleanTime — $cleanLocation",
                        eventType = BulletinNotificationManager.TYPE_SCHEDULE,
                        targetId = docRef.id,
                        topic = BulletinNotificationManager.TOPIC_SCHEDULES,
                        authorId = uid
                    )
                )
                batch.commit().await()
            } else {
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSchedule(
        scheduleId: String,
        title: String,
        description: String,
        categoryCode: String,
        dateLabel: String,
        timeLabel: String,
        location: String,
        speaker: String,
        sortOrder: Long,
        recurring: Boolean,
        published: Boolean,
        notifyOnPublish: Boolean = false
    ): Result<Unit> {
        val docPath = "schedules/$scheduleId"
        return try {
            val uid = requireUserId()
            require(scheduleId.isNotBlank()) { AppLanguageManager.tr("ID jadual tidak sah.", "Invalid schedule ID.") }
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanCategory = categoryCode.trim()
            val cleanDate = dateLabel.trim()
            val cleanTime = timeLabel.trim()
            val cleanLocation = location.trim()
            val cleanSpeaker = speaker.trim()

            require(cleanCategory.matches(Regex("^[a-zA-Z0-9_\\-]{1,40}$"))) {
                AppLanguageManager.tr("Kategori jadual tidak sah.", "Invalid schedule category.")
            }
            require(cleanTitle.length in 3..140) {
                AppLanguageManager.tr("Nama kegiatan mestilah 3-140 aksara.", "Activity name must be 3-140 characters.")
            }
            require(cleanDesc.length in 1..1500) {
                AppLanguageManager.tr("Keterangan kegiatan wajib diisi.", "Activity description is required.")
            }
            require(cleanDate.length in 3..60) {
                AppLanguageManager.tr("Tarikh / hari wajib diisi.", "Date / day is required.")
            }
            require(cleanTime.length in 3..40) {
                AppLanguageManager.tr("Masa kegiatan wajib diisi.", "Activity time is required.")
            }
            require(cleanLocation.length in 2..120) {
                AppLanguageManager.tr("Lokasi kegiatan wajib diisi.", "Activity location is required.")
            }
            require(cleanSpeaker.length <= 100) {
                AppLanguageManager.tr("Nama pengkhotbah maks 100 aksara.", "Speaker name max 100 characters.")
            }
            require(sortOrder in 0L..9_999_999_999_999L) {
                AppLanguageManager.tr("Susunan jadual tidak sah.", "Invalid schedule sort order.")
            }

            val updates = mapOf(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to cleanCategory,
                "dateLabel" to cleanDate,
                "timeLabel" to cleanTime,
                "location" to cleanLocation,
                "speaker" to cleanSpeaker,
                "sortOrder" to sortOrder,
                "recurring" to recurring,
                "published" to published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val docRef = db.collection("schedules").document(scheduleId)
            if (published && notifyOnPublish) {
                val batch = db.batch()
                val notifRef = db.collection("notification_events").document()
                batch.update(docRef, updates)
                batch.set(
                    notifRef,
                    buildNotificationEventPayload(
                        title = AppLanguageManager.tr("Jadual Baharu: $cleanTitle", "New Schedule: $cleanTitle"),
                        body = "$cleanDate • $cleanTime — $cleanLocation",
                        eventType = BulletinNotificationManager.TYPE_SCHEDULE,
                        targetId = scheduleId,
                        topic = BulletinNotificationManager.TOPIC_SCHEDULES,
                        authorId = uid
                    )
                )
                batch.commit().await()
            } else {
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSchedule(scheduleId: String): Result<Unit> {
        val docPath = "schedules/$scheduleId"
        return try {
            requireUserId()
            require(scheduleId.isNotBlank()) { AppLanguageManager.tr("ID jadual tidak sah.", "Invalid schedule ID.") }
            db.collection("schedules").document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun setScheduleFeaturedInNews(
        scheduleId: String,
        featured: Boolean
    ): Result<Unit> {
        val docPath = "schedules/$scheduleId"
        return try {
            requireUserId()
            require(scheduleId.isNotBlank()) { AppLanguageManager.tr("ID jadual tidak sah.", "Invalid schedule ID.") }
            db.collection("schedules").document(scheduleId).update(
                mapOf(
                    "featuredInNews" to featured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    // --- GALLERY MUTATIONS (ADMIN + ATOMIC JEMAAT LIKES) ---

    suspend fun createGalleryPhoto(
        title: String,
        caption: String,
        category: GalleryCategory,
        eventDate: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        extraImagesBase64: List<String> = emptyList(),
        photographerName: String,
        published: Boolean
    ): Result<Unit> {
        var docPath = "gallery_photos"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanCaption = caption.trim()
            val cleanDate = eventDate.trim()
            val cleanPhotoBy = photographerName.trim().ifBlank {
                AppLanguageManager.tr("Pasukan Multimedia SIB Toburon", "SIB Toburon Media Team")
            }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }
            val cleanExtras = extraImagesBase64.map { it.trim() }.filter { it.isNotBlank() }

            require(cleanTitle.length in 2..120) {
                AppLanguageManager.tr("Tajuk foto mestilah 2-120 aksara.", "Photo title must be 2-120 characters.")
            }
            require(cleanCaption.length <= 600) {
                AppLanguageManager.tr("Keterangan foto maksimum 600 aksara.", "Photo caption maximum 600 characters.")
            }
            require(cleanDate.length in 2..50) {
                AppLanguageManager.tr("Tarikh acara wajib diisi (2-50 aksara).", "Event date is required (2-50 characters).")
            }
            if (cleanUrl != null) require(cleanUrl.length <= 1000) {
                AppLanguageManager.tr("URL gambar terlalu panjang.", "Image URL is too long.")
            }
            if (cleanBase64 != null) require(cleanBase64.length <= 260_000) {
                AppLanguageManager.tr("Saiz gambar terlalu besar.", "Image size is too large.")
            }
            require(cleanExtras.size <= 7) {
                AppLanguageManager.tr(
                    "Maksimum 8 gambar dibenarkan dalam satu muat naik.",
                    "A maximum of 8 photos is allowed per upload."
                )
            }
            require(cleanExtras.all { it.length <= 260_000 }) {
                AppLanguageManager.tr("Salah satu gambar melebihi had saiz.", "One of the selected images exceeds the size limit.")
            }
            val totalB64Size = (cleanBase64?.length ?: 0) + cleanExtras.sumOf { it.length }
            require(totalB64Size <= 840_000) {
                AppLanguageManager.tr(
                    "Jumlah saiz semua gambar terlalu besar. Sila kurangkan bilangan gambar.",
                    "Combined size of all selected photos is too large. Please remove a few photos."
                )
            }

            val docRef = db.collection("gallery_photos").document()
            docPath = docRef.path
            val payload = mapOf<String, Any?>(
                "title" to cleanTitle,
                "caption" to cleanCaption,
                "category" to category.code,
                "eventDate" to cleanDate,
                "coverPreset" to coverPreset.code,
                "imageUrl" to cleanUrl,
                "imageBase64" to cleanBase64,
                "extraImagesBase64" to cleanExtras.takeIf { it.isNotEmpty() },
                "likesCount" to 0L,
                "published" to published,
                "authorId" to uid,
                "photographerName" to cleanPhotoBy,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateGalleryPhoto(
        photoId: String,
        title: String,
        caption: String,
        category: GalleryCategory,
        eventDate: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        extraImagesBase64: List<String> = emptyList(),
        photographerName: String,
        published: Boolean
    ): Result<Unit> {
        val docPath = "gallery_photos/$photoId"
        return try {
            requireUserId()
            require(photoId.isNotBlank()) { AppLanguageManager.tr("ID foto tidak sah.", "Invalid photo ID.") }
            val cleanTitle = title.trim()
            val cleanCaption = caption.trim()
            val cleanDate = eventDate.trim()
            val cleanPhotoBy = photographerName.trim().ifBlank {
                AppLanguageManager.tr("Pasukan Multimedia SIB Toburon", "SIB Toburon Media Team")
            }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }
            val cleanExtras = extraImagesBase64.map { it.trim() }.filter { it.isNotBlank() }

            require(cleanTitle.length in 2..120) {
                AppLanguageManager.tr("Tajuk foto mestilah 2-120 aksara.", "Photo title must be 2-120 characters.")
            }
            require(cleanCaption.length <= 600) {
                AppLanguageManager.tr("Keterangan foto maksimum 600 aksara.", "Photo caption maximum 600 characters.")
            }
            require(cleanDate.length in 2..50) {
                AppLanguageManager.tr("Tarikh acara wajib diisi.", "Event date is required.")
            }
            if (cleanUrl != null) require(cleanUrl.length <= 1000) {
                AppLanguageManager.tr("URL gambar terlalu panjang.", "Image URL is too long.")
            }
            if (cleanBase64 != null) require(cleanBase64.length <= 260_000) {
                AppLanguageManager.tr("Saiz gambar terlalu besar.", "Image size is too large.")
            }
            require(cleanExtras.size <= 7) {
                AppLanguageManager.tr(
                    "Maksimum 8 gambar dibenarkan dalam satu muat naik.",
                    "A maximum of 8 photos is allowed per upload."
                )
            }
            require(cleanExtras.all { it.length <= 260_000 }) {
                AppLanguageManager.tr("Salah satu gambar melebihi had saiz.", "One of the selected images exceeds the size limit.")
            }
            val totalB64Size = (cleanBase64?.length ?: 0) + cleanExtras.sumOf { it.length }
            require(totalB64Size <= 840_000) {
                AppLanguageManager.tr(
                    "Jumlah saiz semua gambar terlalu besar. Sila kurangkan bilangan gambar.",
                    "Combined size of all selected photos is too large. Please remove a few photos."
                )
            }

            val updates = mapOf<String, Any?>(
                "title" to cleanTitle,
                "caption" to cleanCaption,
                "category" to category.code,
                "eventDate" to cleanDate,
                "coverPreset" to coverPreset.code,
                "imageUrl" to cleanUrl,
                "imageBase64" to cleanBase64,
                "extraImagesBase64" to cleanExtras,
                "photographerName" to cleanPhotoBy,
                "published" to published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("gallery_photos").document(photoId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGalleryPhoto(photoId: String): Result<Unit> {
        val docPath = "gallery_photos/$photoId"
        return try {
            requireUserId()
            require(photoId.isNotBlank()) { AppLanguageManager.tr("ID foto tidak sah.", "Invalid photo ID.") }
            db.collection("gallery_photos").document(photoId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun togglePhotoLike(photoId: String, currentlyLiked: Boolean): Result<Unit> {
        val docPath = "gallery_photos/$photoId"
        return try {
            val uid = requireUserId()
            require(photoId.isNotBlank()) { AppLanguageManager.tr("ID foto tidak sah.", "Invalid photo ID.") }
            val photoRef = db.collection("gallery_photos").document(photoId)
            val likeRef = db.collection("users").document(uid).collection("liked_photos").document(photoId)
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(
                    photoRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "photoId" to photoId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    photoRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeGalleryComments(photoId: String, uid: String?): Flow<List<GalleryComment>> = flow {
        if (photoId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "gallery_photos/$photoId/comments"
        emitAll(
            db.collection("gallery_photos")
                .document(photoId)
                .collection("comments")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GalleryComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun addGalleryComment(
        photoId: String,
        authorName: String,
        text: String,
        parentCommentId: String? = null,
        replyToAuthorName: String? = null
    ): Result<Unit> {
        var docPath = "gallery_photos/$photoId/comments"
        return try {
            val uid = requireUserId()
            require(photoId.isNotBlank()) {
                AppLanguageManager.tr("ID foto tidak sah.", "Invalid photo ID.")
            }
            val cleanText = text.trim()
            val cleanAuthor = authorName.trim().ifBlank { "Jemaat SIB Toburon" }.take(80)
            val cleanParentId = parentCommentId?.trim()?.takeIf { it.isNotBlank() }
            val cleanReplyTo = replyToAuthorName?.trim()?.takeIf { it.isNotBlank() }?.take(80)

            require(cleanText.length in 1..500) {
                AppLanguageManager.tr(
                    "Komen mestilah antara 1 hingga 500 aksara.",
                    "Comment must be between 1 and 500 characters."
                )
            }

            val docRef = db.collection("gallery_photos")
                .document(photoId)
                .collection("comments")
                .document()
            docPath = docRef.path

            val payload = mapOf<String, Any?>(
                "photoId" to photoId,
                "authorId" to uid,
                "authorName" to cleanAuthor,
                "text" to cleanText,
                "parentCommentId" to cleanParentId,
                "replyToAuthorName" to cleanReplyTo,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteGalleryComment(photoId: String, commentId: String): Result<Unit> {
        val docPath = "gallery_photos/$photoId/comments/$commentId"
        return try {
            requireUserId()
            require(photoId.isNotBlank() && commentId.isNotBlank()) {
                AppLanguageManager.tr("ID komen tidak sah.", "Invalid comment ID.")
            }
            db.collection("gallery_photos")
                .document(photoId)
                .collection("comments")
                .document(commentId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- JEMAAT BOOKMARKS & PROFILE ---

    suspend fun toggleBookmark(
        itemId: String,
        itemType: String,
        title: String,
        currentlyBookmarked: Boolean
    ): Result<Unit> {
        var docPath = "users/bookmarks"
        return try {
            val uid = requireUserId()
            require(itemId.isNotBlank()) { AppLanguageManager.tr("ID item tidak sah.", "Invalid item ID.") }
            val bookmarkDocId = "${itemType.lowercase()}_$itemId"
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(bookmarkDocId)
            docPath = docRef.path
            if (currentlyBookmarked) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "itemId" to itemId,
                    "itemType" to itemType,
                    "title" to title.trim().take(160).ifBlank {
                        AppLanguageManager.tr("Item Tersimpan", "Saved Item")
                    },
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordSignedInUser(
        googleDisplayName: String?,
        googleEmail: String?
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanEmail = googleEmail?.trim().orEmpty().take(160)
            val fallbackName = googleDisplayName?.trim()?.takeIf { it.isNotBlank() }
                ?: cleanEmail.substringBefore("@").takeIf { it.isNotBlank() }
                ?: "Jemaat SIB Toburon"
            val cleanName = fallbackName.take(80)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snap = docRef.get().await()
            if (!snap.exists()) {
                docRef.set(
                    mapOf(
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "fellowshipGroup" to FellowshipGroup.UMUM.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastLoginAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                )
                if (cleanEmail.isNotBlank()) {
                    updates["email"] = cleanEmail
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveUserProfile(
        displayName: String,
        fellowshipGroup: FellowshipGroup,
        existingProfile: UserProfile?,
        userEmail: String? = null
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().ifBlank { "Jemaat SIB Toburon" }.take(80)
            val cleanEmail = userEmail?.trim().orEmpty().take(160)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val exists = existingProfile != null || docRef.get().await().exists()
            if (!exists) {
                docRef.set(
                    mapOf(
                        "displayName" to cleanName,
                        "email" to cleanEmail,
                        "fellowshipGroup" to fellowshipGroup.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                        "lastLoginAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "fellowshipGroup" to fellowshipGroup.code,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanEmail.isNotBlank()) {
                    updates["email"] = cleanEmail
                }
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfilePhoto(
        photoBase64: String?,
        existingProfile: UserProfile?,
        defaultDisplayName: String? = null,
        userEmail: String? = null
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanBase64 = photoBase64?.trim()?.takeIf { it.isNotBlank() }
            if (cleanBase64 != null) {
                require(cleanBase64.length <= 160_000) {
                    AppLanguageManager.tr(
                        "Saiz gambar profil terlalu besar. Sila pilih foto lain.",
                        "Profile photo is too large. Please choose another image."
                    )
                }
            }
            val cleanEmail = userEmail?.trim().orEmpty().take(160)
            val fallbackName = existingProfile?.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: defaultDisplayName?.trim()?.takeIf { it.isNotBlank() }
                ?: cleanEmail.substringBefore("@").takeIf { it.isNotBlank() }
                ?: "Jemaat SIB Toburon"
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val exists = existingProfile != null || docRef.get().await().exists()
            if (!exists) {
                val payload = mutableMapOf<String, Any?>(
                    "displayName" to fallbackName.take(80),
                    "email" to cleanEmail,
                    "fellowshipGroup" to (existingProfile?.resolvedGroup() ?: FellowshipGroup.UMUM).code,
                    "photoBase64" to cleanBase64,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val updates = mapOf<String, Any?>(
                    "photoBase64" to cleanBase64,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- DELEGATED ADMIN MANAGEMENT ---

    suspend fun addAdminMember(targetUid: String, displayName: String, roleTitle: String): Result<Unit> {
        val cleanUid = targetUid.trim()
        val docPath = "admins/$cleanUid"
        return try {
            val uid = requireUserId()
            require(cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]+$")) && cleanUid.length in 1..128) {
                AppLanguageManager.tr(
                    "Format UID tidak sah (hanya huruf, nombor, _ atau -).",
                    "Invalid UID format (letters, numbers, _ or - only)."
                )
            }
            val cleanName = displayName.trim()
            val cleanRole = roleTitle.trim()
            require(cleanName.length in 1..80) {
                AppLanguageManager.tr("Nama pengurus wajib diisi (maks 80 aksara).", "Admin name is required (max 80 characters).")
            }
            require(cleanRole.length in 1..60) {
                AppLanguageManager.tr("Jawatan pelayanan wajib diisi (maks 60 aksara).", "Ministry role is required (max 60 characters).")
            }

            db.collection("admins").document(cleanUid).set(
                mapOf(
                    "displayName" to cleanName,
                    "roleTitle" to cleanRole,
                    "addedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun removeAdminMember(targetUid: String): Result<Unit> {
        val docPath = "admins/$targetUid"
        return try {
            requireUserId()
            require(targetUid.isNotBlank()) { AppLanguageManager.tr("UID tidak sah.", "Invalid UID.") }
            db.collection("admins").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- CHURCH MEMBERS DIRECTORY (/church_members & /church_member_details) ---

    fun observeChurchMembers(uid: String?): Flow<List<ChurchMemberPublic>> = flow {
        if (uid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "church_members"
        emitAll(
            db.collection("church_members")
                .whereEqualTo("published", true)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChurchMemberPublic::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<ChurchMemberPublic> {
                            (it.createdAt ?: Timestamp(0, 0)).seconds
                        }.thenByDescending {
                            (it.createdAt ?: Timestamp(0, 0)).nanoseconds
                        }.thenBy {
                            it.fullName.lowercase()
                        }
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeOwnChurchMemberDetail(uid: String?, email: String?): Flow<ChurchMemberDetail?> = flow {
        val cleanEmail = email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (uid.isNullOrBlank() || cleanEmail == null) {
            emit(null)
            return@flow
        }
        val path = "church_member_details"
        emitAll(
            db.collection("church_member_details")
                .whereEqualTo("memberEmail", cleanEmail)
                .limit(5)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents
                        .mapNotNull { doc ->
                            doc.toObject(
                                ChurchMemberDetail::class.java,
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        }
                        .maxWithOrNull(
                            compareBy<ChurchMemberDetail> { (it.updatedAt ?: Timestamp(0, 0)).seconds }
                                .thenBy { (it.updatedAt ?: Timestamp(0, 0)).nanoseconds }
                        )
                }
                .catch { error ->
                    // Gracefully emit null if account email isn't verified or not linked yet
                    val firestoreErr = unwrapFirestoreException(error)
                    if (firestoreErr == null && error is CancellationException) throw error
                    emit(null)
                }
        )
    }

    suspend fun getChurchMemberDetail(memberId: String): Result<ChurchMemberDetail?> {
        val docPath = "church_member_details/$memberId"
        return try {
            requireUserId()
            require(memberId.isNotBlank()) { AppLanguageManager.tr("ID ahli tidak sah.", "Invalid member ID.") }
            val snap = db.collection("church_member_details").document(memberId).get().await()
            val detail = if (snap.exists()) {
                snap.toObject(ChurchMemberDetail::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
            Result.success(detail)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }

    suspend fun getAllChurchMemberDetails(
        memberIds: List<String>,
        isBootstrappedAdmin: Boolean
    ): Result<List<ChurchMemberDetail>> {
        val path = "church_member_details"
        return try {
            requireUserId()
            val details = if (isBootstrappedAdmin) {
                val snap = db.collection("church_member_details").get().await()
                snap.toObjects(
                    ChurchMemberDetail::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                memberIds.mapNotNull { id ->
                    if (id.isBlank()) return@mapNotNull null
                    val snap = db.collection("church_member_details").document(id).get().await()
                    if (snap.exists()) {
                        snap.toObject(
                            ChurchMemberDetail::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
            }
            Result.success(details.sortedBy { it.fullName.lowercase() })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun createChurchMember(
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
        memberEmail: String? = null
    ): Result<Unit> {
        var docPath = "church_members"
        return try {
            val uid = requireUserId()
            val cleanName = fullName.trim()
            val cleanMyKad = myKadNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanBirth = birthDateIso.trim()
            val cleanDenom = if (baptismStatus == BaptismStatus.DIBAPTIS) denomination.trim() else ""
            val cleanSpouse = if (maritalStatus == MaritalStatus.BERKAHWIN) {
                spouseName?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
            val cleanSibNo = sibMembershipNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanPhoto = passportPhotoBase64?.trim()?.takeIf { it.isNotEmpty() }
            val cleanEmail = memberEmail?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

            require(cleanName.length in 2..120) {
                AppLanguageManager.tr("Nama penuh mestilah 2 hingga 120 aksara.", "Full name must be 2 to 120 characters.")
            }
            require(cleanMyKad == null || cleanMyKad.length <= 30) {
                AppLanguageManager.tr("Nombor MyKad terlalu panjang (maks 30 aksara).", "MyKad number is too long (max 30 characters).")
            }
            require(cleanBirth.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                AppLanguageManager.tr("Format tarikh lahir tidak sah (YYYY-MM-DD).", "Invalid date of birth format (YYYY-MM-DD).")
            }
            require(calculateAgeFromIsoDate(cleanBirth) != null) {
                AppLanguageManager.tr("Tarikh lahir tidak sah atau melebihi tarikh hari ini.", "Invalid date of birth or date is in the future.")
            }
            if (baptismStatus == BaptismStatus.DIBAPTIS) {
                require(cleanDenom.length in 1..80) {
                    AppLanguageManager.tr("Sila masukkan nama denominasi gereja (maks 80 aksara).", "Please enter church denomination (max 80 characters).")
                }
            }
            require(cleanSpouse == null || cleanSpouse.length <= 120) {
                AppLanguageManager.tr("Nama pasangan terlalu panjang (maks 120 aksara).", "Spouse name is too long (max 120 characters).")
            }
            require(cleanSibNo == null || cleanSibNo.length <= 40) {
                AppLanguageManager.tr("Nombor keanggotaan SIB terlalu panjang (maks 40 aksara).", "SIB membership number is too long (max 40 characters).")
            }
            require(cleanPhoto == null || cleanPhoto.length <= 160_000) {
                AppLanguageManager.tr("Saiz foto pasport terlalu besar. Sila pilih foto lain.", "Passport photo is too large. Please pick another photo.")
            }
            require(cleanEmail == null || cleanEmail.length <= 160) {
                AppLanguageManager.tr("Alamat emel terlalu panjang (maks 160 aksara).", "Email address is too long (max 160 characters).")
            }

            // If Admin entered an email that already matches an existing member record (e.g. user self-registered first),
            // update that existing record instead of creating a duplicate entry in the Admin Members list.
            if (cleanEmail != null) {
                val existingMatchId = runCatching {
                    db.collection("church_member_details")
                        .whereEqualTo("memberEmail", cleanEmail)
                        .limit(1)
                        .get()
                        .await()
                        .documents
                        .firstOrNull()
                        ?.id
                }.getOrNull()
                if (!existingMatchId.isNullOrBlank()) {
                    return updateChurchMember(
                        memberId = existingMatchId,
                        fullName = cleanName,
                        myKadNumber = cleanMyKad,
                        gender = gender,
                        birthDateIso = cleanBirth,
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

            val publicRef = db.collection("church_members").document()
            val detailRef = db.collection("church_member_details").document(publicRef.id)
            docPath = publicRef.path

            val publicPayload = buildMap<String, Any?> {
                put("fullName", cleanName)
                put("gender", gender.code)
                put("baptismStatus", baptismStatus.code)
                if (cleanPhoto != null) put("passportPhotoBase64", cleanPhoto)
                put("published", true)
                put("authorId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            val detailPayload = buildMap<String, Any?> {
                put("fullName", cleanName)
                if (cleanMyKad != null) put("myKadNumber", cleanMyKad)
                put("gender", gender.code)
                put("birthDate", cleanBirth)
                put("baptismStatus", baptismStatus.code)
                put("denomination", cleanDenom)
                put("maritalStatus", maritalStatus.code)
                if (cleanSpouse != null) put("spouseName", cleanSpouse)
                if (cleanSibNo != null) put("sibMembershipNumber", cleanSibNo)
                if (cleanPhoto != null) put("passportPhotoBase64", cleanPhoto)
                if (cleanEmail != null) put("memberEmail", cleanEmail)
                put("authorId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }

            val batch = db.batch()
            batch.set(publicRef, publicPayload)
            batch.set(detailRef, detailPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    /**
     * Allows a signed-in user with a verified Google email to self-register their own
     * church member details directly from the Account tab.
     * Atomically writes /church_members/{member_$uid}, /church_member_details/{member_$uid},
     * and an admin-only MEMBER_REGISTERED notification event.
     */
    suspend fun registerOwnChurchMember(
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
        verifiedMemberEmail: String
    ): Result<Unit> {
        var docPath = "church_members"
        return try {
            val uid = requireUserId()
            val cleanEmail = verifiedMemberEmail.trim().lowercase()
            require(cleanEmail.isNotEmpty() && cleanEmail.length <= 160) {
                AppLanguageManager.tr(
                    "Emel akaun Google yang disahkan diperlukan untuk mendaftar Kad Ahli.",
                    "A verified Google account email is required to register your Member Card."
                )
            }
            val cleanName = fullName.trim()
            val cleanMyKad = myKadNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanBirth = birthDateIso.trim()
            val cleanDenom = if (baptismStatus == BaptismStatus.DIBAPTIS) denomination.trim() else ""
            val cleanSpouse = if (maritalStatus == MaritalStatus.BERKAHWIN) {
                spouseName?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
            val cleanSibNo = sibMembershipNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanPhoto = passportPhotoBase64?.trim()?.takeIf { it.isNotEmpty() }

            require(cleanName.length in 2..120) {
                AppLanguageManager.tr("Nama penuh mestilah 2 hingga 120 aksara.", "Full name must be 2 to 120 characters.")
            }
            require(cleanMyKad == null || cleanMyKad.length <= 30) {
                AppLanguageManager.tr("Nombor MyKad terlalu panjang (maks 30 aksara).", "MyKad number is too long (max 30 characters).")
            }
            require(cleanBirth.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                AppLanguageManager.tr("Format tarikh lahir tidak sah (YYYY-MM-DD).", "Invalid date of birth format (YYYY-MM-DD).")
            }
            require(calculateAgeFromIsoDate(cleanBirth) != null) {
                AppLanguageManager.tr("Tarikh lahir tidak sah atau melebihi tarikh hari ini.", "Invalid date of birth or date is in the future.")
            }
            if (baptismStatus == BaptismStatus.DIBAPTIS) {
                require(cleanDenom.length in 1..80) {
                    AppLanguageManager.tr("Sila masukkan nama denominasi gereja (maks 80 aksara).", "Please enter church denomination (max 80 characters).")
                }
            }
            require(cleanSpouse == null || cleanSpouse.length <= 120) {
                AppLanguageManager.tr("Nama pasangan terlalu panjang (maks 120 aksara).", "Spouse name is too long (max 120 characters).")
            }
            require(cleanSibNo == null || cleanSibNo.length <= 40) {
                AppLanguageManager.tr("Nombor keanggotaan SIB terlalu panjang (maks 40 aksara).", "SIB membership number is too long (max 40 characters).")
            }
            require(cleanPhoto == null || cleanPhoto.length <= 160_000) {
                AppLanguageManager.tr("Saiz foto pasport terlalu besar. Sila pilih foto lain.", "Passport photo is too large. Please pick another photo.")
            }

            val memberId = "member_$uid"
            val publicRef = db.collection("church_members").document(memberId)
            val detailRef = db.collection("church_member_details").document(memberId)
            val notifRef = db.collection("notification_events").document()
            docPath = publicRef.path

            // First check if an Admin already pre-registered this member using their matching Google email,
            // or if this user's own member_$uid document already exists. If so, update it automatically to avoid duplicates.
            val existingByEmailId = runCatching {
                db.collection("church_member_details")
                    .whereEqualTo("memberEmail", cleanEmail)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?.id
            }.getOrNull()
            val targetExistingId = existingByEmailId ?: if (runCatching { detailRef.get().await().exists() }.getOrDefault(false)) {
                memberId
            } else {
                null
            }
            if (targetExistingId != null) {
                return updateChurchMember(
                    memberId = targetExistingId,
                    fullName = cleanName,
                    myKadNumber = cleanMyKad,
                    gender = gender,
                    birthDateIso = cleanBirth,
                    baptismStatus = baptismStatus,
                    denomination = cleanDenom,
                    maritalStatus = maritalStatus,
                    spouseName = cleanSpouse,
                    sibMembershipNumber = cleanSibNo,
                    passportPhotoBase64 = cleanPhoto,
                    memberEmail = cleanEmail
                )
            }

            val publicPayload = buildMap<String, Any?> {
                put("fullName", cleanName)
                put("gender", gender.code)
                put("baptismStatus", baptismStatus.code)
                if (cleanPhoto != null) put("passportPhotoBase64", cleanPhoto)
                put("published", true)
                put("authorId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            val detailPayload = buildMap<String, Any?> {
                put("fullName", cleanName)
                if (cleanMyKad != null) put("myKadNumber", cleanMyKad)
                put("gender", gender.code)
                put("birthDate", cleanBirth)
                put("baptismStatus", baptismStatus.code)
                put("denomination", cleanDenom)
                put("maritalStatus", maritalStatus.code)
                if (cleanSpouse != null) put("spouseName", cleanSpouse)
                if (cleanSibNo != null) put("sibMembershipNumber", cleanSibNo)
                if (cleanPhoto != null) put("passportPhotoBase64", cleanPhoto)
                put("memberEmail", cleanEmail)
                put("authorId", uid)
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            val adminNotifPayload = mapOf(
                "title" to AppLanguageManager.tr("Ahli Baharu Berdaftar", "New Member Registered"),
                "body" to AppLanguageManager.tr(
                    "$cleanName telah mengisi maklumat ahli SIB Toburon.",
                    "$cleanName has registered their SIB Toburon member details."
                ).take(320),
                "eventType" to BulletinNotificationManager.TYPE_MEMBER_REGISTERED,
                "targetId" to memberId,
                "topic" to BulletinNotificationManager.TOPIC_ADMIN_MEMBERS,
                "published" to false,
                "authorId" to uid,
                "createdAt" to FieldValue.serverTimestamp()
            )

            val batch = db.batch()
            batch.set(publicRef, publicPayload)
            batch.set(detailRef, detailPayload)
            batch.set(notifRef, adminNotifPayload)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateChurchMember(
        memberId: String,
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
        memberEmail: String? = null
    ): Result<Unit> {
        val docPath = "church_members/$memberId"
        return try {
            requireUserId()
            require(memberId.isNotBlank()) { AppLanguageManager.tr("ID ahli tidak sah.", "Invalid member ID.") }
            val cleanName = fullName.trim()
            val cleanMyKad = myKadNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanBirth = birthDateIso.trim()
            val cleanDenom = if (baptismStatus == BaptismStatus.DIBAPTIS) denomination.trim() else ""
            val cleanSpouse = if (maritalStatus == MaritalStatus.BERKAHWIN) {
                spouseName?.trim()?.takeIf { it.isNotEmpty() }
            } else {
                null
            }
            val cleanSibNo = sibMembershipNumber?.trim()?.takeIf { it.isNotEmpty() }
            val cleanPhoto = passportPhotoBase64?.trim()?.takeIf { it.isNotEmpty() }
            val cleanEmail = memberEmail?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

            require(cleanName.length in 2..120) {
                AppLanguageManager.tr("Nama penuh mestilah 2 hingga 120 aksara.", "Full name must be 2 to 120 characters.")
            }
            require(cleanMyKad == null || cleanMyKad.length <= 30) {
                AppLanguageManager.tr("Nombor MyKad terlalu panjang (maks 30 aksara).", "MyKad number is too long (max 30 characters).")
            }
            require(cleanBirth.matches(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$"))) {
                AppLanguageManager.tr("Format tarikh lahir tidak sah (YYYY-MM-DD).", "Invalid date of birth format (YYYY-MM-DD).")
            }
            require(calculateAgeFromIsoDate(cleanBirth) != null) {
                AppLanguageManager.tr("Tarikh lahir tidak sah atau melebihi tarikh hari ini.", "Invalid date of birth or date is in the future.")
            }
            if (baptismStatus == BaptismStatus.DIBAPTIS) {
                require(cleanDenom.length in 1..80) {
                    AppLanguageManager.tr("Sila masukkan nama denominasi gereja (maks 80 aksara).", "Please enter church denomination (max 80 characters).")
                }
            }
            require(cleanSpouse == null || cleanSpouse.length <= 120) {
                AppLanguageManager.tr("Nama pasangan terlalu panjang (maks 120 aksara).", "Spouse name is too long (max 120 characters).")
            }
            require(cleanSibNo == null || cleanSibNo.length <= 40) {
                AppLanguageManager.tr("Nombor keanggotaan SIB terlalu panjang (maks 40 aksara).", "SIB membership number is too long (max 40 characters).")
            }
            require(cleanPhoto == null || cleanPhoto.length <= 160_000) {
                AppLanguageManager.tr("Saiz foto pasport terlalu besar. Sila pilih foto lain.", "Passport photo is too large. Please pick another photo.")
            }
            require(cleanEmail == null || cleanEmail.length <= 160) {
                AppLanguageManager.tr("Alamat emel terlalu panjang (maks 160 aksara).", "Email address is too long (max 160 characters).")
            }

            val publicRef = db.collection("church_members").document(memberId)
            val detailRef = db.collection("church_member_details").document(memberId)

            val batch = db.batch()
            batch.update(
                publicRef,
                mapOf(
                    "fullName" to cleanName,
                    "gender" to gender.code,
                    "baptismStatus" to baptismStatus.code,
                    "passportPhotoBase64" to cleanPhoto,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                detailRef,
                mapOf(
                    "fullName" to cleanName,
                    "myKadNumber" to cleanMyKad,
                    "gender" to gender.code,
                    "birthDate" to cleanBirth,
                    "baptismStatus" to baptismStatus.code,
                    "denomination" to cleanDenom,
                    "maritalStatus" to maritalStatus.code,
                    "spouseName" to cleanSpouse,
                    "sibMembershipNumber" to cleanSibNo,
                    "passportPhotoBase64" to cleanPhoto,
                    "memberEmail" to cleanEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteChurchMember(memberId: String): Result<Unit> {
        val docPath = "church_members/$memberId"
        return try {
            requireUserId()
            require(memberId.isNotBlank()) { AppLanguageManager.tr("ID ahli tidak sah.", "Invalid member ID.") }
            val batch = db.batch()
            batch.delete(db.collection("church_members").document(memberId))
            batch.delete(db.collection("church_member_details").document(memberId))
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- APP VERSION CONFIG (/app_config/version) ---

    fun observeAppVersionConfig(uid: String?): Flow<AppVersionConfig?> =
        db.collection("app_config").document("version")
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(AppVersionConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logStreamErrorIfActive(uid, error, OperationType.GET, "app_config/version")
                emit(null)
            }

    suspend fun saveAppVersionConfig(
        latestVersionCode: Int,
        latestVersionName: String,
        releaseNotes: String,
        updateUrl: String,
        forceUpdate: Boolean
    ): Result<Unit> {
        val docPath = "app_config/version"
        return try {
            val uid = requireUserId()
            val cleanName = latestVersionName.trim()
            val cleanNotes = releaseNotes.trim()
            val cleanUrl = updateUrl.trim()

            require(latestVersionCode in 1..1_000_000) {
                AppLanguageManager.tr("Kod versi tidak sah (1-1000000).", "Invalid version code (1-1000000).")
            }
            require(cleanName.length in 1..32) {
                AppLanguageManager.tr("Nama versi wajib diisi (maks 32 aksara).", "Version name is required (max 32 characters).")
            }
            require(cleanNotes.length <= 1000) {
                AppLanguageManager.tr("Nota perubahan maks 1000 aksara.", "Release notes max 1000 characters.")
            }
            require(cleanUrl.length <= 500 && (cleanUrl.isEmpty() || cleanUrl.startsWith("https://"))) {
                AppLanguageManager.tr("Pautan kemas kini mesti bermula dengan https://", "Update URL must start with https://")
            }

            db.collection("app_config").document("version")
                .set(
                    mapOf(
                        "latestVersionCode" to latestVersionCode,
                        "latestVersionName" to cleanName,
                        "releaseNotes" to cleanNotes,
                        "updateUrl" to cleanUrl,
                        "forceUpdate" to forceUpdate,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- WEB MEMBER REGISTRATION LINK & QR CONFIG (/app_config/member_registration) ---

    fun observeMemberRegistrationConfig(uid: String?, isAdmin: Boolean): Flow<MemberRegistrationConfig?> = flow {
        if (uid.isNullOrBlank() || !isAdmin) {
            emit(null)
            return@flow
        }
        val docPath = "app_config/member_registration"
        emitAll(
            db.collection("app_config").document("member_registration")
                .snapshots()
                .map { snapshot ->
                    if (!snapshot.exists()) null
                    else snapshot.toObject(
                        MemberRegistrationConfig::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.GET, docPath)
                    emit(null)
                }
        )
    }

    suspend fun saveMemberRegistrationConfig(
        portalUrl: String,
        accessToken: String,
        enabled: Boolean
    ): Result<Unit> {
        val docPath = "app_config/member_registration"
        return try {
            val uid = requireUserId()
            val cleanUrl = portalUrl.trim().trimEnd('/')
            val cleanToken = accessToken.trim()

            require(cleanUrl.length in 10..500 && cleanUrl.startsWith("https://")) {
                AppLanguageManager.tr(
                    "Pautan portal pendaftaran mesti bermula dengan https:// (10–500 aksara).",
                    "Registration portal URL must start with https:// (10–500 characters)."
                )
            }
            require(cleanToken.matches(Regex("^[a-zA-Z0-9_\\-]{16,64}$"))) {
                AppLanguageManager.tr(
                    "Token akses pendaftaran tidak sah (16–64 aksara alfanumerik).",
                    "Invalid registration access token (16–64 alphanumeric characters)."
                )
            }

            db.collection("app_config").document("member_registration")
                .set(
                    mapOf(
                        "portalUrl" to cleanUrl,
                        "accessToken" to cleanToken,
                        "enabled" to enabled,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun ensureMemberRegistrationConfig(): Result<MemberRegistrationConfig> {
        val docPath = "app_config/member_registration"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("app_config").document("member_registration")
            val snap = docRef.get().await()
            val existing = if (snap.exists()) {
                snap.toObject(
                    MemberRegistrationConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            if (existing != null && existing.accessToken.isNotBlank() && existing.portalUrl.startsWith("https://")) {
                Result.success(existing)
            } else {
                val portal = existing?.portalUrl?.takeIf { it.startsWith("https://") }
                    ?: MemberRegistrationConfig.DEFAULT_PORTAL_URL
                val token = existing?.accessToken?.takeIf { it.matches(Regex("^[a-zA-Z0-9_\\-]{16,64}$")) }
                    ?: MemberQrUtils.generateRegistrationAccessToken()
                val enabled = existing?.enabled ?: true
                docRef.set(
                    mapOf(
                        "portalUrl" to portal,
                        "accessToken" to token,
                        "enabled" to enabled,
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(
                    MemberRegistrationConfig(
                        id = "member_registration",
                        portalUrl = portal,
                        accessToken = token,
                        enabled = enabled,
                        updatedBy = uid
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    // --- DYNAMIC NEWS & SCHEDULE CATEGORIES ---

    fun observeNewsCategories(uid: String?): Flow<List<DynamicCategoryItem>> =
        observeDynamicCategories(
            uid = uid,
            collection = COLLECTION_NEWS_CATEGORIES,
            defaults = NewsCategory.defaultCategoryItems()
        )

    fun observeScheduleCategories(uid: String?): Flow<List<DynamicCategoryItem>> =
        observeDynamicCategories(
            uid = uid,
            collection = COLLECTION_SCHEDULE_CATEGORIES,
            defaults = ScheduleCategory.defaultCategoryItems()
        )

    private fun observeDynamicCategories(
        uid: String?,
        collection: String,
        defaults: List<DynamicCategoryItem>
    ): Flow<List<DynamicCategoryItem>> =
        db.collection(collection)
            .snapshots()
            .map { snapshot ->
                if (snapshot.documents.isEmpty()) {
                    defaults
                } else {
                    snapshot.toObjects(
                        DynamicCategoryItem::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                        .filter { it.resolvedCode().isNotBlank() }
                        .sortedWith(
                            compareBy<DynamicCategoryItem> { it.sortOrder }
                                .thenBy { it.labelMs.lowercase() }
                        )
                        .ifEmpty { defaults }
                }
            }
            .catch { error ->
                logStreamErrorIfActive(uid, error, OperationType.LIST, collection)
                emit(defaults)
            }

    suspend fun saveCategoryItem(
        collection: String,
        defaults: List<DynamicCategoryItem>,
        existingCode: String?,
        labelMs: String,
        labelEn: String
    ): Result<Unit> {
        var docPath = collection
        return try {
            val uid = requireUserId()
            val cleanMs = labelMs.trim().take(60)
            val cleanEn = labelEn.trim().ifBlank { cleanMs }.take(60)

            require(cleanMs.length in 1..60) {
                AppLanguageManager.tr("Nama kategori wajib diisi (1–60 aksara).", "Category name is required (1–60 characters).")
            }
            require(cleanEn.length in 1..60) {
                AppLanguageManager.tr("Nama kategori (Inggeris) mestilah 1–60 aksara.", "English category name must be 1–60 characters.")
            }

            val existingSnap = db.collection(collection).get().await()
            if (existingSnap.isEmpty) {
                val batch = db.batch()
                var maxOrder = 0
                var updatedExisting = false
                defaults.forEachIndexed { idx, def ->
                    val code = def.resolvedCode()
                    val docRef = db.collection(collection).document(code)
                    val isEditedDefault = !existingCode.isNullOrBlank() && code.equals(existingCode, ignoreCase = true)
                    if (isEditedDefault) updatedExisting = true
                    batch.set(
                        docRef,
                        mapOf(
                            "code" to code,
                            "labelMs" to if (isEditedDefault) cleanMs else def.labelMs,
                            "labelEn" to if (isEditedDefault) cleanEn else def.labelEn,
                            "sortOrder" to idx,
                            "updatedBy" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    maxOrder = idx + 1
                }
                if (existingCode.isNullOrBlank() || !updatedExisting) {
                    val code = existingCode?.trim()?.takeIf { it.isNotBlank() }
                        ?: generateUniqueCategoryCode(cleanMs, defaults.map { it.resolvedCode() }.toSet())
                    val docRef = db.collection(collection).document(code)
                    docPath = docRef.path
                    batch.set(
                        docRef,
                        mapOf(
                            "code" to code,
                            "labelMs" to cleanMs,
                            "labelEn" to cleanEn,
                            "sortOrder" to maxOrder,
                            "updatedBy" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            } else if (!existingCode.isNullOrBlank()) {
                val cleanCode = existingCode.trim()
                val docRef = db.collection(collection).document(cleanCode)
                docPath = docRef.path
                val existingDoc = existingSnap.documents.firstOrNull { it.id == cleanCode }
                if (existingDoc != null) {
                    docRef.update(
                        mapOf(
                            "labelMs" to cleanMs,
                            "labelEn" to cleanEn,
                            "updatedBy" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    val nextOrder = existingSnap.documents.size
                    docRef.set(
                        mapOf(
                            "code" to cleanCode,
                            "labelMs" to cleanMs,
                            "labelEn" to cleanEn,
                            "sortOrder" to nextOrder,
                            "updatedBy" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                val existingCodes = existingSnap.documents.map { it.id }.toSet()
                val newCode = generateUniqueCategoryCode(cleanMs, existingCodes)
                val maxSortOrder = existingSnap.documents.maxOfOrNull { doc ->
                    (doc.get("sortOrder") as? Number)?.toInt() ?: 0
                } ?: existingSnap.documents.size
                val docRef = db.collection(collection).document(newCode)
                docPath = docRef.path
                docRef.set(
                    mapOf(
                        "code" to newCode,
                        "labelMs" to cleanMs,
                        "labelEn" to cleanEn,
                        "sortOrder" to (maxSortOrder + 1),
                        "updatedBy" to uid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCategoryItem(
        collection: String,
        defaults: List<DynamicCategoryItem>,
        code: String
    ): Result<Unit> {
        var docPath = "$collection/$code"
        return try {
            val uid = requireUserId()
            val cleanCode = code.trim()
            require(cleanCode.isNotBlank()) {
                AppLanguageManager.tr("Kod kategori tidak sah.", "Invalid category code.")
            }

            val existingSnap = db.collection(collection).get().await()
            if (existingSnap.isEmpty) {
                val remaining = defaults.filterNot { it.resolvedCode().equals(cleanCode, ignoreCase = true) }
                require(remaining.isNotEmpty()) {
                    AppLanguageManager.tr("Sekurang-kurangnya satu kategori mesti dikekalkan.", "At least one category must remain.")
                }
                val batch = db.batch()
                remaining.forEachIndexed { idx, def ->
                    val defCode = def.resolvedCode()
                    batch.set(
                        db.collection(collection).document(defCode),
                        mapOf(
                            "code" to defCode,
                            "labelMs" to def.labelMs,
                            "labelEn" to def.labelEn,
                            "sortOrder" to idx,
                            "updatedBy" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            } else {
                val remainingDocs = existingSnap.documents.filterNot { it.id.equals(cleanCode, ignoreCase = true) }
                require(remainingDocs.isNotEmpty()) {
                    AppLanguageManager.tr("Sekurang-kurangnya satu kategori mesti dikekalkan.", "At least one category must remain.")
                }
                val docRef = db.collection(collection).document(cleanCode)
                docPath = docRef.path
                docRef.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private fun logStreamErrorIfActive(
        uid: String?,
        error: Throwable,
        op: OperationType,
        path: String
    ) {
        val firestoreError = unwrapFirestoreException(error)
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && (uid == null || auth.currentUser?.uid == uid)) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "Raxxon47@gmail.com"
        const val COLLECTION_NEWS_CATEGORIES = "news_categories"
        const val COLLECTION_SCHEDULE_CATEGORIES = "schedule_categories"
    }
}
