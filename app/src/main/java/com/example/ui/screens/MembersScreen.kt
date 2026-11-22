package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ChurchMemberDetail
import com.example.data.ChurchMemberPublic
import com.example.data.UiState
import com.example.data.formatBirthDateReadable
import com.example.ui.components.EditorialSearchBar
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LiveSyncStatusPill
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr

@Composable
fun MembersScreen(
    membersState: UiState<List<ChurchMemberPublic>>,
    searchQuery: String,
    isAdmin: Boolean,
    isBusy: Boolean,
    selectedMemberDetail: ChurchMemberDetail?,
    isLoadingDetail: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onMemberClick: (String) -> Unit,
    onDismissMemberDetail: () -> Unit,
    onAddMember: () -> Unit,
    onEditMember: (ChurchMemberDetail) -> Unit,
    onDeleteMember: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteMember by remember { mutableStateOf<ChurchMemberDetail?>(null) }
    var selectedPublicMember by remember { mutableStateOf<ChurchMemberPublic?>(null) }

    val allMembers = (membersState as? UiState.Success)?.data.orEmpty()
    val filteredMembers = remember(allMembers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) allMembers
        else allMembers.filter { it.fullName.contains(q, ignoreCase = true) }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .testTag("members_list")
        ) {
            // 1. Editorial Header Card
            item(key = "members_header") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Groups,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tr("Senarai Ahli Jemaat", "Church Members Directory"),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAdmin) {
                                            tr(
                                                "Ketik nama ahli untuk lihat, edit, atau padam maklumat.",
                                                "Tap a member's name to view, edit, or delete details."
                                            )
                                        } else {
                                            tr(
                                                "Ketik nama ahli untuk lihat jantina & status baptisan.",
                                                "Tap a member's name to view gender & baptism status."
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = tr(
                                        "${allMembers.size} Ahli Berdaftar",
                                        "${allMembers.size} Registered Members"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            LiveSyncStatusPill()
                        }
                    }
                }
            }

            // 2. Search Bar
            item(key = "members_search") {
                EditorialSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange,
                    placeholder = tr("Cari nama ahli jemaat...", "Search member name..."),
                    testTagName = "member_search_input"
                )
            }

            // 3. Content States
            when (membersState) {
                is UiState.Loading -> {
                    item(key = "members_loading") {
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
                                    "Senarai nama ahli jemaat SIB Toburon yang didaftarkan oleh Admin akan dipaparkan di sini.",
                                    "The list of SIB Toburon church members registered by Admin will appear here."
                                ),
                                isAdmin = isAdmin,
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
                        // Public list strictly shows ONLY the member's full name
                        itemsIndexed(
                            items = filteredMembers,
                            key = { _, member -> member.id }
                        ) { index, member ->
                            MemberNameRow(
                                index = index + 1,
                                fullName = member.fullName,
                                onClick = {
                                    if (isAdmin) {
                                        onMemberClick(member.id)
                                    } else {
                                        selectedPublicMember = member
                                    }
                                },
                                modifier = Modifier.testTag("member_row_${member.id}")
                            )
                        }
                    }
                }
            }
        }
    }

    // Loading dialog when admin taps a name to fetch private detail
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

    // Regular user (Jemaat) Member Info Dialog — ONLY Jantina & Status Baptisan
    val publicMember = selectedPublicMember
    if (!isAdmin && publicMember != null) {
        JemaatMemberInfoDialog(
            member = publicMember,
            onDismiss = { selectedPublicMember = null }
        )
    }

    // Admin Member Detail Dialog (only visible to Admin)
    if (isAdmin && selectedMemberDetail != null) {
        AdminMemberDetailDialog(
            detail = selectedMemberDetail,
            isBusy = isBusy,
            onDismiss = onDismissMemberDetail,
            onEdit = { onEditMember(selectedMemberDetail) },
            onRequestDelete = { pendingDeleteMember = selectedMemberDetail }
        )
    }

    // Delete Confirmation Dialog
    val memberToDelete = pendingDeleteMember
    if (isAdmin && memberToDelete != null) {
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
private fun MemberNameRow(
    index: Int,
    fullName: String,
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
            Spacer(Modifier.width(8.dp))
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
private fun JemaatMemberInfoDialog(
    member: ChurchMemberPublic,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 460.dp)
                .testTag("jemaat_member_info_dialog")
        ) {
            val lang = LocalAppLanguage.current
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Maklumat Ahli", "Member Info"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = member.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("Tutup", "Close")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Strictly ONLY Jantina & Status Baptisan for regular users
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Wc,
                        label = tr("Jantina", "Gender"),
                        value = member.resolvedGenderLabel(lang)
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.WaterDrop,
                        label = tr("Status Baptisan", "Baptism Status"),
                        value = member.resolvedBaptismLabel(lang)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("jemaat_member_close_button")
                    ) {
                        Text(tr("Tutup", "Close"))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminMemberDetailDialog(
    detail: ChurchMemberDetail,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val age = detail.calculatedAge()
    val readableBirthDate = formatBirthDateReadable(detail.birthDate, lang)

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .widthIn(max = 520.dp)
                .testTag("member_detail_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = tr("Maklumat Ahli Jemaat", "Church Member Details"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = detail.fullName,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { if (!isBusy) onDismiss() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("Tutup", "Close")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 6 Member Information Rows (scrollable for compact screens)
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Person,
                        label = tr("1. Nama Penuh", "1. Full Name"),
                        value = detail.fullName
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Wc,
                        label = tr("2. Jantina", "2. Gender"),
                        value = detail.resolvedGender().localizedLabel(lang)
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Cake,
                        label = tr("3. Tarikh Lahir", "3. Date of Birth"),
                        value = readableBirthDate,
                        trailingBadge = if (age != null) {
                            tr("Umur: $age tahun", "Age: $age yrs")
                        } else null
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.WaterDrop,
                        label = tr("4. Status Baptisan", "4. Baptism Status"),
                        value = detail.resolvedBaptismStatus().localizedLabel(lang)
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Church,
                        label = tr("5. Denominasi", "5. Denomination"),
                        value = detail.denomination
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Favorite,
                        label = tr("6. Status Perkahwinan", "6. Marital Status"),
                        value = detail.resolvedMaritalStatusLabel(lang)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Admin Actions: Padam & Edit
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    OutlinedButton(
                        onClick = onRequestDelete,
                        enabled = !isBusy,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("member_detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Padam", "Delete"))
                    }
                    Button(
                        onClick = onEdit,
                        enabled = !isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("member_detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Edit", "Edit"))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberDetailInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    trailingBadge: String? = null
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (trailingBadge != null) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = trailingBadge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}
