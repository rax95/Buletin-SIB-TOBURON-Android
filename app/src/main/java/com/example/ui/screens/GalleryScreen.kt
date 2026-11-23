package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.CoverPreset
import com.example.data.GalleryCategory
import com.example.data.GalleryComment
import com.example.data.GalleryPhoto
import com.example.data.UiState
import com.example.ui.components.BulletinCoverImage
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.CommentReplyTarget
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.GalleryCommentComposerBar
import com.example.ui.components.GalleryCommentsSection
import com.example.ui.components.LiveSyncStatusPill
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch

private data class PhotoSlide(
    val coverPreset: CoverPreset,
    val imageUrl: String?,
    val imageBase64: String?
)

@Composable
fun GalleryScreen(
    galleryState: UiState<List<GalleryPhoto>>,
    selectedCategory: GalleryCategory?,
    likedPhotoIds: Set<String>,
    commentsState: UiState<List<GalleryComment>>,
    isPostingComment: Boolean,
    currentUserId: String?,
    isGuest: Boolean,
    isAdmin: Boolean,
    onSelectCategory: (GalleryCategory?) -> Unit,
    onToggleLike: (GalleryPhoto) -> Unit,
    onObserveComments: (String?) -> Unit,
    onPostComment: (
        photoId: String,
        text: String,
        parentCommentId: String?,
        replyToAuthorName: String?,
        onComplete: () -> Unit
    ) -> Unit,
    onDeleteComment: (photoId: String, commentId: String) -> Unit,
    onDetailVisibilityChange: (Boolean) -> Unit = {},
    onEditPhoto: (GalleryPhoto) -> Unit,
    onDeletePhoto: (GalleryPhoto) -> Unit,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allPhotos = (galleryState as? UiState.Success)?.data ?: emptyList()
    val filteredPhotos = remember(allPhotos, selectedCategory) {
        allPhotos.filter { photo ->
            selectedCategory == null || photo.resolvedCategory() == selectedCategory
        }
    }

    var activePhotoId by remember { mutableStateOf<String?>(null) }
    val selectedPhoto = activePhotoId?.let { id -> filteredPhotos.firstOrNull { it.id == id } }

    LaunchedEffect(selectedPhoto?.id) {
        onObserveComments(selectedPhoto?.id)
        onDetailVisibilityChange(selectedPhoto != null)
    }
    DisposableEffect(Unit) {
        onDispose {
            onObserveComments(null)
            onDetailVisibilityChange(false)
        }
    }

    if (selectedPhoto != null) {
        InteractivePhotoDetailPane(
            photo = selectedPhoto,
            isLiked = likedPhotoIds.contains(selectedPhoto.id),
            commentsState = commentsState,
            isPostingComment = isPostingComment,
            currentUserId = currentUserId,
            isGuest = isGuest,
            isAdmin = isAdmin,
            onDismiss = { activePhotoId = null },
            onToggleLike = { onToggleLike(selectedPhoto) },
            onPostComment = { text, parentId, replyToName, onDone ->
                onPostComment(selectedPhoto.id, text, parentId, replyToName, onDone)
            },
            onDeleteComment = { commentId ->
                onDeleteComment(selectedPhoto.id, commentId)
            },
            onEditPhoto = {
                activePhotoId = null
                onEditPhoto(selectedPhoto)
            },
            onDeletePhoto = {
                activePhotoId = null
                onDeletePhoto(selectedPhoto)
            },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columnCount = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(columnCount) }, key = "gallery_header") {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Galeri Foto Interaktif", "Interactive Photo Gallery"),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = tr(
                                    "Ketik foto untuk zum, leret kiri/kanan lihat album, dan beri tanda suka",
                                    "Tap to zoom, swipe left/right to view album photos, and show appreciation"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        LiveSyncStatusPill()
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val lang = LocalAppLanguage.current
                    CategoryFilterRow(
                        items = GalleryCategory.entries,
                        selected = selectedCategory,
                        labelProvider = { it.localizedLabel(lang) },
                        onSelect = onSelectCategory,
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                }
            }

            when (galleryState) {
                is UiState.Loading -> {
                    item(span = { GridItemSpan(columnCount) }, key = "loading_gallery") {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp)
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    item(span = { GridItemSpan(columnCount) }, key = "error_gallery") {
                        ErrorStateCard(message = galleryState.message)
                    }
                }
                is UiState.Success -> {
                    if (filteredPhotos.isEmpty()) {
                        item(span = { GridItemSpan(columnCount) }, key = "empty_gallery") {
                            EmptyBulletinState(
                                icon = Icons.Filled.PhotoLibrary,
                                title = if (galleryState.data.isEmpty()) {
                                    tr("Galeri Foto Masih Kosong", "Photo Gallery Is Empty")
                                } else {
                                    tr("Tiada Foto dalam Kategori Ini", "No Photos in This Category")
                                },
                                description = if (galleryState.data.isEmpty()) {
                                    tr(
                                        "Dokumentasi kegiatan jemaat SIB Toburon akan dipaparkan secara interaktif di sini.",
                                        "SIB Toburon congregation event photos will be showcased interactively here."
                                    )
                                } else {
                                    tr(
                                        "Pilih kategori lain untuk melihat dokumentasi jemaat SIB Toburon.",
                                        "Select another category to browse SIB Toburon event photos."
                                    )
                                },
                                isAdmin = isAdmin && galleryState.data.isEmpty(),
                                primaryActionLabel = tr("Muat Naik Foto", "Upload Photo"),
                                onPrimaryAction = onAddPhoto
                            )
                        }
                    } else {
                        items(filteredPhotos, key = { it.id }) { photo ->
                            GalleryGridCard(
                                photo = photo,
                                isLiked = likedPhotoIds.contains(photo.id),
                                onClick = { activePhotoId = photo.id },
                                onToggleLike = { onToggleLike(photo) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryGridCard(
    photo: GalleryPhoto,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit
) {
    val totalCount = photo.totalImageCount()

    Card(
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.86f)
            .clickable(onClick = onClick)
            .testTag("gallery_card_${photo.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            BulletinCoverImage(
                coverPreset = photo.resolvedCoverPreset(),
                imageUrl = photo.imageUrl,
                imageBase64 = photo.imageBase64,
                contentDescription = photo.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.28f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.80f)
                            )
                        )
                    )
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.50f)
                ) {
                    Text(
                        text = photo.resolvedCategory().localizedLabel(LocalAppLanguage.current),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (totalCount > 1) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xCC1B4332),
                        modifier = Modifier.testTag("gallery_multi_badge_${photo.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = Color(0xFFF2B86B),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = tr("$totalCount Foto", "$totalCount Photos"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
            ) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = tr("Sukai foto", "Like photo"),
                        tint = if (isLiked) Color(0xFFFF6B6B) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(10.dp)
            ) {
                Text(
                    text = photo.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = photo.eventDate,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (photo.likesCount > 0L) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFFF8787),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = photo.likesCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractivePhotoDetailPane(
    photo: GalleryPhoto,
    isLiked: Boolean,
    commentsState: UiState<List<GalleryComment>>,
    isPostingComment: Boolean,
    currentUserId: String?,
    isGuest: Boolean,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onPostComment: (
        text: String,
        parentCommentId: String?,
        replyToAuthorName: String?,
        onComplete: () -> Unit
    ) -> Unit,
    onDeleteComment: (String) -> Unit,
    onEditPhoto: () -> Unit,
    onDeletePhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val slides = remember(photo.id, photo.imageBase64, photo.extraImagesBase64, photo.imageUrl, photo.coverPreset) {
        val b64Images = photo.allBase64Images()
        if (b64Images.isNotEmpty()) {
            b64Images.map { b64 ->
                PhotoSlide(
                    coverPreset = CoverPreset.CUSTOM,
                    imageUrl = null,
                    imageBase64 = b64
                )
            }
        } else {
            listOf(
                PhotoSlide(
                    coverPreset = photo.resolvedCoverPreset(),
                    imageUrl = photo.imageUrl,
                    imageBase64 = null
                )
            )
        }
    }

    BackHandler { onDismiss() }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { slides.size })
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var commentText by remember(photo.id) { mutableStateOf("") }
    var replyTarget by remember(photo.id) { mutableStateOf<CommentReplyTarget?>(null) }
    val commentInputFocusRequester = remember(photo.id) { FocusRequester() }
    val commentsCount = (commentsState as? UiState.Success)?.data?.size ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF80B1110))
    ) {
        // Fixed Top Controls Bar
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp).testTag("lightbox_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Kembali", "Back"),
                        tint = Color.White
                    )
                }
            }

                Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.14f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (slides.size > 1) Icons.Filled.PhotoLibrary else Icons.Filled.ZoomIn,
                            contentDescription = null,
                            tint = Color(0xFFF2B86B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (slides.size > 1) {
                                "${pagerState.currentPage + 1} / ${slides.size} • ${tr("Leret kiri / kanan", "Swipe left / right")}"
                            } else {
                                tr("Ketik 2x / Cubit Zum", "Double-Tap / Pinch Zoom")
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                            IconButton(
                                onClick = onEditPhoto,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = tr("Edit", "Edit"),
                                    tint = Color.White
                                )
                            }
                        }
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)) {
                            IconButton(
                                onClick = onDeletePhoto,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = tr("Padam", "Delete"),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Scrollable Body: Swipeable Multi-Photo Stage + Thumbnail Strip + Full Description Panel
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val photoStageHeight = (maxHeight * 0.56f).coerceIn(180.dp, 460.dp)
                var isCurrentPhotoZoomed by remember(photo.id) { mutableStateOf(false) }

                LaunchedEffect(pagerState.currentPage) {
                    isCurrentPhotoZoomed = false
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState, enabled = !isCurrentPhotoZoomed)
                        .testTag("lightbox_scrollable_content")
                ) {
                    // Swipeable Photo Pager Stage for this single upload's images
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(photoStageHeight)
                            .background(Color.Black)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            userScrollEnabled = !isCurrentPhotoZoomed,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("lightbox_album_pager")
                        ) { page ->
                            val slide = slides[page]
                            var scale by remember(photo.id, page) { mutableFloatStateOf(1f) }
                            var offset by remember(photo.id, page) { mutableStateOf(Offset.Zero) }
                            var containerSize by remember(photo.id, page) { mutableStateOf(IntSize.Zero) }

                            fun clampOffset(raw: Offset, targetScale: Float, size: IntSize): Offset {
                                if (targetScale <= 1.01f || size.width <= 0 || size.height <= 0) {
                                    return Offset.Zero
                                }
                                val maxX = (size.width * (targetScale - 1f)) / 2f
                                val maxY = (size.height * (targetScale - 1f)) / 2f
                                return Offset(
                                    x = raw.x.coerceIn(-maxX, maxX),
                                    y = raw.y.coerceIn(-maxY, maxY)
                                )
                            }

                            LaunchedEffect(pagerState.currentPage) {
                                if (pagerState.currentPage != page) {
                                    scale = 1f
                                    offset = Offset.Zero
                                }
                            }

                            LaunchedEffect(scale, pagerState.currentPage) {
                                if (pagerState.currentPage == page) {
                                    isCurrentPhotoZoomed = scale > 1.02f
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clipToBounds()
                                    .onSizeChanged { newSize ->
                                        containerSize = newSize
                                        offset = clampOffset(offset, scale, newSize)
                                    }
                                    .pointerInput(photo.id, page) {
                                        detectTapGestures(
                                            onDoubleTap = { tapOffset ->
                                                if (scale > 1.1f) {
                                                    scale = 1f
                                                    offset = Offset.Zero
                                                } else {
                                                    val targetScale = 2.5f
                                                    val center = Offset(
                                                        x = containerSize.width / 2f,
                                                        y = containerSize.height / 2f
                                                    )
                                                    val focalOffset = (center - tapOffset) * (targetScale - 1f)
                                                    scale = targetScale
                                                    offset = clampOffset(focalOffset, targetScale, containerSize)
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(photo.id, page) {
                                        val touchSlop = viewConfiguration.touchSlop
                                        awaitEachGesture {
                                            awaitFirstDown(requireUnconsumed = false)
                                            var pastTouchSlop = false
                                            var totalPan = Offset.Zero
                                            do {
                                                val event = awaitPointerEvent()
                                                val pressedCount = event.changes.count { it.pressed }
                                                if (pressedCount >= 2 || scale > 1.02f) {
                                                    val zoomChange = event.calculateZoom()
                                                    val panChange = event.calculatePan()
                                                    val centroid = event.calculateCentroid(useCurrent = true)
                                                    if (!pastTouchSlop) {
                                                        totalPan += panChange
                                                        if (
                                                            pressedCount >= 2 ||
                                                            kotlin.math.abs(zoomChange - 1f) > 0.01f ||
                                                            totalPan.getDistance() > touchSlop
                                                        ) {
                                                            pastTouchSlop = true
                                                        }
                                                    }
                                                    if (pastTouchSlop && (zoomChange != 1f || panChange != Offset.Zero)) {
                                                        val oldScale = scale
                                                        val newScale = (oldScale * zoomChange).coerceIn(1f, 4f)
                                                        val ratio = if (oldScale > 0f) newScale / oldScale else 1f
                                                        val center = Offset(
                                                            x = containerSize.width / 2f,
                                                            y = containerSize.height / 2f
                                                        )
                                                        val updatedOffset = if (newScale > 1.02f) {
                                                            val focalAnchor = if (centroid != Offset.Unspecified) {
                                                                (centroid - center) * (1f - ratio)
                                                            } else {
                                                                Offset.Zero
                                                            }
                                                            val candidate = (offset * ratio) + focalAnchor + panChange
                                                            clampOffset(candidate, newScale, containerSize)
                                                        } else {
                                                            Offset.Zero
                                                        }
                                                        scale = if (newScale <= 1.02f) 1f else newScale
                                                        offset = updatedOffset
                                                        event.changes.forEach { change ->
                                                            if (change.positionChanged()) change.consume()
                                                        }
                                                    }
                                                }
                                            } while (event.changes.any { it.pressed })
                                        }
                                    }
                            ) {
                                BulletinCoverImage(
                                    coverPreset = slide.coverPreset,
                                    imageUrl = slide.imageUrl,
                                    imageBase64 = slide.imageBase64,
                                    contentDescription = "${photo.title} (${page + 1}/${slides.size})",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                )
                            }
                        }

                        // Subtle Prev/Next arrow controls for multi-photo uploads (hidden while zoomed in)
                        if (slides.size > 1 && !isCurrentPhotoZoomed) {
                            if (pagerState.currentPage > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.48f),
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .padding(start = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        },
                                        modifier = Modifier.size(44.dp).testTag("lightbox_prev_photo")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = tr("Foto sebelumnya", "Previous photo"),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                            if (pagerState.currentPage < slides.lastIndex) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.Black.copy(alpha = 0.48f),
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(end = 8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        },
                                        modifier = Modifier.size(44.dp).testTag("lightbox_next_photo")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = tr("Foto seterusnya", "Next photo"),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // Pagination dots overlay at bottom of photo stage
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 10.dp)
                                    .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                repeat(slides.size) { index ->
                                    val selected = pagerState.currentPage == index
                                    Box(
                                        modifier = Modifier
                                            .size(if (selected) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) Color(0xFFF2B86B)
                                                else Color.White.copy(alpha = 0.45f)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    // Interactive thumbnail selector strip when multiple photos in one upload
                    if (slides.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0E1715))
                                .testTag("lightbox_thumbnail_strip")
                        ) {
                            itemsIndexed(slides) { index, slide ->
                                val isSelected = pagerState.currentPage == index
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFF2B86B)
                                        else Color.White.copy(alpha = 0.2f)
                                    ),
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                ) {
                                    BulletinCoverImage(
                                        coverPreset = slide.coverPreset,
                                        imageUrl = slide.imageUrl,
                                        imageBase64 = slide.imageBase64,
                                        contentDescription = tr("Gambar ${index + 1}", "Photo ${index + 1}"),
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    }

                    // Full Scrollable Photo Details & Description Panel Below the Image
                    Surface(
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = Color(0xFF13201D),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x33F2B86B)
                                    ) {
                                        Text(
                                            text = photo.resolvedCategory().localizedLabel(LocalAppLanguage.current),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF2B86B),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (slides.size > 1) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = tr("${slides.size} Gambar", "${slides.size} Photos"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .clickable {
                                                coroutineScope.launch {
                                                    scrollState.animateScrollTo(scrollState.maxValue)
                                                }
                                            }
                                            .testTag("lightbox_scroll_to_comments_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ChatBubbleOutline,
                                                contentDescription = tr("Lihat komen", "View comments"),
                                                tint = Color(0xFFF2B86B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(5.dp))
                                            Text(
                                                text = commentsCount.toString(),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isLiked) Color(0x33FF6B6B) else Color.White.copy(alpha = 0.14f),
                                        modifier = Modifier
                                            .clickable { onToggleLike() }
                                            .testTag("lightbox_like_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                                contentDescription = tr("Sukai foto", "Like photo"),
                                                tint = if (isLiked) Color(0xFFFF6B6B) else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = tr(
                                                    "${photo.likesCount} Suka",
                                                    "${photo.likesCount} Likes"
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = photo.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color(0xFFF2B86B),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${photo.photographerName.ifBlank { tr("Multimedia SIB Toburon", "SIB Toburon Media") }} • ${photo.eventDate}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }

                            if (photo.caption.isNotBlank()) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = tr("Keterangan Foto", "Photo Caption"),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFF2B86B),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = photo.caption,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.94f),
                                    modifier = Modifier.testTag("lightbox_photo_caption")
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            GalleryCommentsSection(
                                commentsState = commentsState,
                                currentUserId = currentUserId,
                                isGuest = isGuest,
                                isAdmin = isAdmin,
                                onReplyClick = { target ->
                                    replyTarget = target
                                    runCatching { commentInputFocusRequester.requestFocus() }
                                },
                                onDeleteComment = { comment ->
                                    onDeleteComment(comment.id)
                                    if (replyTarget?.rootParentCommentId == comment.id) {
                                        replyTarget = null
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }

            // Fixed Bottom Comment & Reply Composer Bar
            GalleryCommentComposerBar(
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                replyTarget = replyTarget,
                onClearReplyTarget = { replyTarget = null },
                isGuest = isGuest,
                isPosting = isPostingComment,
                inputFocusRequester = commentInputFocusRequester,
                onSendClick = {
                    val trimmed = commentText.trim()
                    if (trimmed.isNotEmpty()) {
                        val currentReply = replyTarget
                        onPostComment(
                            trimmed,
                            currentReply?.rootParentCommentId,
                            currentReply?.replyToAuthorName
                        ) {
                            commentText = ""
                            replyTarget = null
                        }
                    }
                }
            )
        }
}
