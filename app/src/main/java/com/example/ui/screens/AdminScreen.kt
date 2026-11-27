package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.BuildConfig
import com.example.data.AdminMember
import com.example.data.AppVersionConfig
import com.example.data.ChurchMemberDetail
import com.example.data.ChurchMemberPublic
import com.example.data.DynamicCategoryItem
import com.example.data.MemberRegistrationConfig
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.components.AdminGoogleUsersView
import com.example.ui.components.AdminManageAdministratorView
import com.example.ui.components.AdminManageEventsView
import com.example.ui.components.AdminMemberIdCardDialog
import com.example.ui.components.AdminMemberRegistrationQrView
import com.example.ui.components.AdminNavigationColumnCard
import com.example.ui.components.AdminQuickRegistrationQrTile
import com.example.ui.components.AdminStatPill
import com.example.ui.components.AdminSubPageHeader
import com.example.ui.components.AdminVersionManagerView
import com.example.ui.components.EditorialSearchBar
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val NEW_MEMBER_BADGE_WINDOW_MS = 48L * 60L * 60L * 1000L

private enum class MemberSortOrder {
    NEWEST,
    ALPHABETICAL
}

private enum class AdminSection {
    MANAGE_EVENTS,
    CHURCH_MEMBERS,
    GOOGLE_USERS,
    MANAGE_ADMINISTRATOR,
    VERSION_MANAGER,
    MEMBER_REGISTRATION_QR
}

/**
 * Admin-only Control & Management Tab:
 * - Main Dashboard:
 *   - Top summary counters (News, Schedule, Gallery, Members).
 *   - Six dedicated click-to-open columns:
 *     1. Manage Events
 *     2. Church Members
 *     3. Google Signed-in Users
 *     4. Manage Administrator
 *     5. Version Manager
 *     6. Member Registration QR & Link
 */
