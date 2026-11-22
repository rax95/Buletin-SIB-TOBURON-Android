package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChurchSchedule
import com.example.data.ScheduleCategory
import com.example.data.UiState
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.EditorialSearchBar
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LiveSyncStatusPill
import com.example.ui.components.formatScheduleTimeDisplay
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.SanctuaryPine
import com.example.ui.theme.SanctuaryPineDeep

@Composable
fun SchedulesScreen(
    schedulesState: UiState<List<ChurchSchedule>>,
    searchQuery: String,
    selectedCategory: ScheduleCategory?,
    bookmarkedIds: Set<String>,
    isAdmin: Boolean,
    isBusy: Boolean,
    isExpandedScreen: Boolean,
    focusedScheduleId: String?,
    onConsumeFocusedSchedule: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ScheduleCategory?) -> Unit,
    onToggleBookmark: (ChurchSchedule) -> Unit,
    onToggleFeaturedInNews: (ChurchSchedule) -> Unit,
    onEditSchedule: (ChurchSchedule) -> Unit,
    onDeleteSchedule: (ChurchSchedule) -> Unit,
    onAddSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedScheduleId by remember { mutableStateOf<String?>(null) }

    val allSchedules = (schedulesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(focusedScheduleId, allSchedules) {
        if (focusedScheduleId != null && allSchedules.any { it.id == focusedScheduleId }) {
            selectedScheduleId = focusedScheduleId
            onConsumeFocusedSchedule()
        }
    }

    val filteredSchedules = remember(allSchedules, searchQuery, selectedCategory) {
        allSchedules.filter { item ->
            val matchesCat = selectedCategory == null || item.resolvedCategory() == selectedCategory
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                item.title.contains(q, ignoreCase = true) ||
                item.location.contains(q, ignoreCase = true) ||
                item.dateLabel.contains(q, ignoreCase = true) ||
                item.speaker.contains(q, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    val activeSchedule = remember(allSchedules, selectedScheduleId, isExpandedScreen, filteredSchedules) {
        allSchedules.firstOrNull { it.id == selectedScheduleId }
            ?: if (isExpandedScreen) filteredSchedules.firstOrNull() else null
    }

    if (!isExpandedScreen && selectedScheduleId != null && activeSchedule != null) {
        BackHandler { selectedScheduleId = null }
        ScheduleDetailPane(
            schedule = activeSchedule,
            isBookmarked = bookmarkedIds.contains(activeSchedule.id),
            isAdmin = isAdmin,
            isBusy = isBusy,
            showBackButton = true,
            onBack = { selectedScheduleId = null },
            onToggleBookmark = { onToggleBookmark(activeSchedule) },
            onToggleFeaturedInNews = { onToggleFeaturedInNews(activeSchedule) },
            onEdit = { onEditSchedule(activeSchedule) },
            onDelete = {
                onDeleteSchedule(activeSchedule)
                selectedScheduleId = null
            },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    if (isExpandedScreen) {
        Row(modifier = modifier.fillMaxSize()) {
            ScheduleListPane(
                schedulesState = schedulesState,
                filteredSchedules = filteredSchedules,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                selectedScheduleId = activeSchedule?.id,
                bookmarkedIds = bookmarkedIds,
                isAdmin = isAdmin,
                onSearchQueryChange = onSearchQueryChange,
                onSelectCategory = onSelectCategory,
                onScheduleClick = { selectedScheduleId = it.id },
                onToggleBookmark = onToggleBookmark,
                onAddSchedule = onAddSchedule,
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
            ) {
                if (activeSchedule != null) {
                    ScheduleDetailPane(
                        schedule = activeSchedule,
                        isBookmarked = bookmarkedIds.contains(activeSchedule.id),
                        isAdmin = isAdmin,
                        isBusy = isBusy,
                        showBackButton = false,
                        onBack = {},
                        onToggleBookmark = { onToggleBookmark(activeSchedule) },
                        onToggleFeaturedInNews = { onToggleFeaturedInNews(activeSchedule) },
                        onEdit = { onEditSchedule(activeSchedule) },
                        onDelete = {
                            onDeleteSchedule(activeSchedule)
                            selectedScheduleId = null
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyBulletinState(
                        icon = Icons.Filled.CalendarMonth,
                        title = tr("Pilih Jadual Kegiatan", "Select Event Schedule"),
                        description = tr(
                            "Ketik salah satu agenda gereja di sebelah kiri untuk melihat butiran masa, lokasi, dan pelayan.",
                            "Tap any church event on the left to view time, location, and speaker details."
                        ),
                        isAdmin = false
                    )
                }
            }
        }
    } else {
        ScheduleListPane(
            schedulesState = schedulesState,
            filteredSchedules = filteredSchedules,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            selectedScheduleId = null,
            bookmarkedIds = bookmarkedIds,
            isAdmin = isAdmin,
            onSearchQueryChange = onSearchQueryChange,
            onSelectCategory = onSelectCategory,
            onScheduleClick = { selectedScheduleId = it.id },
            onToggleBookmark = onToggleBookmark,
            onAddSchedule = onAddSchedule,
            modifier = modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ScheduleListPane(
    schedulesState: UiState<List<ChurchSchedule>>,
    filteredSchedules: List<ChurchSchedule>,
    searchQuery: String,
    selectedCategory: ScheduleCategory?,
    selectedScheduleId: String?,
    bookmarkedIds: Set<String>,
    isAdmin: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ScheduleCategory?) -> Unit,
    onScheduleClick: (ChurchSchedule) -> Unit,
    onToggleBookmark: (ChurchSchedule) -> Unit,
    onAddSchedule: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        item(key = "schedule_header") {
            ScheduleSpotlightBanner(
                totalCount = (schedulesState as? UiState.Success)?.data?.size ?: 0,
                highlightSchedule = filteredSchedules.firstOrNull()
            )
        }

        item(key = "schedule_search") {
            EditorialSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                placeholder = tr("Cari ibadah, lokasi, atau pelayan...", "Search service, location, or speaker..."),
                testTagName = "schedule_search_input",
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        item(key = "schedule_chips") {
            val lang = LocalAppLanguage.current
            CategoryFilterRow(
                items = ScheduleCategory.entries,
                selected = selectedCategory,
                labelProvider = { it.localizedLabel(lang) },
                onSelect = onSelectCategory
            )
        }

        when (schedulesState) {
            is UiState.Loading -> {
                item(key = "loading_schedules") {
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
                item(key = "error_schedules") {
                    ErrorStateCard(message = schedulesState.message)
                }
            }
            is UiState.Success -> {
                if (filteredSchedules.isEmpty()) {
                    item(key = "empty_schedules") {
                        EmptyBulletinState(
                            icon = Icons.Filled.CalendarMonth,
                            title = if (schedulesState.data.isEmpty()) {
                                tr("Belum Ada Jadual Kegiatan", "No Event Schedules Yet")
                            } else {
                                tr("Jadual Tidak Dijumpai", "No Schedules Found")
                            },
                            description = if (schedulesState.data.isEmpty()) {
                                tr(
                                    "Jadual ibadah dan kegiatan pelayanan gereja akan disegerakkan di sini.",
                                    "Church worship services and ministry schedules will be synced here."
                                )
                            } else {
                                tr(
                                    "Cuba ubah penapis kategori atau kata carian.",
                                    "Try changing the category filter or search keywords."
                                )
                            },
                            isAdmin = isAdmin && schedulesState.data.isEmpty(),
                            primaryActionLabel = tr("Tambah Jadual", "Add Schedule"),
                            onPrimaryAction = onAddSchedule
                        )
                    }
                } else {
                    items(filteredSchedules, key = { it.id }) { schedule ->
                        ScheduleTimelineCard(
                            schedule = schedule,
                            isSelected = schedule.id == selectedScheduleId,
                            isBookmarked = bookmarkedIds.contains(schedule.id),
                            onClick = { onScheduleClick(schedule) },
                            onToggleBookmark = { onToggleBookmark(schedule) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleSpotlightBanner(
    totalCount: Int,
    highlightSchedule: ChurchSchedule?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(
                Brush.linearGradient(
                    colors = listOf(SanctuaryPineDeep, SanctuaryPine)
                )
            )
            .padding(20.dp)
    ) {
        Column {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LiveSyncStatusPill(darkBackdrop = true)
                Text(
                    text = tr("$totalCount Agenda Aktif", "$totalCount Active Events"),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = tr("Jadual Kegiatan Gereja", "Church Activity Schedule"),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (highlightSchedule != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${tr("AGENDA UTAMA", "FEATURED EVENT")} • ${highlightSchedule.dateLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFD8A8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = highlightSchedule.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${formatScheduleTimeDisplay(highlightSchedule.timeLabel)} • ${highlightSchedule.location}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            } else {
                Text(
                    text = tr(
                        "Ibadah Raya, Persekutuan Doa, Pemuda, dan Pelayanan SIB Toburon disegerakkan secara automatik.",
                        "Sunday Worship, Prayer Fellowship, Youth, and SIB Toburon Ministries synced automatically."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.86f)
                )
            }
        }
    }
}

@Composable
private fun ScheduleTimelineCard(
    schedule: ChurchSchedule,
    isSelected: Boolean,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = when (schedule.resolvedCategory()) {
        ScheduleCategory.IBADAH -> SanctuaryPine
        ScheduleCategory.DOA -> HarvestGold
        ScheduleCategory.PEMUDA -> MaterialTheme.colorScheme.tertiary
        ScheduleCategory.SEKOLAH_MINGGU -> Color(0xFF2B7A9E)
        ScheduleCategory.PERSEKUTUAN -> Color(0xFF5B7A3D)
        ScheduleCategory.RAPAT -> MaterialTheme.colorScheme.secondary
    }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLowest
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("schedule_card_${schedule.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Editorial vertical category ribbon
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = accentColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = schedule.resolvedCategory().localizedLabel(LocalAppLanguage.current),
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                        if (schedule.recurring) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = tr("Berkala", "Recurring"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (schedule.featuredInNews) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(11.dp),
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = tr("Di Berita", "In News"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        if (!schedule.published) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = tr("Draf", "Draft"),
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = tr("Simpan penanda buku", "Toggle bookmark"),
                            tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))

                ScheduleMetaLine(
                    icon = Icons.Filled.CalendarMonth,
                    text = "${schedule.dateLabel} • ${formatScheduleTimeDisplay(schedule.timeLabel)}"
                )
                Spacer(modifier = Modifier.height(4.dp))
                ScheduleMetaLine(icon = Icons.Filled.LocationOn, text = schedule.location)
                if (schedule.speaker.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    ScheduleMetaLine(icon = Icons.Filled.Person, text = schedule.speaker)
                }
            }
        }
    }
}

@Composable
private fun ScheduleMetaLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ScheduleDetailPane(
    schedule: ChurchSchedule,
    isBookmarked: Boolean,
    isAdmin: Boolean,
    isBusy: Boolean,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleFeaturedInNews: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showBackButton) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("Kembali", "Back")
                        )
                    }
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = tr("Simpan penanda buku", "Toggle bookmark"),
                        tint = if (isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isAdmin) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = tr("Edit", "Edit"))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = tr("Padam", "Delete"),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = schedule.resolvedCategory().localizedLabel(LocalAppLanguage.current),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = schedule.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (isAdmin) {
            Spacer(modifier = Modifier.height(14.dp))
            if (schedule.featuredInNews) {
                OutlinedButton(
                    onClick = onToggleFeaturedInNews,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("toggle_featured_in_news_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tr(
                            "Dipaparkan di Menu Utama Berita (Keluarkan)",
                            "Featured on News Main Menu (Remove)"
                        )
                    )
                }
            } else {
                Button(
                    onClick = onToggleFeaturedInNews,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("toggle_featured_in_news_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Tambahkan ke Menu Utama Berita", "Add to News Main Menu"))
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                DetailInfoRow(Icons.Filled.CalendarMonth, tr("Hari / Tarikh", "Day / Date"), schedule.dateLabel)
                DetailInfoRow(
                    Icons.Filled.AccessTime,
                    tr("Waktu Pelaksanaan", "Time"),
                    formatScheduleTimeDisplay(schedule.timeLabel)
                )
                DetailInfoRow(Icons.Filled.LocationOn, tr("Lokasi", "Location"), schedule.location)
                if (schedule.speaker.isNotBlank()) {
                    DetailInfoRow(
                        Icons.Filled.Person,
                        tr("Pelayan Firman / Penyelaras", "Speaker / Coordinator"),
                        schedule.speaker
                    )
                }
                if (schedule.recurring) {
                    DetailInfoRow(
                        Icons.Filled.Repeat,
                        tr("Kekerapan", "Frequency"),
                        tr("Kegiatan Berkala Mingguan", "Weekly Recurring Activity")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = tr("Keterangan & Maklumat Jemaat", "Details & Congregation Info"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = schedule.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(64.dp))
    }
}

@Composable
private fun DetailInfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
