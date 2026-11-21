package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.NewsArticle
import com.example.data.NewsCategory
import com.example.data.UiState
import com.example.ui.components.BulletinCoverImage
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EditorialSearchBar
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LiveSyncStatusPill
import com.example.ui.components.formatIndonesianTimestamp

@Composable
fun NewsScreen(
    newsState: UiState<List<NewsArticle>>,
    searchQuery: String,
    selectedCategory: NewsCategory?,
    bookmarkedIds: Set<String>,
    isAdmin: Boolean,
    isExpandedScreen: Boolean,
    focusedNewsId: String?,
    onConsumeFocusedNews: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (NewsCategory?) -> Unit,
    onToggleBookmark: (NewsArticle) -> Unit,
    onEditNews: (NewsArticle) -> Unit,
    onDeleteNews: (NewsArticle) -> Unit,
    onSeedStarter: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedArticleId by remember { mutableStateOf<String?>(null) }

    val allArticles = (newsState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(focusedNewsId, allArticles) {
        if (focusedNewsId != null && allArticles.any { it.id == focusedNewsId }) {
            selectedArticleId = focusedNewsId
            onConsumeFocusedNews()
        }
    }

    val filteredArticles = remember(allArticles, searchQuery, selectedCategory) {
        allArticles.filter { article ->
            val matchesCat = selectedCategory == null || article.resolvedCategory() == selectedCategory
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                article.title.contains(q, ignoreCase = true) ||
                article.summary.contains(q, ignoreCase = true) ||
                article.content.contains(q, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val activeArticle = remember(allArticles, selectedArticleId, isExpandedScreen, filteredArticles) {
        allArticles.firstOrNull { it.id == selectedArticleId }
            ?: if (isExpandedScreen) filteredArticles.firstOrNull() else null
    }

    // BackHandler on compact/medium detail view
    if (!isExpandedScreen && selectedArticleId != null && activeArticle != null) {
        BackHandler { selectedArticleId = null }
        NewsDetailPane(
            article = activeArticle,
            isBookmarked = bookmarkedIds.contains(activeArticle.id),
            isAdmin = isAdmin,
            showBackButton = true,
            onBack = { selectedArticleId = null },
            onToggleBookmark = { onToggleBookmark(activeArticle) },
            onEdit = { onEditNews(activeArticle) },
            onDelete = {
                onDeleteNews(activeArticle)
                selectedArticleId = null
            },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (isExpandedScreen) {
        // Canonical List-Detail split pane for Tablets / Foldables
        Row(modifier = modifier.fillMaxSize()) {
            NewsListPane(
                newsState = newsState,
                filteredArticles = filteredArticles,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                selectedArticleId = activeArticle?.id,
                bookmarkedIds = bookmarkedIds,
                isAdmin = isAdmin,
                onSearchQueryChange = onSearchQueryChange,
                onSelectCategory = onSelectCategory,
                onArticleClick = { selectedArticleId = it.id },
                onToggleBookmark = onToggleBookmark,
                onSeedStarter = onSeedStarter,
                modifier = Modifier
                    .weight(0.46f)
                    .fillMaxHeight()
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .weight(0.54f)
                    .fillMaxHeight()
            ) {
                if (activeArticle != null) {
                    NewsDetailPane(
                        article = activeArticle,
                        isBookmarked = bookmarkedIds.contains(activeArticle.id),
                        isAdmin = isAdmin,
                        showBackButton = false,
                        onBack = {},
                        onToggleBookmark = { onToggleBookmark(activeArticle) },
                        onEdit = { onEditNews(activeArticle) },
                        onDelete = {
                            onDeleteNews(activeArticle)
                            selectedArticleId = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyBulletinState(
                        icon = Icons.Filled.Newspaper,
                        title = "Pilih Berita untuk Membaca",
                        description = "Ketuk salah satu warta di panel kiri untuk membaca berita lengkap SIB Toburon.",
                        isAdmin = false
                    )
                }
            }
        }
    } else {
        NewsListPane(
            newsState = newsState,
            filteredArticles = filteredArticles,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            selectedArticleId = null,
            bookmarkedIds = bookmarkedIds,
            isAdmin = isAdmin,
            onSearchQueryChange = onSearchQueryChange,
            onSelectCategory = onSelectCategory,
            onArticleClick = { selectedArticleId = it.id },
            onToggleBookmark = onToggleBookmark,
            onSeedStarter = onSeedStarter,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun NewsListPane(
    newsState: UiState<List<NewsArticle>>,
    filteredArticles: List<NewsArticle>,
    searchQuery: String,
    selectedCategory: NewsCategory?,
    selectedArticleId: String?,
    bookmarkedIds: Set<String>,
    isAdmin: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (NewsCategory?) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onToggleBookmark: (NewsArticle) -> Unit,
    onSeedStarter: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
    ) {
        item(key = "hero_banner") {
            SanctuaryHeroHeader(isAdmin = isAdmin)
        }

        item(key = "search_bar") {
            EditorialSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = stringResource(R.string.search_news_hint),
                testTagName = "news_search_input",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "category_chips") {
            CategoryFilterRow(
                items = NewsCategory.entries,
                selected = selectedCategory,
                labelProvider = { it.label },
                onSelect = onSelectCategory
            )
        }

        when (newsState) {
            is UiState.Loading -> {
                item(key = "loading_news") {
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
                item(key = "error_news") {
                    ErrorStateCard(message = newsState.message)
                }
            }
            is UiState.Success -> {
                if (filteredArticles.isEmpty()) {
                    item(key = "empty_news") {
                        EmptyBulletinState(
                            icon = Icons.Filled.Newspaper,
                            title = if (newsState.data.isEmpty()) stringResource(R.string.empty_news_title)
                            else "Berita Tidak Ditemukan",
                            description = if (newsState.data.isEmpty()) stringResource(R.string.empty_news_desc)
                            else "Coba ubah kata kunci pencarian atau pilih kategori lain.",
                            isAdmin = isAdmin && newsState.data.isEmpty(),
                            onSeedStarter = onSeedStarter
                        )
                    }
                } else {
                    items(filteredArticles, key = { it.id }) { article ->
                        NewsArticleCard(
                            article = article,
                            isSelected = article.id == selectedArticleId,
                            isBookmarked = bookmarkedIds.contains(article.id),
                            onClick = { onArticleClick(article) },
                            onToggleBookmark = { onToggleBookmark(article) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SanctuaryHeroHeader(isAdmin: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .height(210.dp)
            .clip(MaterialTheme.shapes.large)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_hero_sanctuary),
            contentDescription = stringResource(R.string.cd_sanctuary_banner),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x550E4139),
                            Color(0xDD0B2D27)
                        )
                    )
                )
        )
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiveSyncStatusPill(darkBackdrop = true)
                if (isAdmin) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = stringResource(R.string.admin_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.church_motto),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
private fun NewsArticleCard(
    article: NewsArticle,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (article.pinned) 3.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("news_card_${article.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (article.pinned) 185.dp else 152.dp)
            ) {
                BulletinCoverImage(
                    coverPreset = article.resolvedCoverPreset(),
                    imageUrl = article.imageUrl,
                    imageBase64 = article.imageBase64,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = article.resolvedCategory().label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (article.pinned) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondary
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Utama",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondary
                                )
                            }
                        }
                    }
                    if (!article.published) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.error
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Draf",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError
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
                        .padding(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(R.string.cd_bookmark_toggle),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.secondaryContainer else Color.White
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = article.authorName.ifBlank { "SIB Toburon" },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatIndonesianTimestamp(article.updatedAt ?: article.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NewsDetailPane(
    article: NewsArticle,
    isBookmarked: Boolean,
    isAdmin: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
        ) {
            BulletinCoverImage(
                coverPreset = article.resolvedCoverPreset(),
                imageUrl = article.imageUrl,
                imageBase64 = article.imageBase64,
                contentDescription = article.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.35f))
                        )
                    )
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                if (showBackButton) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_navigate_back),
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = stringResource(R.string.cd_bookmark_toggle),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.secondaryContainer else Color.White
                            )
                        }
                    }
                    if (isAdmin) {
                        Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    tint = Color.White
                                )
                            }
                        }
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = article.resolvedCategory().label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
                if (!article.published) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "Belum Dipublikasi (Draf)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Oleh ${article.authorName.ifBlank { "Sekretariat SIB Toburon" }} • ${formatIndonesianTimestamp(article.updatedAt ?: article.createdAt)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = article.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
