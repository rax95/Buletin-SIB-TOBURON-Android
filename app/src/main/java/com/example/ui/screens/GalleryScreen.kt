package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.GalleryCategory
import com.example.data.GalleryPhoto
import com.example.data.UiState
import com.example.ui.components.BulletinCoverImage
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LiveSyncStatusPill

@Composable
fun GalleryScreen(
    galleryState: UiState<List<GalleryPhoto>>,
    selectedCategory: GalleryCategory?,
    likedPhotoIds: Set<String>,
    isAdmin: Boolean,
    onSelectCategory: (GalleryCategory?) -> Unit,
    onToggleLike: (GalleryPhoto) -> Unit,
    onEditPhoto: (GalleryPhoto) -> Unit,
    onDeletePhoto: (GalleryPhoto) -> Unit,
    onSeedStarter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allPhotos = (galleryState as? UiState.Success)?.data ?: emptyList()
    val filteredPhotos = remember(allPhotos, selectedCategory) {
        allPhotos.filter { photo ->
            selectedCategory == null || photo.resolvedCategory() == selectedCategory
        }
    }

    var lightboxStartIndex by remember { mutableStateOf<Int?>(null) }

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
                                text = "Galeri Foto Interaktif",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Ketuk foto untuk memperbesar, menggeser album, dan memberi apresiasi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        LiveSyncStatusPill()
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CategoryFilterRow(
                        items = GalleryCategory.entries,
                        selected = selectedCategory,
                        labelProvider = { it.label },
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
                                title = if (galleryState.data.isEmpty()) stringResource(R.string.empty_gallery_title)
                                else "Tidak Ada Foto di Kategori Ini",
                                description = if (galleryState.data.isEmpty()) stringResource(R.string.empty_gallery_desc)
                                else "Pilih kategori lain untuk melihat dokumentasi jemaat SIB Toburon.",
                                isAdmin = isAdmin && galleryState.data.isEmpty(),
                                onSeedStarter = onSeedStarter
                            )
                        }
                    } else {
                        itemsIndexed(filteredPhotos, key = { _, photo -> photo.id }) { index, photo ->
                            GalleryGridCard(
                                photo = photo,
                                isLiked = likedPhotoIds.contains(photo.id),
                                onClick = { lightboxStartIndex = index },
                                onToggleLike = { onToggleLike(photo) }
                            )
                        }
                    }
                }
            }
        }
    }

    val activeIndex = lightboxStartIndex
    if (activeIndex != null && filteredPhotos.isNotEmpty()) {
        val safeIndex = activeIndex.coerceIn(0, filteredPhotos.lastIndex)
        InteractivePhotoLightbox(
            photos = filteredPhotos,
            initialIndex = safeIndex,
            likedPhotoIds = likedPhotoIds,
            isAdmin = isAdmin,
            onDismiss = { lightboxStartIndex = null },
            onToggleLike = onToggleLike,
            onEditPhoto = { photo ->
                lightboxStartIndex = null
                onEditPhoto(photo)
            },
            onDeletePhoto = { photo ->
                onDeletePhoto(photo)
                if (filteredPhotos.size <= 1) lightboxStartIndex = null
            }
        )
    }
}

@Composable
private fun GalleryGridCard(
    photo: GalleryPhoto,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit
) {
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
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = photo.resolvedCategory().label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
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
                        contentDescription = stringResource(R.string.cd_like_photo),
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
private fun InteractivePhotoLightbox(
    photos: List<GalleryPhoto>,
    initialIndex: Int,
    likedPhotoIds: Set<String>,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onToggleLike: (GalleryPhoto) -> Unit,
    onEditPhoto: (GalleryPhoto) -> Unit,
    onDeletePhoto: (GalleryPhoto) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BackHandler { onDismiss() }

        val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
        val currentPhoto = photos.getOrNull(pagerState.currentPage) ?: photos.first()
        val isLiked = likedPhotoIds.contains(currentPhoto.id)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xF50B1110))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val photo = photos[page]
                var scale by remember(photo.id) { mutableFloatStateOf(1f) }
                var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }

                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != page) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(photo.id) {
                            detectTapGestures(
                                onDoubleTap = {
                                    if (scale > 1.1f) {
                                        scale = 1f
                                        offset = Offset.Zero
                                    } else {
                                        scale = 2.4f
                                    }
                                }
                            )
                        }
                        .pointerInput(photo.id) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 3.5f)
                                scale = newScale
                                offset = if (newScale > 1f) {
                                    Offset(
                                        x = (offset.x + pan.x).coerceIn(-420f, 420f),
                                        y = (offset.y + pan.y).coerceIn(-420f, 420f)
                                    )
                                } else {
                                    Offset.Zero
                                }
                            }
                        }
                ) {
                    BulletinCoverImage(
                        coverPreset = photo.resolvedCoverPreset(),
                        imageUrl = photo.imageUrl,
                        imageBase64 = photo.imageBase64,
                        contentDescription = photo.title,
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

            // Floating Top Controls
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp).testTag("lightbox_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = Color.White
                        )
                    }
                }

                Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.55f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${pagerState.currentPage + 1} / ${photos.size} • Ketuk 2x / Cubit Zoom",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }

                if (isAdmin) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f)) {
                            IconButton(
                                onClick = { onEditPhoto(currentPhoto) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.action_edit), tint = Color.White)
                            }
                        }
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)) {
                            IconButton(
                                onClick = { onDeletePhoto(currentPhoto) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = Color.White)
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // Floating Bottom Editorial Caption & Live Like Bar
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xD9111B18),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentPhoto.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    tint = Color(0xFFF2B86B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "${currentPhoto.photographerName.ifBlank { "Multimedia SIB Toburon" }} • ${currentPhoto.eventDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isLiked) Color(0x33FF6B6B) else Color.White.copy(alpha = 0.14f),
                            modifier = Modifier
                                .clickable { onToggleLike(currentPhoto) }
                                .testTag("lightbox_like_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = stringResource(R.string.cd_like_photo),
                                    tint = if (isLiked) Color(0xFFFF6B6B) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${currentPhoto.likesCount} Apresiasi",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (currentPhoto.caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = currentPhoto.caption,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
