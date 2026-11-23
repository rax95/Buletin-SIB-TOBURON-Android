package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.ChurchMemberDetail
import com.example.data.ChurchMemberPublic
import com.example.data.UiState
import com.example.ui.components.AdminMemberIdCardDialog
import com.example.ui.components.EditorialSearchBar
import com.example.ui.components.EmptyBulletinState
import com.example.ui.components.ErrorStateCard
import com.example.ui.components.LiveSyncStatusPill
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val NEW_MEMBER_BADGE_WINDOW_MS = 48L * 60L * 60L * 1000L

/**
 * Admin-only Members directory screen:
 * - Newly registered members automatically appear at the TOP of the list (#1) with a "Baharu" badge.
 * - Full scrollable list paired with an interactive right-side Fast-Scroll Slider & position callout.
 */
@Composable
fun MembersScreen(
    membersState: UiState<List<ChurchMemberPublic>>,
    searchQuery: String,
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
    val allMembers = (membersState as? UiState.Success)?.data.orEmpty()
    var pendingDeleteMember by remember { mutableStateOf<ChurchMemberDetail?>(null) }
    val listState = rememberLazyListState()

    val filteredMembers = remember(allMembers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) allMembers
        else allMembers.filter { it.fullName.contains(q, ignoreCase = true) }
    }

    val nowMillis = remember(allMembers) { System.currentTimeMillis() }
    val showFastScrollSlider = membersState is UiState.Success && filteredMembers.isNotEmpty()
    // 2 fixed header items ("members_header" + "members_search") precede member rows in LazyColumn
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
                    bottom = 104.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("members_list")
            ) {
                // 1. Editorial Header Card (Admin Directory)
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
                                            text = tr(
                                                "Ahli baharu dipaparkan di bahagian atas. Ketik nama untuk lihat Kad Ahli.",
                                                "Newest registered members appear at the top. Tap a name to view Member Card."
                                            ),
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

                // 2. Search Bar (Admin only)
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

            // Right-Side Interactive Fast-Scroll Slider
            if (showFastScrollSlider) {
                MembersFastScrollSlider(
                    listState = listState,
                    members = filteredMembers,
                    headerItemCount = headerItemCount,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .padding(top = 16.dp, bottom = 92.dp, end = 4.dp)
                )
            }
        }
    }

    // Loading dialog when admin taps a name to fetch full member record
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

    // Admin Member ID Card Detail Dialog
    if (selectedMemberDetail != null) {
        AdminMemberIdCardDialog(
            detail = selectedMemberDetail,
            isBusy = isBusy,
            onDismiss = onDismissMemberDetail,
            onEdit = { onEditMember(selectedMemberDetail) },
            onRequestDelete = { pendingDeleteMember = selectedMemberDetail }
        )
    }

    // Delete Confirmation Dialog
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

/**
 * Interactive right-side vertical fast-scroll slider for the Admin Members directory.
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

    // Compute live scroll fraction from LazyListState when user scrolls normally
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
        // Floating Position Callout Bubble shown to the left of thumb while scrubbing
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

        // Interactive 36.dp-wide touch zone with sleek visual track + draggable thumb
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
            // Vertical Slider Track Rail
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            )

            // Draggable Fast-Scroll Thumb
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
