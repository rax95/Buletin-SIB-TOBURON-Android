package com.example.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wc
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.R
import com.example.data.BaptismStatus
import com.example.data.ChurchMemberDetail
import com.example.data.ImageUtils
import com.example.data.MaritalStatus
import com.example.data.MemberQrUtils
import com.example.data.UserProfile
import com.example.data.formatBirthDateReadable
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import com.example.ui.theme.HarvestGold
import com.example.ui.theme.HarvestGoldLight
import com.example.ui.theme.SanctuaryPine
import com.example.ui.theme.SanctuaryPineDeep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Kad Ahli (Member ID Card) displayed at the top of the Account tab directly below the user profile.
 * Synced in real time with the Admin Members directory (/church_member_details) via verified Google email.
 *
 * Displays the exact 5 required Kad Ahli details:
 * 1. Gambar (3:4 passport photo)
 * 2. Nama (Full Name)
 * 3. Nombor ID (MyKad)
 * 4. Tarikh Lahir (with auto-calculated age)
 * 5. Nombor Keanggotaan SIB ("Belum ada" if unassigned)
 */
@Composable
fun OwnMemberIdCard(
    ownMemberDetail: ChurchMemberDetail?,
    userProfile: UserProfile?,
    currentUserDisplayName: String?,
    currentUserEmail: String?,
    onFillOrEditMemberDetails: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val effectiveName = ownMemberDetail?.fullName?.takeIf { it.isNotBlank() }
        ?: userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUserDisplayName?.takeIf { it.isNotBlank() }
        ?: tr("Ahli Jemaat SIB Toburon", "SIB Toburon Member")

    val effectivePhoto = ownMemberDetail?.passportPhotoBase64?.takeIf { it.isNotBlank() }
        ?: userProfile?.photoBase64?.takeIf { it.isNotBlank() }

    val myKadDisplay = ownMemberDetail?.myKadNumber?.takeIf { it.isNotBlank() } ?: "-"
    val sibNumberDisplay = ownMemberDetail?.resolvedSibMembershipLabel(lang)
        ?: tr("Belum ada", "Not yet assigned")
    val hasSibNumber = !ownMemberDetail?.sibMembershipNumber.isNullOrBlank()

    val birthDateDisplay = ownMemberDetail?.birthDate
        ?.takeIf { it.isNotBlank() }
        ?.let { formatBirthDateReadable(it, lang) }
        ?: "-"
    val calculatedAge = ownMemberDetail?.calculatedAge()
    val ageBadgeText = calculatedAge?.let { tr("$it thn", "$it yrs") }

    val memberQrPayload = remember(
        ownMemberDetail?.id,
        ownMemberDetail?.sibMembershipNumber,
        currentUserEmail,
        userProfile?.id
    ) {
        MemberQrUtils.buildMemberQrPayload(
            memberId = ownMemberDetail?.id,
            email = currentUserEmail ?: userProfile?.email,
            uid = userProfile?.id,
            sibNumber = ownMemberDetail?.sibMembershipNumber
        )
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
        shadowElevation = 6.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("account_member_id_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MemberIdCardBanner(
                subtitle = tr("KAD AHLI JEMAAT", "CONGREGATION MEMBER CARD"),
                onClose = null
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                // Top Row: 1. Gambar + 2. Nama + 5. Status badge
                MemberIdHeroRow(
                    fullName = effectiveName,
                    topCodeLabel = if (hasSibNumber) {
                        "SIB: $sibNumberDisplay"
                    } else {
                        tr("NO. SIB: Belum ada", "SIB NO: Not yet assigned")
                    },
                    passportPhotoBase64 = effectivePhoto,
                    statusLabel = if (ownMemberDetail != null) {
                        tr("Terpaut Automatik • Aktif", "Auto-Linked • Active")
                    } else {
                        tr("Belum Dilengkapkan", "Incomplete Profile")
                    },
                    isHighlightedStatus = ownMemberDetail != null,
                    badgeIcon = Icons.Filled.Badge,
                    subLine = currentUserEmail?.takeIf { it.isNotBlank() }
                        ?: tr("Jemaat SIB Toburon", "SIB Toburon Congregation")
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

                // Required Kad Ahli Details: Nombor ID (MyKad), Tarikh Lahir (auto umur), Nombor Keanggotaan SIB
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    IdCardFieldTile(
                        icon = Icons.Filled.CreditCard,
                        label = tr("NOMBOR ID (MYKAD)", "ID NUMBER (MYKAD)"),
                        value = myKadDisplay,
                        isMonospace = myKadDisplay != "-",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kad_ahli_mykad_value")
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IdCardFieldTile(
                            icon = Icons.Filled.Cake,
                            label = tr("TARIKH LAHIR", "DATE OF BIRTH"),
                            value = birthDateDisplay,
                            badgeText = ageBadgeText,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kad_ahli_dob_value")
                        )
                        IdCardFieldTile(
                            icon = Icons.Filled.ConfirmationNumber,
                            label = tr("NO. KEANGGOTAAN SIB", "SIB MEMBERSHIP NO."),
                            value = sibNumberDisplay,
                            isMonospace = hasSibNumber,
                            isMutedValue = !hasSibNumber,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kad_ahli_sib_number_value")
                        )
                    }

                    if (ownMemberDetail == null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tr(
                                        "Isi maklumat ahli anda sekarang. Kad Ahli dan akaun Google anda akan dipautkan serta ditambah secara automatik ke senarai ahli Admin.",
                                        "Fill in your member details now. Your Member Card and Google account will be automatically linked and added to the Admin members list."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    if (onFillOrEditMemberDetails != null) {
                        if (ownMemberDetail == null) {
                            Button(
                                onClick = onFillOrEditMemberDetails,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("account_fill_member_details_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tr("Isi Maklumat Ahli", "Fill Member Details"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onFillOrEditMemberDetails,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("account_edit_member_details_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = tr("Kemas Kini Maklumat Ahli", "Update Member Details"),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            MemberIdCardFooterStrip(
                qrPayload = memberQrPayload,
                memberName = effectiveName,
                sibBadgeLabel = if (hasSibNumber) {
                    "SIB: $sibNumberDisplay"
                } else {
                    tr("NO. SIB: Belum ada", "SIB NO: Not yet assigned")
                },
                footerLabel = if (ownMemberDetail != null) {
                    tr("KAD AHLI RASMI • SIB TOBURON", "OFFICIAL MEMBER CARD • SIB TOBURON")
                } else {
                    tr("KAD AHLI DIGITAL • SIB TOBURON", "DIGITAL MEMBER CARD • SIB TOBURON")
                }
            )
        }
    }
}

/**
 * Admin full Member ID Card modal showing passport-size portrait, synced Kad Ahli details,
 * and full Admin church member records (Gender, Marital Status + Spouse, Baptism + Denomination, Email).
 */
@Composable
fun AdminMemberIdCardDialog(
    detail: ChurchMemberDetail,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val lang = LocalAppLanguage.current
    val age = detail.calculatedAge()
    val readableBirthDate = formatBirthDateReadable(detail.birthDate, lang)
    val myKadDisplay = detail.myKadNumber?.takeIf { it.isNotBlank() } ?: "-"
    val sibNumberDisplay = detail.resolvedSibMembershipLabel(lang)
    val hasSibNumber = !detail.sibMembershipNumber.isNullOrBlank()
    val memberQrPayload = remember(detail.id, detail.sibMembershipNumber, detail.memberEmail) {
        MemberQrUtils.buildMemberQrPayload(
            memberId = detail.id,
            email = detail.memberEmail,
            sibNumber = detail.sibMembershipNumber
        )
    }
    val isBaptized = detail.resolvedBaptismStatus() == BaptismStatus.DIBAPTIS
    val isMarried = detail.resolvedMaritalStatus() == MaritalStatus.BERKAHWIN

    Dialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isBusy) {
                    detectTapGestures {
                        if (!isBusy) onDismiss()
                    }
                }
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures { /* absorb taps inside card */ }
                    }
                    .testTag("member_detail_dialog")
            ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                MemberIdCardBanner(
                    subtitle = tr("KAD AHLI RASMI", "OFFICIAL MEMBER CARD"),
                    onClose = { if (!isBusy) onDismiss() }
                )

                // Scrollable ID Card Body
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    // Top Portrait + Name + Baptism Status
                    MemberIdHeroRow(
                        fullName = detail.fullName,
                        topCodeLabel = if (hasSibNumber) {
                            "SIB: $sibNumberDisplay"
                        } else {
                            tr("NO. SIB: Belum ada", "SIB NO: Not yet assigned")
                        },
                        passportPhotoBase64 = detail.passportPhotoBase64,
                        statusLabel = detail.resolvedBaptismStatus().localizedLabel(lang),
                        isHighlightedStatus = isBaptized,
                        badgeIcon = Icons.Filled.WaterDrop,
                        subLine = if (isBaptized && detail.denomination.isNotBlank()) {
                            tr("Denominasi: ${detail.denomination}", "Denomination: ${detail.denomination}")
                        } else {
                            tr("Ahli Jemaat SIB Toburon", "SIB Toburon Member")
                        }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Row 1: MyKad + SIB Membership Number (Matches Kad Ahli)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IdCardFieldTile(
                                icon = Icons.Filled.CreditCard,
                                label = tr("NOMBOR ID (MYKAD)", "ID NUMBER (MYKAD)"),
                                value = myKadDisplay,
                                isMonospace = myKadDisplay != "-",
                                modifier = Modifier.weight(1f)
                            )
                            IdCardFieldTile(
                                icon = Icons.Filled.ConfirmationNumber,
                                label = tr("NO. KEANGGOTAAN SIB", "SIB MEMBERSHIP NO."),
                                value = sibNumberDisplay,
                                isMonospace = hasSibNumber,
                                isMutedValue = !hasSibNumber,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 2: Tarikh Lahir (+ auto umur) + Jantina
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IdCardFieldTile(
                                icon = Icons.Filled.Cake,
                                label = tr("TARIKH LAHIR", "DATE OF BIRTH"),
                                value = readableBirthDate,
                                badgeText = age?.let { tr("$it thn", "$it yrs") },
                                modifier = Modifier.weight(1f)
                            )
                            IdCardFieldTile(
                                icon = Icons.Filled.Wc,
                                label = tr("JANTINA", "GENDER"),
                                value = detail.resolvedGender().localizedLabel(lang),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Row 3: Status Perkahwinan + Status Baptisan
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IdCardFieldTile(
                                icon = Icons.Filled.Favorite,
                                label = tr("PERKAHWINAN", "MARITAL STATUS"),
                                value = detail.resolvedMaritalStatusLabel(lang),
                                modifier = Modifier.weight(1f)
                            )
                            IdCardFieldTile(
                                icon = Icons.Filled.WaterDrop,
                                label = tr("STATUS BAPTISAN", "BAPTISM STATUS"),
                                value = detail.resolvedBaptismStatus().localizedLabel(lang),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Conditional Row: Nama Suami / Nama Isteri (if Berkahwin)
                        if (isMarried && !detail.spouseName.isNullOrBlank()) {
                            IdCardFieldTile(
                                icon = Icons.Filled.Person,
                                label = detail.resolvedSpouseFieldLabel(lang).uppercase(),
                                value = detail.spouseName,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Conditional Row: Denominasi Gereja (if Sudah Dibaptis)
                        if (isBaptized && detail.denomination.isNotBlank()) {
                            IdCardFieldTile(
                                icon = Icons.Filled.Church,
                                label = tr("DENOMINASI GEREJA", "CHURCH DENOMINATION"),
                                value = detail.denomination,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Auto-linked Google Account Email (syncs automatically when member signs in with Google)
                        if (!detail.memberEmail.isNullOrBlank()) {
                            IdCardFieldTile(
                                icon = Icons.Filled.Email,
                                label = tr("AKAUN GOOGLE TERPAUT AUTOMATIK", "AUTO-LINKED GOOGLE ACCOUNT"),
                                value = detail.memberEmail,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                MemberIdCardFooterStrip(
                    qrPayload = memberQrPayload,
                    memberName = detail.fullName,
                    sibBadgeLabel = if (hasSibNumber) {
                        "SIB: $sibNumberDisplay"
                    } else {
                        tr("NO. SIB: Belum ada", "SIB NO: Not yet assigned")
                    }
                )

                // Admin Actions: Padam & Edit
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
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
}

@Composable
private fun MemberIdCardBanner(
    subtitle: String,
    onClose: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(SanctuaryPineDeep, SanctuaryPine)
                    )
                )
                .padding(
                    start = 16.dp,
                    end = if (onClose != null) 8.dp else 16.dp,
                    top = 12.dp,
                    bottom = 12.dp
                )
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
                    Image(
                        painter = painterResource(id = R.drawable.sib_toburon_logo),
                        contentDescription = tr("Logo Gereja SIB Toburon", "SIB Toburon Church Emblem"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, HarvestGoldLight, CircleShape)
                            .padding(2.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GEREJA SIB TOBURON",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = HarvestGoldLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onClose != null) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("Tutup", "Close"),
                            tint = Color.White.copy(alpha = 0.92f)
                        )
                    }
                }
            }
        }
        // Signature Harvest Gold Metallic Accent Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.5.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(HarvestGold, HarvestGoldLight, HarvestGold)
                    )
                )
        )
    }
}

@Composable
private fun MemberIdHeroRow(
    fullName: String,
    topCodeLabel: String,
    passportPhotoBase64: String?,
    statusLabel: String,
    isHighlightedStatus: Boolean,
    subLine: String,
    badgeIcon: ImageVector = Icons.Filled.WaterDrop
) {
    val decodedPhoto = remember(passportPhotoBase64) {
        ImageUtils.decodeBase64ToImageBitmap(passportPhotoBase64)
    }
    val initials = remember(fullName) {
        fullName
            .trim()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifEmpty { "ST" }
    }

    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Classic 3:4 Passport-Size Portrait Photo Frame (96.dp x 124.dp)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 4.dp,
            modifier = Modifier
                .width(96.dp)
                .height(124.dp)
                .testTag("member_id_passport_photo")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                if (decodedPhoto != null) {
                    Image(
                        bitmap = decodedPhoto,
                        contentDescription = tr("Foto pasport $fullName", "Passport photo of $fullName"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = tr("TIADA FOTO", "NO PHOTO"),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Member Identity Column
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = topCodeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = tr("NAMA", "NAME"),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    letterSpacing = 0.7.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = fullName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Status Chip
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isHighlightedStatus) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh
                }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = if (isHighlightedStatus) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isHighlightedStatus) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = subLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun IdCardFieldTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    isMonospace: Boolean = false,
    isMutedValue: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = if (isMutedValue) FontWeight.Medium else FontWeight.SemiBold,
                    color = if (isMutedValue) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (badgeText != null) {
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberIdCardFooterStrip(
    qrPayload: String,
    memberName: String,
    sibBadgeLabel: String,
    footerLabel: String = tr("AHLI BERDAFTAR • SIB TOBURON", "REGISTERED MEMBER • SIB TOBURON")
) {
    var showEnlargedQr by remember { mutableStateOf(false) }
    val qrBitmap by produceState<ImageBitmap?>(initialValue = null, qrPayload) {
        value = withContext(Dispatchers.Default) {
            MemberQrUtils.generateQrCodeBitmap(content = qrPayload, sizePx = 384)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = footerLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = tr(
                                "Ketik Kod QR untuk besarkan & imbas",
                                "Tap QR Code to enlarge & scan"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                // Scannable Member QR Code (tappable to enlarge full-size for effortless Admin scanning)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showEnlargedQr = true }
                        .testTag("member_card_qr_code")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                    ) {
                        val bitmap = qrBitmap
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = tr("Kod QR Kad Ahli $memberName", "Member Card QR Code for $memberName"),
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = tr("Kod QR Ahli", "Member QR Code"),
                                tint = SanctuaryPineDeep,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }

    if (showEnlargedQr) {
        Dialog(
            onDismissRequest = { showEnlargedQr = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                dialogWindow?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { showEnlargedQr = false }
                    }
                    .safeDrawingPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures { /* absorb taps inside card */ }
                        }
                        .testTag("member_qr_enlarged_dialog")
                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(22.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = tr("Kod QR Kad Ahli", "Member Card QR Code"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(
                            onClick = { showEnlargedQr = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = tr("Tutup", "Close")
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(240.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                        ) {
                            val enlargedBitmap = qrBitmap
                            if (enlargedBitmap != null) {
                                Image(
                                    bitmap = enlargedBitmap,
                                    contentDescription = tr("Kod QR besar $memberName", "Enlarged QR Code for $memberName"),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Text(
                        text = memberName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = sibBadgeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = tr(
                            "Tunjukkan Kod QR ini kepada Admin SIB Toburon untuk melihat maklumat penuh ahli anda.",
                            "Show this QR Code to an SIB Toburon Admin to view your full church member details."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            }
        }
    }
}
