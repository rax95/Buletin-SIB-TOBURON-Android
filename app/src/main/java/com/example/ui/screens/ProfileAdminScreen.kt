package com.example.ui.screens

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.AdminMember
import com.example.data.FellowshipGroup
import com.example.data.ImageUtils
import com.example.data.UserBookmark
import com.example.data.UserProfile
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.LiveSyncStatusPill
import com.example.ui.components.NotificationSettingsCard
import com.example.ui.components.formatIndonesianTimestamp
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LanguageSelectorCard
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileAdminScreen(
    isGuest: Boolean,
    uid: String?,
    email: String?,
    googleDisplayName: String?,
    userProfile: UserProfile?,
    bookmarks: List<UserBookmark>,
    isAdmin: Boolean,
    adminMembers: List<AdminMember>,
    registeredUsers: List<UserProfile>,
    newsCount: Int,
    schedulesCount: Int,
    galleryCount: Int,
    membersCount: Int,
    isBusy: Boolean,
    onSaveProfile: (String, FellowshipGroup, () -> Unit) -> Unit,
    onUpdateProfilePhoto: (String?, () -> Unit) -> Unit,
    onProfilePhotoError: (String) -> Unit,
    onOpenBookmark: (UserBookmark) -> Unit,
    onRemoveBookmark: (UserBookmark) -> Unit,
    onAddNewsClick: () -> Unit,
    onAddScheduleClick: () -> Unit,
    onAddPhotoClick: () -> Unit,
    onAddMemberClick: () -> Unit,
    onOpenAddAdminDialog: () -> Unit,
    onRemoveAdminMember: (String) -> Unit,
    onUidCopied: () -> Unit,
    onNotificationPrefsUpdated: (newsEnabled: Boolean, schedulesEnabled: Boolean) -> Unit,
    onGoogleSignInSuccess: () -> Unit,
    onExitGuestMode: () -> Unit,
    onSignOutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isGuest || uid.isNullOrBlank()) {
        GuestAccountView(
            newsCount = newsCount,
            schedulesCount = schedulesCount,
            galleryCount = galleryCount,
            onNotificationPrefsUpdated = onNotificationPrefsUpdated,
            onGoogleSignInSuccess = onGoogleSignInSuccess,
            onExitGuestMode = onExitGuestMode,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    val currentDisplayName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: googleDisplayName?.takeIf { it.isNotBlank() }
        ?: "Jemaat SIB Toburon"
    val currentGroup = userProfile?.resolvedGroup() ?: FellowshipGroup.UMUM
    val currentPhotoBase64 = userProfile?.photoBase64?.takeIf { it.isNotBlank() }
    val avatarBitmap = remember(currentPhotoBase64) {
        ImageUtils.decodeBase64ToImageBitmap(currentPhotoBase64)
    }

    var showMemberProfileDialog by remember { mutableStateOf(false) }
    var isUpdatingPhoto by remember { mutableStateOf(false) }

    fun handlePickedAvatarUri(uri: Uri?) {
        if (uri == null) return
        isUpdatingPhoto = true
        scope.launch {
            val encodedResult = ImageUtils.encodeUriToBase64(
                context = context,
                uri = uri,
                maxBase64Length = 120_000,
                initialMaxDimensionPx = 420
            )
            encodedResult.onSuccess { encoded ->
                onUpdateProfilePhoto(encoded.base64) {
                    isUpdatingPhoto = false
                }
            }.onFailure { err ->
                isUpdatingPhoto = false
                onProfilePhotoError(
                    err.localizedMessage ?: AppLanguageManager.tr(
                        "Gagal memproses gambar profil daripada peranti.",
                        "Failed to process profile photo from device."
                    )
                )
            }
        }
    }

    val openAvatarDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedAvatarUri(uri) }

    val avatarPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> handlePickedAvatarUri(uri) }

    fun launchAvatarPicker() {
        if (isUpdatingPhoto || isBusy) return
        try {
            avatarPhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: ActivityNotFoundException) {
            openAvatarDocLauncher.launch(arrayOf("image/*"))
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
        ) {
            // 1. Account Identity Header (Tap avatar to change photo, tap name to open Member Profile)
            item(key = "account_header") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val initials = currentDisplayName.trim().take(2).uppercase().ifBlank { "ST" }

                            // Top-left clickable Profile Photo Avatar
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .testTag("account_profile_avatar_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !isUpdatingPhoto && !isBusy) {
                                            launchAvatarPicker()
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (avatarBitmap != null) {
                                            Image(
                                                bitmap = avatarBitmap,
                                                contentDescription = tr("Tukar gambar profil", "Change profile photo"),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = initials,
                                                style = MaterialTheme.typography.titleLarge,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        }
                                        if (isUpdatingPhoto) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.45f))
                                            ) {
                                                CircularProgressIndicator(
                                                    color = Color.White,
                                                    strokeWidth = 2.5.dp,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Subtle camera badge indicating tap-to-change avatar
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondary,
                                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceContainerLowest),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.PhotoCamera,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondary,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Clickable Top User Name area -> Opens Member Profile Editor Dialog
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showMemberProfileDialog = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                                    .testTag("account_profile_name_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentDisplayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = tr("Edit profil jemaat", "Edit member profile"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                if (!email.isNullOrBlank()) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${currentGroup.localizedLabel(lang)} • ${tr("Ketik nama untuk edit profil", "Tap name to edit profile")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isAdmin) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (isAdmin) {
                                                tr("Admin Buletin", "Bulletin Admin")
                                            } else {
                                                tr("Jemaat SIB Toburon", "SIB Toburon Member")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAdmin) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    LiveSyncStatusPill()
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    clipboard.setText(AnnotatedString(uid))
                                    onUidCopied()
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tr(
                                            "ID Pengguna (UID) • Ketik untuk salin",
                                            "User ID (UID) • Tap to copy"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = uid,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = tr("Salin UID", "Copy UID"),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1b. Language Selector (Bahasa Malaysia / English)
            item(key = "language_settings") {
                LanguageSelectorCard(
                    selectedLanguage = lang,
                    onSelectLanguage = { AppLanguageManager.setLanguage(context, it) }
                )
            }

            // 2. Admin Real-Time Console (Visible to Admins)
            if (isAdmin) {
                item(key = "admin_console") {
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
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = tr("Panel Admin SIB Toburon", "SIB Toburon Admin Panel"),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = tr(
                                            "Setiap kemas kini dipaparkan secara automatik pada peranti semua jemaat.",
                                            "Every update appears automatically on all church members' devices."
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

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = onAddNewsClick,
                                    modifier = Modifier.testTag("admin_add_news_button")
                                ) {
                                    Icon(Icons.Filled.Newspaper, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Tulis Berita", "Write News"))
                                }
                                Button(
                                    onClick = onAddScheduleClick,
                                    modifier = Modifier.testTag("admin_add_schedule_button")
                                ) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Tambah Jadual", "Add Schedule"))
                                }
                                Button(
                                    onClick = onAddPhotoClick,
                                    modifier = Modifier.testTag("admin_add_photo_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Muat Naik Foto", "Upload Photo"))
                                }
                                Button(
                                    onClick = onAddMemberClick,
                                    modifier = Modifier.testTag("admin_add_member_button")
                                ) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Tambah Ahli", "Add Member"))
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = tr(
                                        "Pentadbir Tambahan (${adminMembers.size})",
                                        "Additional Admins (${adminMembers.size})"
                                    ),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                OutlinedButton(onClick = onOpenAddAdminDialog) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Tambah", "Add"))
                                }
                            }

                            if (adminMembers.isEmpty()) {
                                Text(
                                    text = tr(
                                        "Belum ada pentadbir tambahan. Anda boleh menambah setiausaha atau pasukan multimedia gereja menggunakan UID mereka.",
                                        "No additional admins yet. You can add church secretaries or media team members using their UID."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    adminMembers.forEach { member ->
                                        Surface(
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(member.displayName, style = MaterialTheme.typography.titleSmall)
                                                    Text(
                                                        text = "${member.roleTitle} • ${member.id}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onRemoveAdminMember(member.id) },
                                                    modifier = Modifier.size(48.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete,
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
                }

                item(key = "admin_registered_users") {
                    AdminGoogleUsersCard(
                        registeredUsers = registeredUsers,
                        currentAdminUid = uid,
                        onCopyUid = { targetUid ->
                            clipboard.setText(AnnotatedString(targetUid))
                            onUidCopied()
                        }
                    )
                }
            }

            // 2b. Real-Time Push Notification Preferences (FCM)
            item(key = "notification_settings") {
                NotificationSettingsCard(
                    isAdmin = isAdmin,
                    onPreferencesUpdated = onNotificationPrefsUpdated
                )
            }

            // 3. Saved Bookmarks
            item(key = "bookmarks_section") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    "Warta & Jadual Tersimpan (${bookmarks.size})",
                                    "Saved News & Schedules (${bookmarks.size})"
                                ),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        if (bookmarks.isEmpty()) {
                            Text(
                                text = tr(
                                    "Ketik ikon simpan pada berita atau jadual kegiatan agar mudah diakses semula bila-bila masa.",
                                    "Tap the bookmark icon on any news or schedule to easily revisit it anytime."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                bookmarks.forEach { bookmark ->
                                    val icon: ImageVector = if (bookmark.itemType == "SCHEDULE") {
                                        Icons.Filled.CalendarMonth
                                    } else {
                                        Icons.Filled.Newspaper
                                    }
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onOpenBookmark(bookmark) }
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = bookmark.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = if (bookmark.itemType == "SCHEDULE") {
                                                        tr("Jadual Kegiatan", "Event Schedule")
                                                    } else {
                                                        tr("Berita Terkini", "Latest News")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onRemoveBookmark(bookmark) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = tr("Padam simpanan", "Remove bookmark"),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Sign Out
            item(key = "sign_out_section") {
                OutlinedButton(
                    onClick = onSignOutClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = tr("Log Keluar", "Sign Out"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showMemberProfileDialog) {
        MemberProfileDialog(
            initialDisplayName = currentDisplayName,
            initialGroup = currentGroup,
            email = email,
            photoBase64 = currentPhotoBase64,
            isBusy = isBusy,
            isUpdatingPhoto = isUpdatingPhoto,
            onChangePhotoClick = { launchAvatarPicker() },
            onRemovePhotoClick = {
                isUpdatingPhoto = true
                onUpdateProfilePhoto(null) { isUpdatingPhoto = false }
            },
            onDismiss = { showMemberProfileDialog = false },
            onSave = { newName, newGroup ->
                onSaveProfile(newName, newGroup) {
                    showMemberProfileDialog = false
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberProfileDialog(
    initialDisplayName: String,
    initialGroup: FellowshipGroup,
    email: String?,
    photoBase64: String?,
    isBusy: Boolean,
    isUpdatingPhoto: Boolean,
    onChangePhotoClick: () -> Unit,
    onRemovePhotoClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, FellowshipGroup) -> Unit
) {
    val lang = LocalAppLanguage.current
    var draftName by remember(initialDisplayName) { mutableStateOf(initialDisplayName) }
    var draftGroup by remember(initialGroup) { mutableStateOf(initialGroup) }
    val avatarBitmap = remember(photoBase64) { ImageUtils.decodeBase64ToImageBitmap(photoBase64) }
    val initials = draftName.trim().take(2).uppercase().ifBlank { "ST" }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("member_profile_dialog"),
        title = {
            Text(
                text = tr("Profil Jemaat", "Member Profile"),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Avatar preview + quick photo controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable(enabled = !isUpdatingPhoto && !isBusy) { onChangePhotoClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = tr("Gambar profil", "Profile photo"),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            if (isUpdatingPhoto) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f))
                                ) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.5.dp,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!email.isNullOrBlank()) {
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = onChangePhotoClick,
                                enabled = !isUpdatingPhoto && !isBusy,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("dialog_change_profile_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = tr("Tukar Foto", "Change Photo"),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            if (!photoBase64.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = onRemovePhotoClick,
                                    enabled = !isUpdatingPhoto && !isBusy,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("dialog_remove_profile_photo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = tr("Padam foto profil", "Remove profile photo"),
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                OutlinedTextField(
                    value = draftName,
                    onValueChange = { draftName = it },
                    label = { Text(tr("Nama Paparan", "Display Name")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_profile_name_input")
                )

                Text(
                    text = tr("Kumpulan Persekutuan", "Fellowship Group"),
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FellowshipGroup.entries.forEach { group ->
                        FilterChip(
                            selected = draftGroup == group,
                            onClick = { draftGroup = group },
                            label = { Text(group.localizedLabel(lang), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draftName, draftGroup) },
                enabled = !isBusy && !isUpdatingPhoto && draftName.trim().isNotEmpty(),
                modifier = Modifier.testTag("save_profile_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (isBusy) tr("Menyimpan…", "Saving…") else tr("Simpan Profil", "Save Profile"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}

@Composable
private fun GuestAccountView(
    newsCount: Int,
    schedulesCount: Int,
    galleryCount: Int,
    onNotificationPrefsUpdated: (newsEnabled: Boolean, schedulesEnabled: Boolean) -> Unit,
    onGoogleSignInSuccess: () -> Unit,
    onExitGuestMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var signInError by remember { mutableStateOf<String?>(null) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
        ) {
            item(key = "guest_identity_card") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = tr("Akaun Tetamu SIB Toburon", "SIB Toburon Guest Account"),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = tr("Mod Tetamu", "Guest Mode"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            LiveSyncStatusPill()
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = tr(
                                "Anda sedang membaca Berita Terkini, Jadual Kegiatan, dan Galeri Foto SIB Toburon secara masa nyata tanpa perlu mendaftar. Log masuk dengan Google bila-bila masa untuk melihat Senarai Ahli Jemaat, menyimpan warta, menyukai foto, atau mengakses Panel Admin.",
                                "You are viewing Latest News, Event Schedules, and SIB Toburon Photo Gallery in real time without registering. Sign in with Google anytime to view Church Members, bookmark articles, like photos, or access the Admin Panel."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminStatPill(tr("Berita", "News"), newsCount, Modifier.weight(1f))
                            AdminStatPill(tr("Jadual", "Schedule"), schedulesCount, Modifier.weight(1f))
                            AdminStatPill(tr("Galeri", "Gallery"), galleryCount, Modifier.weight(1f))
                        }

                        if (signInError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = signInError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                signInError = null
                                isSigningIn = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isSigningIn = false
                                        onGoogleSignInSuccess()
                                    },
                                    onAuthError = { err ->
                                        isSigningIn = false
                                        signInError = err
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isSigningIn = false
                                    }
                                )
                            },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("guest_upgrade_google_button")
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(tr("Menyambung akaun Google…", "Connecting Google account…"))
                            } else {
                                Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    tr("Log Masuk dengan Google", "Sign In with Google"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = onExitGuestMode,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("exit_guest_mode_button")
                        ) {
                            Text(tr("Kembali ke Halaman Log Masuk", "Back to Sign-In Screen"))
                        }
                    }
                }
            }

            item(key = "guest_language_settings") {
                LanguageSelectorCard(
                    selectedLanguage = lang,
                    onSelectLanguage = { AppLanguageManager.setLanguage(context, it) }
                )
            }

            item(key = "guest_notification_settings") {
                NotificationSettingsCard(
                    isAdmin = false,
                    onPreferencesUpdated = onNotificationPrefsUpdated
                )
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun AdminGoogleUsersCard(
    registeredUsers: List<UserProfile>,
    currentAdminUid: String,
    onCopyUid: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showAllUsers by remember { mutableStateOf(false) }

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

    val displayedUsers = remember(filteredUsers, showAllUsers) {
        if (showAllUsers) filteredUsers else filteredUsers.take(8)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_google_users_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(20.dp)
                    .testTag("admin_google_users_header")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.SupervisorAccount,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = tr("Pengguna Log Masuk Google", "Google Signed-In Users"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isExpanded) {
                                tr("Ketik untuk menyembunyikan senarai", "Tap to hide list")
                            } else {
                                tr("Ketik untuk melihat senarai pengguna", "Tap to view user list")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier.testTag("admin_google_users_count")
                    ) {
                        Text(
                            text = tr("${registeredUsers.size} Pengguna", "${registeredUsers.size} Users"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) {
                            tr("Tutup senarai pengguna", "Collapse user list")
                        } else {
                            tr("Buka senarai pengguna", "Expand user list")
                        },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (registeredUsers.size > 5) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(tr("Cari nama atau emel pengguna...", "Search user name or email...")) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_google_users_search")
                )
            }

            if (registeredUsers.isEmpty()) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "Belum ada pengguna akaun Google direkodkan. Setiap jemaat yang log masuk menggunakan Google akan dipaparkan secara automatik di sini.",
                            "No Google account users recorded yet. Every member who signs in with Google will appear here automatically."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else if (filteredUsers.isEmpty()) {
                Text(
                    text = tr(
                        "Tiada pengguna sepadan dengan carian \"$searchQuery\".",
                        "No users match \"$searchQuery\"."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    displayedUsers.forEachIndexed { index, user ->
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
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_google_user_item_$index")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp)
                            ) {
                                val userAvatarBitmap = remember(user.photoBase64) {
                                    ImageUtils.decodeBase64ToImageBitmap(user.photoBase64)
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelf) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(38.dp)
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

                if (filteredUsers.size > 8) {
                    OutlinedButton(
                        onClick = { showAllUsers = !showAllUsers },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_google_users_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (showAllUsers) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (showAllUsers) {
                                tr("Papar Ringkas", "Show Less")
                            } else {
                                tr("Lihat Semua (${filteredUsers.size})", "View All (${filteredUsers.size})")
                            }
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
private fun AdminStatPill(
    label: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
