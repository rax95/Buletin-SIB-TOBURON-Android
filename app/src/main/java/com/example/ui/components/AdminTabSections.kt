package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.data.AdminMember
import com.example.data.AppVersionConfig
import com.example.data.DynamicCategoryItem
import com.example.data.ImageUtils
import com.example.data.UserProfile
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr

@Composable
fun AdminNavigationColumnCard(
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    cardTestTag: String,
    badgeTestTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(cardTestTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconBgColor,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTintColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag(badgeTestTag)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = tr("Buka $title", "Open $title"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun AdminSubPageHeader(
    title: String,
    subtitle: String,
    badgeText: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("admin_subpage_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("Kembali ke Panel Admin", "Back to Admin Panel"),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!badgeText.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

private enum class ManageEventsSubPage {
    MENU,
    NEWS_CATEGORIES,
    SCHEDULE_CATEGORIES
}

@Composable
fun AdminManageEventsView(
    newsCategories: List<DynamicCategoryItem>,
    scheduleCategories: List<DynamicCategoryItem>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onAddNewsClick: () -> Unit,
    onAddScheduleClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onSaveNewsCategory: (existingCode: String?, labelMs: String, labelEn: String, onDone: () -> Unit) -> Unit,
    onDeleteNewsCategory: (code: String, onDone: () -> Unit) -> Unit,
    onSaveScheduleCategory: (existingCode: String?, labelMs: String, labelEn: String, onDone: () -> Unit) -> Unit,
    onDeleteScheduleCategory: (code: String, onDone: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var subPage by rememberSaveable { mutableStateOf(ManageEventsSubPage.MENU) }

    BackHandler(enabled = subPage != ManageEventsSubPage.MENU) {
        subPage = ManageEventsSubPage.MENU
    }

    AnimatedContent(
        targetState = subPage,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "manage_events_subpage_nav",
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            ManageEventsSubPage.MENU -> {
                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("admin_manage_events_page")
                    ) {
                        AdminSubPageHeader(
                            title = tr("Urus Acara", "Manage Events"),
                            subtitle = tr(
                                "Cipta berita, jadual, foto, ahli & urus kategori",
                                "Create news, schedule, gallery, member & manage categories"
                            ),
                            badgeText = tr("6 Alat", "6 Tools"),
                            onBack = onBack
                        )

                        AdminActionTileCard(
                            icon = Icons.Filled.Newspaper,
                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = tr("Tulis Berita", "Write News"),
                            description = tr(
                                "Terbitkan warta, pengumuman atau berita gereja kepada semua pengguna secara masa nyata.",
                                "Publish church news or announcements to all users in real time."
                            ),
                            onClick = onAddNewsClick,
                            testTagName = "admin_add_news_button"
                        )

                        AdminActionTileCard(
                            icon = Icons.Filled.CalendarMonth,
                            iconBg = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = tr("Tambah Jadual", "Add Schedule"),
                            description = tr(
                                "Tambah jadual ibadah, pelayanan, doa atau persekutuan baharu.",
                                "Add a new worship service, ministry, prayer, or fellowship schedule."
                            ),
                            onClick = onAddScheduleClick,
                            testTagName = "admin_add_schedule_button"
                        )

                        AdminActionTileCard(
                            icon = Icons.Filled.PhotoLibrary,
                            iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = tr("Muat Naik Foto", "Upload Photo"),
                            description = tr(
                                "Muat naik dokumentasi foto aktiviti dan kebersamaan jemaat ke Galeri.",
                                "Upload church activity and congregation photos to the Gallery."
                            ),
                            onClick = onAddPhotoClick,
                            testTagName = "admin_add_photo_button"
                        )

                        AdminActionTileCard(
                            icon = Icons.Filled.Groups,
                            iconBg = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = tr("Tambah Ahli", "Add Member"),
                            description = tr(
                                "Daftar ahli jemaat baharu terus ke dalam direktori gereja SIB Toburon.",
                                "Register a new church member directly into the SIB Toburon directory."
                            ),
                            onClick = onAddMemberClick,
                            testTagName = "admin_add_member_button"
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = tr("Urus Kategori", "Manage Categories"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        AdminActionTileCard(
                            icon = Icons.Filled.Category,
                            iconBg = MaterialTheme.colorScheme.secondaryContainer,
                            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = tr("Kategori Berita", "News Categories"),
                            description = tr(
                                "${newsCategories.size} kategori aktif • Tambah, edit & padam kategori Berita.",
                                "${newsCategories.size} active categories • Add, edit & delete News categories."
                            ),
                            onClick = { subPage = ManageEventsSubPage.NEWS_CATEGORIES },
                            testTagName = "admin_manage_news_categories_button"
                        )

                        AdminActionTileCard(
                            icon = Icons.Filled.EditCalendar,
                            iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = tr("Kategori Jadual", "Schedule Categories"),
                            description = tr(
                                "${scheduleCategories.size} kategori aktif • Tambah, edit & padam kategori Jadual.",
                                "${scheduleCategories.size} active categories • Add, edit & delete Schedule categories."
                            ),
                            onClick = { subPage = ManageEventsSubPage.SCHEDULE_CATEGORIES },
                            testTagName = "admin_manage_schedule_categories_button"
                        )
                    }
                }
            }

            ManageEventsSubPage.NEWS_CATEGORIES -> {
                AdminCategoryManagerView(
                    title = tr("Kategori Berita", "News Categories"),
                    subtitle = tr("Tambah, edit atau padam kategori untuk tab Berita", "Add, edit, or delete categories for News"),
                    categories = newsCategories,
                    isBusy = isBusy,
                    pageTestTag = "admin_news_categories_page",
                    onBack = { subPage = ManageEventsSubPage.MENU },
                    onSaveCategory = onSaveNewsCategory,
                    onDeleteCategory = onDeleteNewsCategory
                )
            }

            ManageEventsSubPage.SCHEDULE_CATEGORIES -> {
                AdminCategoryManagerView(
                    title = tr("Kategori Jadual", "Schedule Categories"),
                    subtitle = tr("Tambah, edit atau padam kategori untuk tab Jadual", "Add, edit, or delete categories for Schedules"),
                    categories = scheduleCategories,
                    isBusy = isBusy,
                    pageTestTag = "admin_schedule_categories_page",
                    onBack = { subPage = ManageEventsSubPage.MENU },
                    onSaveCategory = onSaveScheduleCategory,
                    onDeleteCategory = onDeleteScheduleCategory
                )
            }
        }
    }
}

@Composable
private fun AdminActionTileCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
    testTagName: String
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTagName)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClick = onClick)
                .padding(18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AdminManageAdministratorView(
    adminMembers: List<AdminMember>,
    onBack: () -> Unit,
    onOpenAddAdminDialog: () -> Unit,
    onRemoveAdminMember: (String) -> Unit,
    onCopyUid: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("admin_manage_administrator_page")
        ) {
            item(key = "admin_mgr_header") {
                AdminSubPageHeader(
                    title = tr("Urus Pentadbir", "Manage Administrator"),
                    subtitle = tr(
                        "Tambah atau batalkan akses pentadbir tambahan menggunakan UID",
                        "Add or revoke additional administrator access by UID"
                    ),
                    badgeText = tr("${adminMembers.size} Pentadbir", "${adminMembers.size} Admins"),
                    onBack = onBack
                )
            }

            item(key = "admin_mgr_add_action") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr(
                                        "Pentadbir Tambahan (${adminMembers.size})",
                                        "Additional Admins (${adminMembers.size})"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = tr(
                                        "Salin UID daripada senarai Pengguna Log Masuk Google untuk memberi akses admin kepada setiausaha atau pasukan multimedia.",
                                        "Copy a user's UID from Google Signed-in Users to grant admin access to secretaries or media team members."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = onOpenAddAdminDialog,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_add_administrator_button")
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Tambah Pentadbir Baharu", "Add New Administrator"))
                        }
                    }
                }
            }

            if (adminMembers.isEmpty()) {
                item(key = "admin_mgr_empty") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "Belum ada pentadbir tambahan. Ketik butang di atas untuk menambah setiausaha atau pasukan multimedia gereja menggunakan UID mereka.",
                                "No additional admins yet. Tap the button above to add church secretaries or media team members using their UID."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = adminMembers,
                    key = { _, admin -> admin.id }
                ) { index, member ->
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_additional_item_$index")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${member.roleTitle} • ${member.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onCopyUid(member.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = tr("Salin UID ${member.displayName}", "Copy UID for ${member.displayName}"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onRemoveAdminMember(member.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = tr("Batalkan akses admin", "Revoke admin access"),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminGoogleUsersView(
    registeredUsers: List<UserProfile>,
    currentAdminUid: String,
    onBack: () -> Unit,
    onCopyUid: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var searchQuery by remember { mutableStateOf("") }

    val filteredUsers = remember(registeredUsers, searchQuery, lang) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            registeredUsers
        } else {
            registeredUsers.filter { user ->
                user.displayName.contains(q, ignoreCase = true) ||
                    user.email.contains(q, ignoreCase = true) ||
                    user.resolvedGroup().localizedLabel(lang).contains(q, ignoreCase = true)
            }
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("admin_google_users_page")
        ) {
            item(key = "google_users_header") {
                AdminSubPageHeader(
                    title = tr("Pengguna Log Masuk Google", "Google Signed-In Users"),
                    subtitle = tr("Semua akaun Google yang pernah log masuk", "All Google accounts signed in to the app"),
                    badgeText = tr("${registeredUsers.size} Pengguna", "${registeredUsers.size} Users"),
                    onBack = onBack
                )
            }

            if (registeredUsers.isNotEmpty()) {
                item(key = "google_users_search") {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(tr("Cari nama atau emel pengguna...", "Search user name or email...")) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = tr("Padam carian", "Clear search")
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_google_users_search")
                    )
                }
            }

            if (registeredUsers.isEmpty()) {
                item(key = "google_users_empty") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "Belum ada pengguna akaun Google direkodkan. Setiap jemaat yang log masuk menggunakan Google akan dipaparkan secara automatik di sini.",
                                "No Google account users recorded yet. Every member who signs in with Google will appear here automatically."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else if (filteredUsers.isEmpty()) {
                item(key = "google_users_no_match") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tr(
                                "Tiada pengguna sepadan dengan carian \"$searchQuery\".",
                                "No users match \"$searchQuery\"."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(18.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredUsers,
                    key = { _, user -> user.id }
                ) { index, user ->
                    val resolvedName = user.displayName.trim().ifBlank { "Jemaat SIB Toburon" }
                    val initials = resolvedName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifBlank { "JG" }
                    val isSelf = user.id == currentAdminUid
                    val lastSeen = user.lastLoginAt ?: user.updatedAt ?: user.createdAt

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_google_user_item_$index")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
                        ) {
                            val userAvatarBitmap = remember(user.photoBase64) {
                                ImageUtils.decodeBase64ToImageBitmap(user.photoBase64)
                            }
                            Surface(
                                shape = CircleShape,
                                color = if (isSelf) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (userAvatarBitmap != null) {
                                        Image(
                                            bitmap = userAvatarBitmap,
                                            contentDescription = resolvedName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Text(
                                            text = initials,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelf) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = resolvedName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isSelf) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.secondaryContainer
                                        ) {
                                            Text(
                                                text = tr("Anda", "You"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                if (user.email.isNotBlank()) {
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val metaLine = buildString {
                                    append(user.resolvedGroup().localizedLabel(lang))
                                    if (lastSeen != null) {
                                        append(" • ")
                                        append(formatIndonesianTimestamp(lastSeen, lang))
                                    }
                                }
                                Text(
                                    text = metaLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onCopyUid(user.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = tr("Salin UID $resolvedName", "Copy UID for $resolvedName"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVersionManagerView(
    appVersionConfig: AppVersionConfig?,
    onBack: () -> Unit,
    onManageAppVersionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val publishedVersionName = appVersionConfig?.latestVersionName?.ifBlank { BuildConfig.VERSION_NAME }
        ?: BuildConfig.VERSION_NAME
    val publishedVersionCode = appVersionConfig?.latestVersionCode ?: BuildConfig.VERSION_CODE
    val configuredUrl = appVersionConfig?.updateUrl.orEmpty()
    val isForceUpdate = appVersionConfig?.forceUpdate == true

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("admin_version_manager_page")
        ) {
            AdminSubPageHeader(
                title = tr("Pengurus Versi", "Version Manager"),
                subtitle = tr("Versi terbitan & pautan kemas kini aplikasi", "Published version & direct app update link"),
                badgeText = "v$publishedVersionName",
                onBack = onBack
            )

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.SystemUpdate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    "Versi Terbitan: v$publishedVersionName (Kod $publishedVersionCode)",
                                    "Published Version: v$publishedVersionName (Build $publishedVersionCode)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = tr(
                                    "Binaan semasa peranti: v${BuildConfig.VERSION_NAME} (Kod ${BuildConfig.VERSION_CODE})",
                                    "Current installed build: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isForceUpdate) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = tr("Wajib", "Required"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Text(
                                text = tr("Pautan Muat Turun Terus", "Direct Update Download Link"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = if (configuredUrl.startsWith("https://")) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (configuredUrl.startsWith("https://")) {
                                        configuredUrl
                                    } else {
                                        tr(
                                            "Belum ada pautan muat turun terus ditetapkan",
                                            "No direct update download link set yet"
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (configuredUrl.startsWith("https://")) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (!appVersionConfig?.releaseNotes.isNullOrBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text(
                                    text = tr("Nota Keluaran", "Release Notes"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = appVersionConfig?.releaseNotes.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    FilledTonalButton(
                        onClick = onManageAppVersionClick,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("admin_manage_version_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tr(
                                "Urus Versi & Pautan Kemas Kini",
                                "Manage Version & Direct Update Link"
                            )
                        )
                    }
                }
            }
        }
    }
}
