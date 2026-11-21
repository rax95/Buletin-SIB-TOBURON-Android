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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.ChurchMemberDetail
import com.example.data.ChurchMemberPublic
import com.example.data.UiState
import com.example.data.formatBirthDateReadable
import com.example.ui.components.EditorialSearchBar
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LiveSyncStatusPill

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
                                        text = "Senarai Ahli Jemaat",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isAdmin) {
                                            "Ketuk nama ahli untuk lihat, edit, atau padam maklumat."
                                        } else {
                                            "Ketuk nama ahli untuk lihat jantina & status baptisan."
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
                                    text = "${allMembers.size} Ahli Berdaftar",
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
                    placeholder = stringResource(R.string.search_members_hint),
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
                                title = stringResource(R.string.empty_members_title),
                                description = stringResource(R.string.empty_members_desc),
                                isAdmin = isAdmin,
                                primaryActionLabel = stringResource(R.string.action_add_member),
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
                                        text = "Nama tidak dijumpai",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Tiada ahli yang sepadan dengan \"$searchQuery\".",
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
                        text = "Memuat maklumat ahli...",
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
            title = { Text("Padam Ahli Jemaat?") },
            text = {
                Text(
                    "Adakah anda pasti mahu memadam \"${memberToDelete.fullName}\" daripada senarai ahli SIB Toburon? Tindakan ini tidak boleh dibatalkan."
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
                    Text("Padam")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteMember = null },
                    enabled = !isBusy
                ) {
                    Text("Batal")
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
                contentDescription = "Lihat maklumat $fullName",
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
                            text = "Maklumat Ahli",
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
                            contentDescription = "Tutup"
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
                        label = "Jantina",
                        value = member.resolvedGenderLabel()
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.WaterDrop,
                        label = "Status Baptisan",
                        value = member.resolvedBaptismLabel()
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
                        Text("Tutup")
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
    val age = detail.calculatedAge()
    val readableBirthDate = formatBirthDateReadable(detail.birthDate)

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
                            text = "Maklumat Ahli Jemaat",
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
                            contentDescription = "Tutup"
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // 5 Member Information Rows
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Person,
                        label = "1. Nama Penuh",
                        value = detail.fullName
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Wc,
                        label = "2. Jantina",
                        value = detail.resolvedGender().label
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Cake,
                        label = "3. Tarikh Lahir",
                        value = readableBirthDate,
                        trailingBadge = if (age != null) "Umur: $age tahun" else null
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.WaterDrop,
                        label = "4. Status Baptisan",
                        value = detail.resolvedBaptismStatus().label
                    )
                    MemberDetailInfoRow(
                        icon = Icons.Filled.Church,
                        label = "5. Denominasi",
                        value = detail.denomination
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
                        Text("Padam")
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
                        Text("Edit")
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
