package com.example.data

import android.content.Context
import com.example.R
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
            throw IllegalStateException("Pengguna harus masuk dengan Google sebelum mengakses data.")
        }
        return uid
    }

    private fun isBootstrappedAdminEmail(email: String?, emailVerified: Boolean): Boolean {
        return emailVerified && email?.equals(BOOTSTRAPPED_ADMIN_EMAIL, ignoreCase = true) == true
    }

    // --- REAL-TIME STREAMS (snapshots() with ESTIMATE) ---

    fun observeAdminAccess(uid: String, email: String?, emailVerified: Boolean): Flow<Boolean> {
        if (uid.isBlank()) return flowOf(false)
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

    fun observeNews(uid: String, isAdmin: Boolean, isBootstrappedAdmin: Boolean): Flow<List<NewsArticle>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "news"
        val sourceFlow: Flow<List<NewsArticle>> = when {
            isAdmin && isBootstrappedAdmin -> {
                db.collection("news").snapshots().map { snapshot ->
                    snapshot.toObjects(NewsArticle::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            isAdmin -> {
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

    fun observeSchedules(uid: String, isAdmin: Boolean, isBootstrappedAdmin: Boolean): Flow<List<ChurchSchedule>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "schedules"
        val sourceFlow: Flow<List<ChurchSchedule>> = when {
            isAdmin && isBootstrappedAdmin -> {
                db.collection("schedules").snapshots().map { snapshot ->
                    snapshot.toObjects(ChurchSchedule::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            isAdmin -> {
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
                        compareBy<ChurchSchedule> { it.sortOrder }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .catch { error ->
                    logStreamErrorIfActive(uid, error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeGallery(uid: String, isAdmin: Boolean, isBootstrappedAdmin: Boolean): Flow<List<GalleryPhoto>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "gallery_photos"
        val sourceFlow: Flow<List<GalleryPhoto>> = when {
            isAdmin && isBootstrappedAdmin -> {
                db.collection("gallery_photos").snapshots().map { snapshot ->
                    snapshot.toObjects(GalleryPhoto::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
            }
            isAdmin -> {
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

    fun observeUserBookmarks(uid: String): Flow<List<UserBookmark>> = flow {
        if (uid.isBlank()) {
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

    fun observeLikedPhotoIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) {
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

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        if (uid.isBlank()) {
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

    fun observeAdminMembers(uid: String, isBootstrappedAdmin: Boolean): Flow<List<AdminMember>> = flow {
        if (uid.isBlank()) {
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

    // --- NEWS MUTATIONS (ADMIN) ---

    suspend fun createNews(
        title: String,
        summary: String,
        content: String,
        category: NewsCategory,
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
            val cleanAuthor = authorName.trim().ifBlank { "Admin SIB Toburon" }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }

            require(cleanTitle.length in 3..160) { "Judul berita harus antara 3 hingga 160 karakter." }
            require(cleanSummary.length in 5..320) { "Ringkasan berita harus antara 5 hingga 320 karakter." }
            require(cleanContent.length in 10..5000) { "Isi berita minimal 10 karakter (maks 5000)." }
            if (cleanUrl != null) require(cleanUrl.length <= 1000) { "URL gambar terlalu panjang." }
            if (cleanBase64 != null) require(cleanBase64.length <= 260_000) { "Ukuran gambar terlalu besar." }

            val docRef = db.collection("news").document()
            docPath = docRef.path
            val payload = mapOf<String, Any?>(
                "title" to cleanTitle,
                "summary" to cleanSummary,
                "content" to cleanContent,
                "category" to category.code,
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

            docRef.set(payload).await()
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
        category: NewsCategory,
        authorName: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        pinned: Boolean,
        published: Boolean
    ): Result<Unit> {
        val docPath = "news/$newsId"
        return try {
            requireUserId()
            require(newsId.isNotBlank()) { "ID berita tidak valid." }
            val cleanTitle = title.trim()
            val cleanSummary = summary.trim()
            val cleanContent = content.trim()
            val cleanAuthor = authorName.trim().ifBlank { "Admin SIB Toburon" }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }

            require(cleanTitle.length in 3..160) { "Judul berita harus antara 3 hingga 160 karakter." }
            require(cleanSummary.length in 5..320) { "Ringkasan berita harus antara 5 hingga 320 karakter." }
            require(cleanContent.length in 10..5000) { "Isi berita minimal 10 karakter (maks 5000)." }
            if (cleanUrl != null) require(cleanUrl.length <= 1000) { "URL gambar terlalu panjang." }
            if (cleanBase64 != null) require(cleanBase64.length <= 260_000) { "Ukuran gambar terlalu besar." }

            val updates = mapOf<String, Any?>(
                "title" to cleanTitle,
                "summary" to cleanSummary,
                "content" to cleanContent,
                "category" to category.code,
                "authorName" to cleanAuthor,
                "coverPreset" to coverPreset.code,
                "imageUrl" to cleanUrl,
                "imageBase64" to cleanBase64,
                "pinned" to pinned,
                "published" to published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("news").document(newsId).update(updates).await()
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
            require(newsId.isNotBlank()) { "ID berita tidak valid." }
            db.collection("news").document(newsId).delete().await()
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
        category: ScheduleCategory,
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
            val cleanDate = dateLabel.trim()
            val cleanTime = timeLabel.trim()
            val cleanLocation = location.trim()
            val cleanSpeaker = speaker.trim()

            require(cleanTitle.length in 3..140) { "Nama kegiatan harus 3-140 karakter." }
            require(cleanDesc.length in 1..1500) { "Keterangan kegiatan harus diisi (maks 1500 karakter)." }
            require(cleanDate.length in 3..60) { "Tanggal / hari harus diisi (3-60 karakter)." }
            require(cleanTime.length in 3..40) { "Jam kegiatan harus diisi (3-40 karakter)." }
            require(cleanLocation.length in 2..120) { "Lokasi kegiatan harus diisi (2-120 karakter)." }
            require(cleanSpeaker.length <= 100) { "Nama pengkhotbah/koordinator maks 100 karakter." }
            require(sortOrder in 0L..9_999_999_999_999L) { "Urutan jadwal tidak valid." }

            val docRef = db.collection("schedules").document()
            docPath = docRef.path
            val payload = mapOf(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category.code,
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
            docRef.set(payload).await()
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
        category: ScheduleCategory,
        dateLabel: String,
        timeLabel: String,
        location: String,
        speaker: String,
        sortOrder: Long,
        recurring: Boolean,
        published: Boolean
    ): Result<Unit> {
        val docPath = "schedules/$scheduleId"
        return try {
            requireUserId()
            require(scheduleId.isNotBlank()) { "ID jadwal tidak valid." }
            val cleanTitle = title.trim()
            val cleanDesc = description.trim()
            val cleanDate = dateLabel.trim()
            val cleanTime = timeLabel.trim()
            val cleanLocation = location.trim()
            val cleanSpeaker = speaker.trim()

            require(cleanTitle.length in 3..140) { "Nama kegiatan harus 3-140 karakter." }
            require(cleanDesc.length in 1..1500) { "Keterangan kegiatan harus diisi." }
            require(cleanDate.length in 3..60) { "Tanggal / hari harus diisi." }
            require(cleanTime.length in 3..40) { "Jam kegiatan harus diisi." }
            require(cleanLocation.length in 2..120) { "Lokasi kegiatan harus diisi." }
            require(cleanSpeaker.length <= 100) { "Nama pengkhotbah maks 100 karakter." }
            require(sortOrder in 0L..9_999_999_999_999L) { "Urutan jadwal tidak valid." }

            val updates = mapOf(
                "title" to cleanTitle,
                "description" to cleanDesc,
                "category" to category.code,
                "dateLabel" to cleanDate,
                "timeLabel" to cleanTime,
                "location" to cleanLocation,
                "speaker" to cleanSpeaker,
                "sortOrder" to sortOrder,
                "recurring" to recurring,
                "published" to published,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("schedules").document(scheduleId).update(updates).await()
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
            require(scheduleId.isNotBlank()) { "ID jadwal tidak valid." }
            db.collection("schedules").document(scheduleId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
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
        photographerName: String,
        published: Boolean
    ): Result<Unit> {
        var docPath = "gallery_photos"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim()
            val cleanCaption = caption.trim()
            val cleanDate = eventDate.trim()
            val cleanPhotoBy = photographerName.trim().ifBlank { "Tim Multimedia SIB Toburon" }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }

            require(cleanTitle.length in 2..120) { "Judul foto harus 2-120 karakter." }
            require(cleanCaption.length <= 600) { "Keterangan foto maksimal 600 karakter." }
            require(cleanDate.length in 2..50) { "Tanggal acara harus diisi (2-50 karakter)." }
            if (cleanUrl != null) require(cleanUrl.length <= 1000) { "URL gambar terlalu panjang." }
            if (cleanBase64 != null) require(cleanBase64.length <= 260_000) { "Ukuran gambar terlalu besar." }

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
        photographerName: String,
        published: Boolean
    ): Result<Unit> {
        val docPath = "gallery_photos/$photoId"
        return try {
            requireUserId()
            require(photoId.isNotBlank()) { "ID foto tidak valid." }
            val cleanTitle = title.trim()
            val cleanCaption = caption.trim()
            val cleanDate = eventDate.trim()
            val cleanPhotoBy = photographerName.trim().ifBlank { "Tim Multimedia SIB Toburon" }.take(80)
            val cleanUrl = imageUrl?.trim()?.takeIf { it.isNotBlank() }
            val cleanBase64 = imageBase64?.takeIf { it.isNotBlank() }

            require(cleanTitle.length in 2..120) { "Judul foto harus 2-120 karakter." }
            require(cleanCaption.length <= 600) { "Keterangan foto maksimal 600 karakter." }
            require(cleanDate.length in 2..50) { "Tanggal acara harus diisi." }

            val updates = mapOf<String, Any?>(
                "title" to cleanTitle,
                "caption" to cleanCaption,
                "category" to category.code,
                "eventDate" to cleanDate,
                "coverPreset" to coverPreset.code,
                "imageUrl" to cleanUrl,
                "imageBase64" to cleanBase64,
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
            require(photoId.isNotBlank()) { "ID foto tidak valid." }
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
            require(photoId.isNotBlank()) { "ID foto tidak valid." }
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
            require(itemId.isNotBlank()) { "ID item tidak valid." }
            val bookmarkDocId = "${itemType.lowercase()}_$itemId"
            val docRef = db.collection("users").document(uid).collection("bookmarks").document(bookmarkDocId)
            docPath = docRef.path
            if (currentlyBookmarked) {
                docRef.delete().await()
            } else {
                val payload = mapOf(
                    "itemId" to itemId,
                    "itemType" to itemType,
                    "title" to title.trim().take(160).ifBlank { "Item Tersimpan" },
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

    suspend fun saveUserProfile(
        displayName: String,
        fellowshipGroup: FellowshipGroup,
        existingProfile: UserProfile?
    ): Result<Unit> {
        var docPath = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().ifBlank { "Jemaat SIB Toburon" }.take(80)
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            if (existingProfile == null) {
                docRef.set(
                    mapOf(
                        "displayName" to cleanName,
                        "fellowshipGroup" to fellowshipGroup.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "displayName" to cleanName,
                        "fellowshipGroup" to fellowshipGroup.code,
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

    // --- DELEGATED ADMIN MANAGEMENT ---

    suspend fun addAdminMember(targetUid: String, displayName: String, roleTitle: String): Result<Unit> {
        val cleanUid = targetUid.trim()
        val docPath = "admins/$cleanUid"
        return try {
            val uid = requireUserId()
            require(cleanUid.matches(Regex("^[a-zA-Z0-9_\\-]+$")) && cleanUid.length in 1..128) {
                "Format UID tidak valid (hanya huruf, angka, _ atau -)."
            }
            val cleanName = displayName.trim()
            val cleanRole = roleTitle.trim()
            require(cleanName.length in 1..80) { "Nama pengurus harus diisi (maks 80 karakter)." }
            require(cleanRole.length in 1..60) { "Jabatan pelayanan harus diisi (maks 60 karakter)." }

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
            require(targetUid.isNotBlank()) { "UID tidak valid." }
            db.collection("admins").document(targetUid).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    // --- ADMIN STARTER CONTENT SEEDER ---

    suspend fun seedStarterBulletinData(authorName: String): Result<Unit> {
        return try {
            val uid = requireUserId()
            val cleanAuthor = authorName.trim().ifBlank { "Sekretariat SIB Toburon" }.take(80)
            val batch = db.batch()

            val starterNews = listOf(
                mapOf(
                    "title" to "Ibadah Syukur & Perjamuan Kasih Jemaat SIB Toburon",
                    "summary" to "Seluruh jemaat diundang hadir dalam Ibadah Raya Minggu yang disertai perjamuan kasih bersama di halaman gereja.",
                    "content" to "Shalom jemaat yang dikasihi Tuhan,\n\nDengan penuh ucapan syukur, Majelis Jemaat SIB Toburon mengundang seluruh keluarga, pemuda, dan anak Sekolah Minggu untuk menghadiri Ibadah Raya Syukur hari Minggu ini pukul 08:00 WITA.\n\nUsai ibadah, kita akan melanjutkan kebersamaan melalui Perjamuan Kasih dan ramah tamah di balai persekutuan. Mari datang dengan hati yang rindu memuji Tuhan dan saling menguatkan satu sama lain.",
                    "category" to NewsCategory.WARTA.code,
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "coverPreset" to CoverPreset.SANCTUARY.code,
                    "pinned" to true,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Latihan Gabungan Tim Pujian & Paduan Suara Jemaat",
                    "summary" to "Persiapan pujian khusus menyambut minggu pelayanan dan perayaan bulan keluarga SIB Toburon.",
                    "content" to "Diinformasikan kepada seluruh anggota tim musik, singers, dan paduan suara SIB Toburon bahwa latihan gabungan akan diadakan pada hari Jumat pukul 19:00 WITA di ruang ibadah utama.\n\nDiharapkan seluruh pelayan pujian hadir tepat waktu membawa catatan lagu terbaru. Tuhan memberkati pelayanan kita bersama.",
                    "category" to NewsCategory.PELAYANAN.code,
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "coverPreset" to CoverPreset.WORSHIP.code,
                    "pinned" to false,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Persekutuan Padang & Bakti Sosial Pemuda SIB Toburon",
                    "summary" to "Kaum muda SIB Toburon menggelar aksi kasih dan persekutuan alam terbuka bersama jemaat.",
                    "content" to "Persekutuan Pemuda & Remaja (PPR) SIB Toburon akan melaksanakan kegiatan Persekutuan Padang sekaligus penyaluran paket kasih bagi warga sekitar.\n\nPendaftaran dibuka hingga hari Kamis melalui koordinator sektor masing-masing. Mari ambil bagian membawa sukacita Kristus bagi sesama.",
                    "category" to NewsCategory.PEMUDA.code,
                    "authorId" to uid,
                    "authorName" to cleanAuthor,
                    "coverPreset" to CoverPreset.FELLOWSHIP.code,
                    "pinned" to false,
                    "published" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            starterNews.forEach { payload ->
                batch.set(db.collection("news").document(), payload)
            }

            val starterSchedules = listOf(
                mapOf(
                    "title" to "Ibadah Raya Minggu",
                    "description" to "Ibadah utama seluruh jemaat SIB Toburon disertai pujian paduan suara dan pemberitaan Firman Tuhan.",
                    "category" to ScheduleCategory.IBADAH.code,
                    "dateLabel" to "Setiap Minggu",
                    "timeLabel" to "08:00 - 10:00 WITA",
                    "location" to "Gedung Utama SIB Toburon",
                    "speaker" to "Pdt. Markus L. T.",
                    "sortOrder" to 100L,
                    "recurring" to true,
                    "published" to true,
                    "authorId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Sekolah Minggu Anak & Tunas",
                    "description" to "Pembelajaran Alkitab interaktif, pujian ceria, dan kreativitas untuk anak-anak jemaat.",
                    "category" to ScheduleCategory.SEKOLAH_MINGGU.code,
                    "dateLabel" to "Setiap Minggu",
                    "timeLabel" to "08:30 - 10:00 WITA",
                    "location" to "Ruang Sekolah Minggu SIB Toburon",
                    "speaker" to "Tim Guru Sekolah Minggu",
                    "sortOrder" to 200L,
                    "recurring" to true,
                    "published" to true,
                    "authorId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Persekutuan Doa Malam Jemaat",
                    "description" to "Mendoakan pertumbuhan iman jemaat, keluarga, pelayanan misi, dan kesejahteraan masyarakat.",
                    "category" to ScheduleCategory.DOA.code,
                    "dateLabel" to "Setiap Rabu",
                    "timeLabel" to "19:00 - 20:30 WITA",
                    "location" to "Gedung Utama SIB Toburon",
                    "speaker" to "Majelis & Tim Doa",
                    "sortOrder" to 300L,
                    "recurring" to true,
                    "published" to true,
                    "authorId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Ibadah Pemuda & Remaja (PPR)",
                    "description" to "Pujian akustik, diskusi firman yang relevan bagi generasi muda, dan persekutuan akrab.",
                    "category" to ScheduleCategory.PEMUDA.code,
                    "dateLabel" to "Setiap Sabtu",
                    "timeLabel" to "18:30 - 20:30 WITA",
                    "location" to "Balai Pemuda SIB Toburon",
                    "speaker" to "Pengurus PPR SIB Toburon",
                    "sortOrder" to 400L,
                    "recurring" to true,
                    "published" to true,
                    "authorId" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            starterSchedules.forEach { payload ->
                batch.set(db.collection("schedules").document(), payload)
            }

            val starterPhotos = listOf(
                mapOf(
                    "title" to "Pagi Cerah di Gedung Ibadah SIB Toburon",
                    "caption" to "Suasana teduh gedung gereja SIB Toburon menyambut jemaat sebelum Ibadah Raya dimulai.",
                    "category" to GalleryCategory.IBADAH.code,
                    "eventDate" to "November 2026",
                    "coverPreset" to CoverPreset.SANCTUARY.code,
                    "likesCount" to 0L,
                    "published" to true,
                    "authorId" to uid,
                    "photographerName" to "Tim Multimedia SIB Toburon",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Pujian & Penyembahan Jemaat",
                    "caption" to "Kebersamaan tim pujian dan seluruh jemaat mengangkat pujian syukur di hari Minggu.",
                    "category" to GalleryCategory.IBADAH.code,
                    "eventDate" to "November 2026",
                    "coverPreset" to CoverPreset.WORSHIP.code,
                    "likesCount" to 0L,
                    "published" to true,
                    "authorId" to uid,
                    "photographerName" to "Tim Multimedia SIB Toburon",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                mapOf(
                    "title" to "Persekutuan Padang & Ramah Tamah Keluarga",
                    "caption" to "Momen hangat makan bersama dan persekutuan keluarga besar SIB Toburon di alam terbuka.",
                    "category" to GalleryCategory.PEMUDA.code,
                    "eventDate" to "November 2026",
                    "coverPreset" to CoverPreset.FELLOWSHIP.code,
                    "likesCount" to 0L,
                    "published" to true,
                    "authorId" to uid,
                    "photographerName" to "Tim Dokumentasi PPR",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            starterPhotos.forEach { payload ->
                batch.set(db.collection("gallery_photos").document(), payload)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "seed_starter_bulletin")
            Result.failure(e)
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? =
        generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()

    private fun logStreamErrorIfActive(
        uid: String,
        error: Throwable,
        op: OperationType,
        path: String
    ) {
        val firestoreError = unwrapFirestoreException(error)
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }

    companion object {
        const val BOOTSTRAPPED_ADMIN_EMAIL = "Raxxon47@gmail.com"
    }
}
