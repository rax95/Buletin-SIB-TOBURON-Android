package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.AdminMember
import com.example.data.FellowshipGroup
import com.example.data.UserBookmark
import com.example.data.UserProfile
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.LiveSyncStatusPill
import com.example.ui.components.NotificationSettingsCard

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
    newsCount: Int,
    schedulesCount: Int,
    galleryCount: Int,
    membersCount: Int,
    isBusy: Boolean,
    onSaveProfile: (String, FellowshipGroup) -> Unit,
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

    val clipboard = LocalClipboardManager.current
    var displayName by remember { mutableStateOf(userProfile?.displayName ?: googleDisplayName ?: "Jemaat SIB Toburon") }
    var selectedGroup by remember { mutableStateOf(userProfile?.resolvedGroup() ?: FellowshipGroup.UMUM) }

    LaunchedEffect(userProfile) {
        if (userProfile != null) {
            displayName = userProfile.displayName
            selectedGroup = userProfile.resolvedGroup()
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
            // 1. Account Identity Header
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
                            val initials = displayName.trim().take(2).uppercase().ifBlank { "ST" }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!email.isNullOrBlank()) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isAdmin) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = if (isAdmin) stringResource(R.string.admin_badge)
                                            else stringResource(R.string.jemaat_badge),
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
                                        text = "ID Pengguna (UID) • Ketuk untuk menyalin",
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
                                    contentDescription = "Salin UID",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
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
                                        text = "Panel Admin SIB Toburon",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Setiap pembaruan langsung tayang otomatis di perangkat seluruh jemaat.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AdminStatPill("Berita", newsCount, Modifier.weight(1f))
                                AdminStatPill("Jadwal", schedulesCount, Modifier.weight(1f))
                                AdminStatPill("Galeri", galleryCount, Modifier.weight(1f))
                                AdminStatPill("Ahli", membersCount, Modifier.weight(1f))
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
                                    Text("Tulis Berita")
                                }
                                Button(
                                    onClick = onAddScheduleClick,
                                    modifier = Modifier.testTag("admin_add_schedule_button")
                                ) {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tambah Jadwal")
                                }
                                Button(
                                    onClick = onAddPhotoClick,
                                    modifier = Modifier.testTag("admin_add_photo_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Unggah Foto")
                                }
                                Button(
                                    onClick = onAddMemberClick,
                                    modifier = Modifier.testTag("admin_add_member_button")
                                ) {
                                    Icon(Icons.Filled.Groups, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tambah Ahli")
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Pengurus Admin Tambahan (${adminMembers.size})",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                OutlinedButton(onClick = onOpenAddAdminDialog) {
                                    Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Tambah")
                                }
                            }

                            if (adminMembers.isEmpty()) {
                                Text(
                                    text = "Belum ada pengurus admin tambahan. Anda dapat menambahkan sekretaris atau tim multimedia gereja menggunakan UID mereka.",
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
                                                        contentDescription = "Cabut akses admin",
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
            }

            // 2b. Real-Time Push Notification Preferences (FCM)
            item(key = "notification_settings") {
                NotificationSettingsCard(
                    isAdmin = isAdmin,
                    onPreferencesUpdated = onNotificationPrefsUpdated
                )
            }

            // 3. Personal Congregation Profile
            item(key = "profile_editor") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Profil Jemaat",
                            style = MaterialTheme.typography.titleLarge
                        )
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Nama Tampilan") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Kelompok Persekutuan",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FellowshipGroup.entries.forEach { group ->
                                FilterChip(
                                    selected = selectedGroup == group,
                                    onClick = { selectedGroup = group },
                                    label = { Text(group.label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Button(
                            onClick = { onSaveProfile(displayName, selectedGroup) },
                            enabled = !isBusy && displayName.trim().isNotEmpty(),
                            modifier = Modifier.testTag("save_profile_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Simpan Profil")
                        }
                    }
                }
            }

            // 4. Saved Bookmarks
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
                                text = "Warta & Jadwal Tersimpan (${bookmarks.size})",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                        if (bookmarks.isEmpty()) {
                            Text(
                                text = "Ketuk ikon simpan pada berita atau jadwal kegiatan agar mudah diakses kembali kapan saja.",
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
                                                    text = if (bookmark.itemType == "SCHEDULE") "Jadwal Kegiatan" else "Berita Terkini",
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
                                                    contentDescription = "Hapus simpanan",
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
                        text = stringResource(R.string.auth_sign_out),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
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
                            text = "Akun Tamu SIB Toburon",
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
                                    text = stringResource(R.string.guest_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            LiveSyncStatusPill()
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Anda sedang membaca Berita Terkini, Jadwal Kegiatan, dan Galeri Foto SIB Toburon secara real-time tanpa perlu mendaftar. Masuk dengan Google kapan saja untuk melihat Senarai Ahli Jemaat, menyimpan warta, memberi apresiasi foto, atau mengakses Panel Admin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminStatPill("Berita", newsCount, Modifier.weight(1f))
                            AdminStatPill("Jadwal", schedulesCount, Modifier.weight(1f))
                            AdminStatPill("Galeri", galleryCount, Modifier.weight(1f))
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
                                Text(stringResource(R.string.auth_signing_in))
                            } else {
                                Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.auth_sign_in_google), fontWeight = FontWeight.Bold)
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
                            Text("Kembali ke Halaman Masuk")
                        }
                    }
                }
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
