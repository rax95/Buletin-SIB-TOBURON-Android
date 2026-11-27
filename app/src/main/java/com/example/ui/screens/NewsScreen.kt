package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ChurchSchedule
import com.example.data.DynamicCategoryItem
import com.example.data.NewsArticle
import com.example.data.NewsComment
import com.example.data.UiState
import com.example.data.resolveNewsCategoryLabel
import com.example.data.resolveScheduleCategoryLabel
import com.example.ui.components.BulletinCoverImage
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.CommentReplyTarget
import com.example.ui.components.EditorialSearchBar
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.NewsCommentComposerBar
import com.example.ui.components.NewsCommentsSection
import com.example.ui.components.formatIndonesianTimestamp
import com.example.ui.components.formatScheduleTimeDisplay
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch

@Composable
fun NewsScreen(
    newsState: UiState<List<NewsArticle>>,
    featuredSchedules: List<ChurchSchedule>,
    newsCategories: List<DynamicCategoryItem>,
    scheduleCategories: List<DynamicCategoryItem>,
    searchQuery: String,
    selectedCategory: String?,
    bookmarkedIds: Set<String>,
    commentsState: UiState<List<NewsComment>>,
    isPostingComment: Boolean,
    currentUserId: String?,
    isGuest: Boolean,
    isAdmin: Boolean,
    isExpandedScreen: Boolean,
    focusedNewsId: String?,
    onConsumeFocusedNews: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onToggleBookmark: (NewsArticle) -> Unit,
    onObserveComments: (String?) -> Unit,
    onPostComment: (newsId: String, text: String, parentCommentId: String?, replyToAuthorName: String?, onComplete: () -> Unit) -> Unit,
    onDeleteComment: (newsId: String, commentId: String) -> Unit,
    onDetailVisibilityChange: (Boolean) -> Unit,
    onOpenFeaturedSchedule: (ChurchSchedule) -> Unit,
    onRemoveFeaturedSchedule: (ChurchSchedule) -> Unit,
    onEditNews: (NewsArticle) -> Unit,
    onDeleteNews: (NewsArticle) -> Unit,
    onAddNews: () -> Unit,
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
            val matchesCat = selectedCategory == null ||
                article.category.equals(selectedCategory, ignoreCase = true)
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

    val compactDetailArticle = if (!isExpandedScreen && selectedArticleId != null) activeArticle else null
    val isCompactDetailVisible = compactDetailArticle != null

    LaunchedEffect(activeArticle?.id) {
        onObserveComments(activeArticle?.id)
    }
    LaunchedEffect(isCompactDetailVisible) {
        onDetailVisibilityChange(isCompactDetailVisible)
    }
    DisposableEffect(Unit) {
        onDispose {
            onObserveComments(null)
            onDetailVisibilityChange(false)
        }
    }

    // BackHandler on compact/medium detail view
    if (compactDetailArticle != null) {
        BackHandler { selectedArticleId = null }
        NewsDetailPane(
            article = compactDetailArticle,
            newsCategories = newsCategories,
            isBookmarked = bookmarkedIds.contains(compactDetailArticle.id),
            commentsState = commentsState,
            isPostingComment = isPostingComment,
            currentUserId = currentUserId,
            isGuest = isGuest,
            isAdmin = isAdmin,
            showBackButton = true,
            onBack = { selectedArticleId = null },
            onToggleBookmark = { onToggleBookmark(compactDetailArticle) },
            onPostComment = { text, parentId, replyToName, onDone ->
                onPostComment(compactDetailArticle.id, text, parentId, replyToName, onDone)
            },
            onDeleteComment = { commentId ->
                onDeleteComment(compactDetailArticle.id, commentId)
            },
            onEdit = { onEditNews(compactDetailArticle) },
            onDelete = {
                onDeleteNews(compactDetailArticle)
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
                featuredSchedules = featuredSchedules,
                newsCategories = newsCategories,
                scheduleCategories = scheduleCategories,
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
                onOpenFeaturedSchedule = onOpenFeaturedSchedule,
                onRemoveFeaturedSchedule = onRemoveFeaturedSchedule,
                onAddNews = onAddNews,
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
                        newsCategories = newsCategories,
                        isBookmarked = bookmarkedIds.contains(activeArticle.id),
                        commentsState = commentsState,
                        isPostingComment = isPostingComment,
                        currentUserId = currentUserId,
                        isGuest = isGuest,
                        isAdmin = isAdmin,
                        showBackButton = false,
                        onBack = {},
                        onToggleBookmark = { onToggleBookmark(activeArticle) },
                        onPostComment = { text, parentId, replyToName, onDone ->
                            onPostComment(activeArticle.id, text, parentId, replyToName, onDone)
                        },
                        onDeleteComment = { commentId ->
                            onDeleteComment(activeArticle.id, commentId)
                        },
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
                        title = tr("Pilih Berita untuk Membaca", "Select News to Read"),
                        description = tr(
                            "Ketik salah satu warta di panel kiri untuk membaca berita lengkap SIB Toburon.",
                            "Tap any bulletin article on the left panel to read the full SIB Toburon news."
                        ),
                        isAdmin = false
                    )
                }
            }
        }
    } else {
        NewsListPane(
            newsState = newsState,
            featuredSchedules = featuredSchedules,
            newsCategories = newsCategories,
            scheduleCategories = scheduleCategories,
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
            onOpenFeaturedSchedule = onOpenFeaturedSchedule,
            onRemoveFeaturedSchedule = onRemoveFeaturedSchedule,
            onAddNews = onAddNews,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun NewsListPane(
    newsState: UiState<List<NewsArticle>>,
    featuredSchedules: List<ChurchSchedule>,
    newsCategories: List<DynamicCategoryItem>,
    scheduleCategories: List<DynamicCategoryItem>,
    filteredArticles: List<NewsArticle>,
    searchQuery: String,
    selectedCategory: String?,
    selectedArticleId: String?,
    bookmarkedIds: Set<String>,
    isAdmin: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (String?) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onToggleBookmark: (NewsArticle) -> Unit,
    onOpenFeaturedSchedule: (ChurchSchedule) -> Unit,
    onRemoveFeaturedSchedule: (ChurchSchedule) -> Unit,
    onAddNews: () -> Unit,
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

        // Featured Important Church Schedules displayed RIGHT ABOVE the Search Bar
        if (featuredSchedules.isNotEmpty()) {
            items(featuredSchedules, key = { "featured_schedule_${it.id}" }) { schedule ->
                FeaturedImportantScheduleCard(
                    schedule = schedule,
                    scheduleCategories = scheduleCategories,
                    isAdmin = isAdmin,
                    onClick = { onOpenFeaturedSchedule(schedule) },
                    onRemove = { onRemoveFeaturedSchedule(schedule) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        item(key = "search_bar") {
            EditorialSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = tr("Cari warta atau pengumuman...", "Search news or announcements..."),
                testTagName = "news_search_input",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "category_chips") {
            val lang = LocalAppLanguage.current
            val selectedItem = remember(newsCategories, selectedCategory) {
                selectedCategory?.let { code ->
                    newsCategories.firstOrNull { it.resolvedCode().equals(code, ignoreCase = true) }
                }
            }
            CategoryFilterRow(
                items = newsCategories,
                selected = selectedItem,
                labelProvider = { it.localizedLabel(lang) },
                keyProvider = { it.resolvedCode() },
                onSelect = { item -> onSelectCategory(item?.resolvedCode()) }
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
                            title = if (newsState.data.isEmpty()) {
                                tr("Belum Ada Berita Gereja", "No Church News Yet")
                            } else {
                                tr("Berita Tidak Dijumpai", "No News Found")
                            },
                            description = if (newsState.data.isEmpty()) {
                                tr(
                                    "Warta jemaat SIB Toburon akan dipaparkan secara automatik di sini sebaik sahaja diterbitkan oleh Admin.",
                                    "SIB Toburon church news will appear here automatically once published by an Admin."
                                )
                            } else {
                                tr(
                                    "Cuba ubah kata kunci carian atau pilih kategori lain.",
                                    "Try changing your search keywords or selecting another category."
                                )
                            },
                            isAdmin = isAdmin && newsState.data.isEmpty(),
                            primaryActionLabel = tr("Tulis Berita", "Write News"),
                            onPrimaryAction = onAddNews
                        )
                    }
                } else {
                    items(filteredArticles, key = { it.id }) { article ->
                        NewsArticleCard(
                            article = article,
                            newsCategories = newsCategories,
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
private fun FeaturedImportantScheduleCard(
    schedule: ChurchSchedule,
    scheduleCategories: List<DynamicCategoryItem>,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ribbonColor = MaterialTheme.colorScheme.error
    val onRibbonColor = MaterialTheme.colorScheme.onError

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(1.5.dp, ribbonColor.copy(alpha = 0.38f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("featured_schedule_card_${schedule.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
        ) {
            // Card Content with generous right padding on top so text never collides with corner ribbon
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 68.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = resolveScheduleCategoryLabel(schedule.category, scheduleCategories, LocalAppLanguage.current),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (!schedule.published) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = tr("Draf", "Draft"),
                            tint = ribbonColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 56.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${schedule.dateLabel} • ${formatScheduleTimeDisplay(schedule.timeLabel)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = schedule.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = tr("Lihat butiran kegiatan", "View event details"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isAdmin) {
                        TextButton(
                            onClick = onRemove,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("remove_featured_schedule_${schedule.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = tr("Keluarkan dari Berita", "Remove from News"),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = tr("Keluarkan", "Remove"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Diagonal Top-Right Corner Ribbon ("PENTING" / "IMPORTANT")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 34.dp, y = 16.dp)
                    .rotate(45f)
                    .width(124.dp)
                    .background(ribbonColor)
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = tr("PENTING", "IMPORTANT"),
                    color = onRibbonColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    maxLines = 1
                )
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
        AsyncImage(
            model = R.drawable.img_hero_sanctuary,
            contentDescription = tr("Ilustrasi Gereja SIB Toburon", "SIB Toburon Church Illustration"),
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
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isAdmin) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = tr("Admin Gereja", "Church Admin"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = tr("SIB Toburon", "SIB Toburon"),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tr(
                        "Bersatu dalam Kasih, Bertumbuh dalam Iman",
                        "United in Love, Growing in Faith"
                    ),
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
    newsCategories: List<DynamicCategoryItem>,
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
                            text = resolveNewsCategoryLabel(article.category, newsCategories, LocalAppLanguage.current),
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
                                    text = tr("Utama", "Featured"),
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
                                    text = tr("Draf", "Draft"),
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
                            contentDescription = tr("Simpan penanda buku", "Toggle bookmark"),
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
                        text = formatIndonesianTimestamp(article.updatedAt ?: article.createdAt, LocalAppLanguage.current),
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
    newsCategories: List<DynamicCategoryItem>,
    isBookmarked: Boolean,
    commentsState: UiState<List<NewsComment>>,
    isPostingComment: Boolean,
    currentUserId: String?,
    isGuest: Boolean,
    isAdmin: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onPostComment: (text: String, parentCommentId: String?, replyToAuthorName: String?, onComplete: () -> Unit) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val commentFocusRequester = remember { FocusRequester() }
    var commentDraft by remember(article.id) { mutableStateOf("") }
    var replyTarget by remember(article.id) { mutableStateOf<CommentReplyTarget?>(null) }

    val commentCount = (commentsState as? UiState.Success)?.data?.size ?: 0
    val usePinnedComposer = showBackButton

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(bottom = if (usePinnedComposer) 20.dp else 96.dp)
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
                                    contentDescription = tr("Kembali", "Back"),
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
                                    contentDescription = tr("Simpan penanda buku", "Toggle bookmark"),
                                    tint = if (isBookmarked) MaterialTheme.colorScheme.secondaryContainer else Color.White
                                )
                            }
                        }
                        if (isAdmin) {
                            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
                                IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = tr("Edit", "Edit"),
                                        tint = Color.White
                                    )
                                }
                            }
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f)) {
                                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = tr("Padam", "Delete"),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                val lang = LocalAppLanguage.current
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = resolveNewsCategoryLabel(article.category, newsCategories, lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable {
                                scope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            }
                            .testTag("news_comment_count_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ChatBubbleOutline,
                                contentDescription = tr("Pergi ke komen", "Jump to comments"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tr("$commentCount Komen", "$commentCount Comments"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    if (!article.published) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = tr("Belum Diterbitkan (Draf)", "Unpublished (Draft)"),
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
                val defaultAuthor = tr("Setiausaha SIB Toburon", "SIB Toburon Secretariat")
                val byPrefix = tr("Oleh", "By")
                Text(
                    text = "$byPrefix ${article.authorName.ifBlank { defaultAuthor }} • ${formatIndonesianTimestamp(article.updatedAt ?: article.createdAt, lang)}",
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

                Spacer(modifier = Modifier.height(24.dp))

                // On expanded split-pane, place inline composer right above comments so it never collides with the Admin FAB
                if (!usePinnedComposer) {
                    NewsCommentComposerBar(
                        commentText = commentDraft,
                        onCommentTextChange = { commentDraft = it },
                        replyTarget = replyTarget,
                        onClearReplyTarget = { replyTarget = null },
                        isGuest = isGuest,
                        isPosting = isPostingComment,
                        onSendClick = {
                            onPostComment(
                                commentDraft,
                                replyTarget?.rootParentCommentId,
                                replyTarget?.replyToAuthorName
                            ) {
                                commentDraft = ""
                                replyTarget = null
                            }
                        },
                        inputFocusRequester = commentFocusRequester,
                        isInline = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                NewsCommentsSection(
                    commentsState = commentsState,
                    currentUserId = currentUserId,
                    isGuest = isGuest,
                    isAdmin = isAdmin,
                    onReplyClick = { target ->
                        replyTarget = target
                        runCatching { commentFocusRequester.requestFocus() }
                    },
                    onDeleteComment = { comment ->
                        onDeleteComment(comment.id)
                    }
                )
            }
        }

        // On compact phones, pin the comment composer cleanly at the bottom (with keyboard inset padding)
        if (usePinnedComposer) {
            NewsCommentComposerBar(
                commentText = commentDraft,
                onCommentTextChange = { commentDraft = it },
                replyTarget = replyTarget,
                onClearReplyTarget = { replyTarget = null },
                isGuest = isGuest,
                isPosting = isPostingComment,
                onSendClick = {
                    onPostComment(
                        commentDraft,
                        replyTarget?.rootParentCommentId,
                        replyTarget?.replyToAuthorName
                    ) {
                        commentDraft = ""
                        replyTarget = null
                        scope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    }
                },
                inputFocusRequester = commentFocusRequester,
                isInline = false
            )
        }
    }
}