@Composable
fun AdminScreen(
    newsCount: Int,
    schedulesCount: Int,
    galleryCount: Int,
    membersCount: Int,
    membersState: UiState<List<ChurchMemberPublic>>,
    searchQuery: String,
    isBusy: Boolean,
    selectedMemberDetail: ChurchMemberDetail?,
    isLoadingDetail: Boolean,
    adminMembers: List<AdminMember>,
    registeredUsers: List<UserProfile>,
    currentAdminUid: String,
    appVersionConfig: AppVersionConfig?,
    memberRegistrationConfig: MemberRegistrationConfig?,
    newsCategories: List<DynamicCategoryItem>,
    scheduleCategories: List<DynamicCategoryItem>,
    onSearchQueryChange: (String) -> Unit,
    onMemberClick: (String) -> Unit,
    onDismissMemberDetail: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (ChurchMemberDetail) -> Unit,
    onDeleteMember: (String) -> Unit,
    onExportMemberCvPdf: (ChurchMemberDetail) -> Unit = {},
    onScanMemberQrClick: () -> Unit,
    onExportMembersClick: () -> Unit,
    onAddNewsClick: () -> Unit,
    onAddScheduleClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onSaveNewsCategory: (existingCode: String?, labelMs: String, labelEn: String, onDone: () -> Unit) -> Unit,
    onDeleteNewsCategory: (code: String, onDone: () -> Unit) -> Unit,
    onSaveScheduleCategory: (existingCode: String?, labelMs: String, labelEn: String, onDone: () -> Unit) -> Unit,
    onDeleteScheduleCategory: (code: String, onDone: () -> Unit) -> Unit,
    onOpenAddAdminDialog: () -> Unit,
    onRemoveAdminMember: (String) -> Unit,
    onCopyUid: (String) -> Unit,
    onManageAppVersionClick: () -> Unit,
    onEnsureMemberRegistrationConfig: () -> Unit,
    onSaveMemberRegistrationConfig: (portalUrl: String, accessToken: String, enabled: Boolean) -> Unit,
    onRegenerateMemberRegistrationToken: () -> Unit,
    onCopyRegistrationLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSection by rememberSaveable { mutableStateOf<AdminSection?>(null) }
    var pendingDeleteMember by remember { mutableStateOf<ChurchMemberDetail?>(null) }

    BackHandler(enabled = activeSection != null) {
        activeSection = null
    }

    val allMembers = (membersState as? UiState.Success)?.data.orEmpty()
    val publishedVersionName = appVersionConfig?.latestVersionName?.ifBlank { BuildConfig.VERSION_NAME }
        ?: BuildConfig.VERSION_NAME

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = activeSection,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "admin_section_nav",
            modifier = Modifier.fillMaxSize()
        ) { section ->
        when (section) {
            null -> {
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
                            .testTag("admin_tab_list")
                    ) {
                        // Top Counters Banner (News, Schedule, Gallery, Members)
                        Card(
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_panel_card")
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.AdminPanelSettings,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = tr("Panel Admin SIB Toburon", "SIB Toburon Admin Panel"),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = tr(
                                                "Ketik mana-mana bahagian di bawah untuk mengurus gereja.",
                                                "Tap any section below to open its management view."
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    AdminStatPill(tr("Berita", "News"), newsCount, Modifier.weight(1f))
                                    AdminStatPill(tr("Jadual", "Schedule"), schedulesCount, Modifier.weight(1f))
                                    AdminStatPill(tr("Galeri", "Gallery"), galleryCount, Modifier.weight(1f))
                                    AdminStatPill(tr("Ahli", "Members"), membersCount, Modifier.weight(1f))
                                }
                            }
                        }

                        // 1. Manage Events
                        AdminNavigationColumnCard(
                            icon = Icons.AutoMirrored.Filled.EventNote,
                            iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTintColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = tr("Urus Acara", "Manage Events"),
                            subtitle = tr(
                                "Berita, jadual, galeri, ahli & kategori berita/jadual",
                                "News, schedule, gallery, member & category management"
                            ),
                            badgeText = tr("6 Alat", "6 Tools"),
                            onClick = { activeSection = AdminSection.MANAGE_EVENTS },
                            cardTestTag = "admin_manage_events_card",
                            badgeTestTag = "admin_manage_events_badge"
                        )

                        // 2. Church Members
                        AdminNavigationColumnCard(
                            icon = Icons.Filled.Groups,
                            iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTintColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = tr("Ahli Jemaat", "Church Members"),
                            subtitle = tr("Direktori ahli berdaftar, carian, susunan, QR & eksport", "Registered members directory, search, sort, QR & export"),
                            badgeText = tr("${allMembers.size} Ahli", "${allMembers.size} Members"),
                            onClick = { activeSection = AdminSection.CHURCH_MEMBERS },
                            cardTestTag = "admin_church_members_card",
                            badgeTestTag = "admin_church_members_count"
                        )

                        // 3. Google Signed-in Users
                        AdminNavigationColumnCard(
                            icon = Icons.Filled.SupervisorAccount,
                            iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTintColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = tr("Pengguna Log Masuk Google", "Google Signed-In Users"),
                            subtitle = tr("Lihat senarai akaun Google & salin UID pengguna", "View signed-in Google accounts & copy user UIDs"),
                            badgeText = tr("${registeredUsers.size} Pengguna", "${registeredUsers.size} Users"),
                            onClick = { activeSection = AdminSection.GOOGLE_USERS },
                            cardTestTag = "admin_google_users_card",
                            badgeTestTag = "admin_google_users_count"
                        )

                        // 4. Manage Administrator
                        AdminNavigationColumnCard(
                            icon = Icons.Filled.ManageAccounts,
                            iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                            iconTintColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            title = tr("Urus Pentadbir", "Manage Administrator"),
                            subtitle = tr("Tambah atau urus pentadbir tambahan menggunakan UID", "Add or manage additional administrators by UID"),
                            badgeText = tr("${adminMembers.size} Admin", "${adminMembers.size} Admins"),
                            onClick = { activeSection = AdminSection.MANAGE_ADMINISTRATOR },
                            cardTestTag = "admin_manage_administrator_card",
                            badgeTestTag = "admin_manage_administrator_count"
                        )

                        // 5. Version Manager
                        AdminNavigationColumnCard(
                            icon = Icons.Filled.SystemUpdate,
                            iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                            iconTintColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            title = tr("Pengurus Versi", "Version Manager"),
                            subtitle = tr("Urus versi aplikasi & pautan muat turun terus", "Manage app version & direct update download link"),
                            badgeText = "v$publishedVersionName",
                            onClick = { activeSection = AdminSection.VERSION_MANAGER },
                            cardTestTag = "admin_version_manager_card",
                            badgeTestTag = "admin_version_manager_badge"
                        )

                        // 6. Member Registration QR & Web Link (directly below Version Manager) + quick-scan QR tile on right
                        val density = LocalDensity.current
                        var regCardHeightPx by remember { mutableIntStateOf(0) }
                        val regQrTileSize = if (regCardHeightPx > 0) {
                            with(density) { regCardHeightPx.toDp() }.coerceAtLeast(76.dp)
                        } else {
                            88.dp
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminNavigationColumnCard(
                                icon = Icons.Filled.QrCode2,
                                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                                iconTintColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = tr("QR & Pautan Pendaftaran Ahli", "Member Registration QR & Link"),
                                subtitle = tr(
                                    "Imbas QR atau buka borang web untuk iPhone iOS & tanpa aplikasi",
                                    "Scan QR or open web registration form for iPhone iOS & non-app users"
                                ),
                                badgeText = when {
                                    memberRegistrationConfig == null -> tr("Sediakan", "Setup")
                                    memberRegistrationConfig.enabled -> tr("Aktif", "Active")
                                    else -> tr("Dijeda", "Paused")
                                },
                                onClick = { activeSection = AdminSection.MEMBER_REGISTRATION_QR },
                                cardTestTag = "admin_member_reg_qr_card",
                                badgeTestTag = "admin_member_reg_qr_badge",
                                modifier = Modifier
                                    .weight(1f)
                                    .onSizeChanged { size ->
                                        if (size.height > 0 && size.height != regCardHeightPx) {
                                            regCardHeightPx = size.height
                                        }
                                    }
                            )

                            AdminQuickRegistrationQrTile(
                                config = memberRegistrationConfig,
                                onEnsureConfig = onEnsureMemberRegistrationConfig,
                                tileSize = regQrTileSize
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

            AdminSection.MANAGE_EVENTS -> {
                AdminManageEventsView(
                    newsCategories = newsCategories,
                    scheduleCategories = scheduleCategories,
                    isBusy = isBusy,
                    onBack = { activeSection = null },
                    onAddNewsClick = onAddNewsClick,
                    onAddScheduleClick = onAddScheduleClick,
                    onAddPhotoClick = onAddPhotoClick,
                    onAddMemberClick = onAddMember,
                    onSaveNewsCategory = onSaveNewsCategory,
                    onDeleteNewsCategory = onDeleteNewsCategory,
                    onSaveScheduleCategory = onSaveScheduleCategory,
                    onDeleteScheduleCategory = onDeleteScheduleCategory
                )
            }

            AdminSection.CHURCH_MEMBERS -> {
                ChurchMembersSubView(
                    membersState = membersState,
                    searchQuery = searchQuery,
                    onBack = { activeSection = null },
                    onSearchQueryChange = onSearchQueryChange,
                    onMemberClick = onMemberClick,
                    onAddMember = onAddMember,
                    onScanMemberQrClick = onScanMemberQrClick,
                    onExportMembersClick = onExportMembersClick
                )
            }

            AdminSection.GOOGLE_USERS -> {
                AdminGoogleUsersView(
                    registeredUsers = registeredUsers,
                    currentAdminUid = currentAdminUid,
                    onBack = { activeSection = null },
                    onCopyUid = onCopyUid
                )
            }

            AdminSection.MANAGE_ADMINISTRATOR -> {
                AdminManageAdministratorView(
                    adminMembers = adminMembers,
                    onBack = { activeSection = null },
                    onOpenAddAdminDialog = onOpenAddAdminDialog,
                    onRemoveAdminMember = onRemoveAdminMember,
                    onCopyUid = onCopyUid
                )
            }

            AdminSection.VERSION_MANAGER -> {
                AdminVersionManagerView(
                    appVersionConfig = appVersionConfig,
                    onBack = { activeSection = null },
                    onManageAppVersionClick = onManageAppVersionClick
                )
            }

            AdminSection.MEMBER_REGISTRATION_QR -> {
                AdminMemberRegistrationQrView(
                    config = memberRegistrationConfig,
                    isBusy = isBusy,
                    onBack = { activeSection = null },
                    onEnsureConfig = onEnsureMemberRegistrationConfig,
                    onSaveConfig = onSaveMemberRegistrationConfig,
                    onRegenerateToken = onRegenerateMemberRegistrationToken,
                    onCopyLink = onCopyRegistrationLink
                )
            }
        }
    }

    if (isLoadingDetail) {
        Dialog(onDismissRequest = {}) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLowest
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                    Text(
                        text = tr("Memuatkan maklumat ahli...", "Loading member details..."),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (selectedMemberDetail != null) {
        AdminMemberIdCardDialog(
            detail = selectedMemberDetail,
            isBusy = isBusy,
            onDismiss = onDismissMemberDetail,
            onEdit = { onEditMember(selectedMemberDetail) },
            onRequestDelete = { pendingDeleteMember = selectedMemberDetail },
            onExportCvPdf = { onExportMemberCvPdf(selectedMemberDetail) }
        )
    }
    }

    val memberToDelete = pendingDeleteMember
    if (memberToDelete != null) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) pendingDeleteMember = null },
            title = { Text(tr("Padam Ahli Jemaat?", "Delete Church Member?")) },
            text = {
                Text(
                    tr(
                        "Adakah anda pasti mahu memadam \"${memberToDelete.fullName}\" daripada senarai ahli SIB Toburon? Tindakan ini tidak boleh dibatalkan.",
                        "Are you sure you want to delete \"${memberToDelete.fullName}\" from the SIB Toburon members list? This action cannot be undone."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = memberToDelete.id
                        pendingDeleteMember = null
                        onDeleteMember(id)
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_member_button")
                ) {
                    Text(tr("Padam", "Delete"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteMember = null },
                    enabled = !isBusy
                ) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun ChurchMembersSubView(
    membersState: UiState<List<ChurchMemberPublic>>,
    searchQuery: String,
    onBack: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onMemberClick: (String) -> Unit,
    onAddMember: () -> Unit,
    onScanMemberQrClick: () -> Unit,
    onExportMembersClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allMembers = (membersState as? UiState.Success)?.data.orEmpty()
    var sortOrder by rememberSaveable { mutableStateOf(MemberSortOrder.NEWEST) }
    val listState = rememberLazyListState()

    val filteredMembers = remember(allMembers, searchQuery, sortOrder) {
        val q = searchQuery.trim()
        val filtered = if (q.isEmpty()) allMembers
        else allMembers.filter { it.fullName.contains(q, ignoreCase = true) }

        when (sortOrder) {
            MemberSortOrder.NEWEST -> filtered
            MemberSortOrder.ALPHABETICAL -> filtered.sortedWith(
                compareBy<ChurchMemberPublic, String>(String.CASE_INSENSITIVE_ORDER) { it.fullName.trim() }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }
    }

    val nowMillis = remember(allMembers) { System.currentTimeMillis() }
    val isListScrollable by remember(listState) {
        derivedStateOf {
            listState.canScrollForward || listState.canScrollBackward
        }
    }
    val showFastScrollSlider = membersState is UiState.Success &&
        filteredMembers.isNotEmpty() &&
        isListScrollable
    // 2 header items ("church_members_page_header", "church_members_controls") precede member rows
    val headerItemCount = 2

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = if (showFastScrollSlider) 40.dp else 16.dp,
                    top = 12.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("admin_church_members_page")
            ) {
                item(key = "church_members_page_header") {
                    AdminSubPageHeader(
                        title = tr("Ahli Jemaat", "Church Members"),
                        subtitle = if (sortOrder == MemberSortOrder.NEWEST) {
                            tr("Ahli baharu di atas • Ketik untuk Kad Ahli", "Newest at top • Tap for Member Card")
                        } else {
                            tr("Disusun A–Z • Ketik untuk Kad Ahli", "Sorted A–Z • Tap for Member Card")
                        },
                        badgeText = tr("${allMembers.size} Ahli", "${allMembers.size} Members"),
                        onBack = onBack
                    )
                }

                item(key = "church_members_controls") {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilledTonalButton(
                                    onClick = onScanMemberQrClick,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_scan_member_qr_button")
                                ) {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = tr("Imbas QR", "Scan QR"),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                OutlinedButton(
                                    onClick = onExportMembersClick,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_export_members_button")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = tr("Eksport", "Export"),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Button(
                                    onClick = onAddMember,
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("admin_fab_members")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = tr("Tambah", "Add"),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                EditorialSearchBar(
                                    query = searchQuery,
                                    onQueryChange = onSearchQueryChange,
                                    placeholder = tr("Cari nama...", "Search name..."),
                                    testTagName = "member_search_input",
                                    modifier = Modifier.weight(1f)
                                )

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier
                                        .heightIn(min = 52.dp)
                                        .testTag("member_sort_switcher")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        modifier = Modifier.padding(4.dp)
                                    ) {
                                        MemberSortChip(
                                            selected = sortOrder == MemberSortOrder.NEWEST,
                                            label = tr("Baharu", "New"),
                                            icon = Icons.Filled.Schedule,
                                            onClick = { sortOrder = MemberSortOrder.NEWEST },
                                            testTagName = "member_sort_newest"
                                        )
                                        MemberSortChip(
                                            selected = sortOrder == MemberSortOrder.ALPHABETICAL,
                                            label = "A–Z",
                                            icon = Icons.Filled.SortByAlpha,
                                            onClick = { sortOrder = MemberSortOrder.ALPHABETICAL },
                                            testTagName = "member_sort_az"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (membersState) {
                    is UiState.Loading -> {
                        item(key = "members_loading") {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp)
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    is UiState.Error -> {
                        item(key = "members_error") {
                            ErrorStateCard(message = membersState.message)
                        }
                    }

                    is UiState.Success -> {
                        if (allMembers.isEmpty()) {
                            item(key = "members_empty") {
                                EmptyBulletinState(
                                    icon = Icons.Filled.Groups,
                                    title = tr("Belum Ada Ahli Berdaftar", "No Registered Members Yet"),
                                    description = tr(
                                        "Ahli baharu yang mendaftar di tab Akaun atau ditambah oleh Admin akan muncul di bahagian atas senarai ini.",
                                        "New members who register in the Account tab or are added by Admin will appear at the top of this list."
                                    ),
                                    isAdmin = true,
                                    primaryActionLabel = tr("Tambah Ahli", "Add Member"),
                                    onPrimaryAction = onAddMember
                                )
                            }
                        } else if (filteredMembers.isEmpty()) {
                            item(key = "members_no_search_match") {
                                Surface(
                                    shape = MaterialTheme.shapes.large,
                                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Text(
                                            text = tr("Nama tidak dijumpai", "Name not found"),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = tr(
                                                "Tiada ahli yang sepadan dengan \"$searchQuery\".",
                                                "No members match \"$searchQuery\"."
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(
                                items = filteredMembers,
                                key = { _, member -> member.id }
                            ) { index, member ->
                                val createdMs = member.createdAt?.toDate()?.time
                                val isRecentlyRegistered = createdMs != null &&
                                    (nowMillis - createdMs) in 0L..NEW_MEMBER_BADGE_WINDOW_MS
                                MemberNameRow(
                                    index = index + 1,
                                    fullName = member.fullName,
                                    isNew = isRecentlyRegistered,
                                    onClick = { onMemberClick(member.id) },
                                    modifier = Modifier.testTag("member_row_${member.id}")
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showFastScrollSlider,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(top = 16.dp, bottom = 24.dp, end = 4.dp)
            ) {
                MembersFastScrollSlider(
                    listState = listState,
                    members = filteredMembers,
                    headerItemCount = headerItemCount,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

/**
 * Interactive right-side vertical fast-scroll slider for the Church Members view.
 * Supports tap-to-jump and drag-to-scrub with live position bubble indicator.
 */
@Composable
private fun MembersFastScrollSlider(
    listState: LazyListState,
    members: List<ChurchMemberPublic>,
    headerItemCount: Int,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    var trackHeightPx by remember { mutableFloatStateOf(1f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val thumbHeightDp = 48.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
    val totalListItems = headerItemCount + members.size

    val listScrollFraction by remember(listState, totalListItems) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleCount = layoutInfo.visibleItemsInfo.size
            val maxScrollableIndex = (totalListItems - visibleCount).coerceAtLeast(1)
            val firstIdx = listState.firstVisibleItemIndex.coerceAtLeast(0)
            if (visibleCount >= totalListItems) {
                0f
            } else {
                (firstIdx.toFloat() / maxScrollableIndex.toFloat()).coerceIn(0f, 1f)
            }
        }
    }

    val activeFraction = if (isDragging) dragFraction else listScrollFraction
    val usableTrackPx = (trackHeightPx - thumbHeightPx).coerceAtLeast(1f)
    val thumbOffsetY = (activeFraction * usableTrackPx).roundToInt()

    val activeMemberIndex = remember(activeFraction, members.size) {
        if (members.isEmpty()) 0
        else (activeFraction * (members.size - 1)).roundToInt().coerceIn(0, members.size - 1)
    }
    val activeMember = members.getOrNull(activeMemberIndex)

    fun scrollToFraction(fraction: Float) {
        val clamped = fraction.coerceIn(0f, 1f)
        dragFraction = clamped
        if (members.isEmpty()) return
        val targetMemberIdx = (clamped * (members.size - 1)).roundToInt().coerceIn(0, members.size - 1)
        val targetListIdx = if (clamped <= 0.01f) 0 else headerItemCount + targetMemberIdx
        coroutineScope.launch {
            listState.scrollToItem(targetListIdx)
        }
    }

    Box(
        contentAlignment = Alignment.TopEnd,
        modifier = modifier
            .onSizeChanged { size ->
                if (size.height > 0) trackHeightPx = size.height.toFloat()
            }
            .testTag("members_fast_scroll_slider")
    ) {
        AnimatedVisibility(
            visible = isDragging && activeMember != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 42.dp)
                .offset { IntOffset(x = 0, y = thumbOffsetY) }
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "#${activeMemberIndex + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = activeMember?.fullName.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                }
            }
        }

        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .pointerInput(members.size, trackHeightPx) {
                    detectTapGestures { offset ->
                        val fraction = ((offset.y - thumbHeightPx / 2f) / usableTrackPx).coerceIn(0f, 1f)
                        isDragging = true
                        scrollToFraction(fraction)
                        isDragging = false
                    }
                }
                .pointerInput(members.size, trackHeightPx) {
                    detectVerticalDragGestures(
                        onDragStart = { startOffset ->
                            isDragging = true
                            val fraction = ((startOffset.y - thumbHeightPx / 2f) / usableTrackPx).coerceIn(0f, 1f)
                            scrollToFraction(fraction)
                        },
                        onVerticalDrag = { change, _ ->
                            change.consume()
                            val fraction = ((change.position.y - thumbHeightPx / 2f) / usableTrackPx).coerceIn(0f, 1f)
                            scrollToFraction(fraction)
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false }
                    )
                }
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = if (isDragging) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDragging) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    }
                ),
                shadowElevation = if (isDragging) 6.dp else 2.dp,
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = thumbOffsetY) }
                    .width(22.dp)
                    .height(thumbHeightDp)
                    .testTag("members_fast_scroll_thumb")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Filled.DragHandle,
                        contentDescription = tr("Skrol pantas senarai ahli", "Fast scroll members list"),
                        tint = if (isDragging) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberNameRow(
    index: Int,
    fullName: String,
    isNew: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = index.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = fullName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isNew) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = tr("Baharu", "New"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = tr("Lihat maklumat $fullName", "View details for $fullName"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun MemberSortChip(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTagName: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier
            .heightIn(min = 44.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTagName)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
