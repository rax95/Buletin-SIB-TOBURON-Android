package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BaptismStatus
import com.example.data.ChurchMemberDetail
import com.example.data.ImageUtils
import com.example.data.MaritalStatus
import com.example.data.MemberGender
import com.example.data.calculateAgeFromIsoDate
import com.example.data.epochMillisToIsoDate
import com.example.data.formatBirthDateReadable
import com.example.data.isoDateToEpochMillis
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
import kotlinx.coroutines.launch
import java.util.Calendar

private val COMMON_DENOMINATIONS = listOf(
    "SIB",
    "Katolik",
    "Anglikan",
    "PCS",
    "Baptis",
    "Methodist"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminMemberEditorDialog(
    initial: ChurchMemberDetail?,
    isSelfRegistration: Boolean = false,
    defaultFullName: String? = null,
    defaultEmail: String? = null,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        fullName: String,
        myKadNumber: String?,
        gender: MemberGender,
        birthDateIso: String,
        baptismStatus: BaptismStatus,
        denomination: String,
        maritalStatus: MaritalStatus,
        spouseName: String?,
        sibMembershipNumber: String?,
        passportPhotoBase64: String?,
        memberEmail: String?
    ) -> Unit
) {
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by remember(initial, defaultFullName) {
        mutableStateOf(initial?.fullName ?: defaultFullName?.takeIf { isSelfRegistration } ?: "")
    }
    var myKadNumber by remember(initial) { mutableStateOf(initial?.myKadNumber ?: "") }
    var birthDateIso by remember(initial) { mutableStateOf(initial?.birthDate ?: "") }
    var gender by remember(initial) { mutableStateOf(initial?.resolvedGender() ?: MemberGender.LELAKI) }
    var maritalStatus by remember(initial) {
        mutableStateOf(initial?.resolvedMaritalStatus() ?: MaritalStatus.BUJANG)
    }
    var spouseName by remember(initial) { mutableStateOf(initial?.spouseName ?: "") }
    var baptismStatus by remember(initial) {
        mutableStateOf(initial?.resolvedBaptismStatus() ?: BaptismStatus.BELUM_DIBAPTIS)
    }
    var denomination by remember(initial) {
        mutableStateOf(
            if (initial?.resolvedBaptismStatus() == BaptismStatus.DIBAPTIS) {
                initial.denomination.ifBlank { "SIB" }
            } else {
                initial?.denomination ?: ""
            }
        )
    }
    var sibMembershipNumber by remember(initial) { mutableStateOf(initial?.sibMembershipNumber ?: "") }
    var memberEmail by remember(initial, defaultEmail) {
        mutableStateOf(initial?.memberEmail ?: defaultEmail?.takeIf { isSelfRegistration } ?: "")
    }
    var passportPhotoBase64 by remember(initial) {
        mutableStateOf(initial?.passportPhotoBase64?.takeIf { it.isNotBlank() })
    }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var photoStatusNote by remember { mutableStateOf<String?>(null) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val decodedPassportBitmap = remember(passportPhotoBase64) {
        ImageUtils.decodeBase64ToImageBitmap(passportPhotoBase64)
    }

    fun handleSelectedPhotoUri(uri: Uri?, onCleanup: (() -> Unit)? = null) {
        if (uri == null) {
            onCleanup?.invoke()
            return
        }
        isEncodingPhoto = true
        photoError = null
        coroutineScope.launch {
            val encoded = try {
                ImageUtils.encodeUriToBase64(
                    context = context,
                    uri = uri,
                    maxBase64Length = 120_000,
                    initialMaxDimensionPx = 420
                )
            } finally {
                onCleanup?.invoke()
            }
            encoded.onSuccess { result ->
                passportPhotoBase64 = result.base64
                photoStatusNote = "${result.width}x${result.height} • ${result.sizeKb} KB"
                photoError = null
            }.onFailure { err ->
                photoError = err.localizedMessage ?: AppLanguageManager.tr(
                    "Gagal memproses foto pasport.",
                    "Failed to process passport photo."
                )
            }
            isEncodingPhoto = false
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handleSelectedPhotoUri(uri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> handleSelectedPhotoUri(uri) }

    val launchPassportCamera = rememberCameraCaptureLauncher(
        onPhotoCaptured = { uri, cleanup -> handleSelectedPhotoUri(uri, cleanup) },
        onError = { message -> photoError = message }
    )

    fun launchPassportGalleryPicker() {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: Exception) {
            openDocumentLauncher.launch(arrayOf("image/*"))
        }
    }

    val calculatedAge = remember(birthDateIso) { calculateAgeFromIsoDate(birthDateIso) }
    val isoPattern = remember { Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$") }
    val isBirthDateValid = birthDateIso.trim().matches(isoPattern) && calculatedAge != null
    val isDenominationValid = baptismStatus != BaptismStatus.DIBAPTIS || denomination.trim().length in 1..80

    val isFormValid = fullName.trim().length in 2..120 &&
        isBirthDateValid &&
        isDenominationValid &&
        !isEncodingPhoto

    val spouseFieldLabel = if (gender == MemberGender.PEREMPUAN) {
        tr("Nama Suami", "Husband's Name")
    } else {
        tr("Nama Isteri", "Wife's Name")
    }
    val spouseFieldPlaceholder = if (gender == MemberGender.PEREMPUAN) {
        tr("Masukkan nama penuh suami", "Enter husband's full name")
    } else {
        tr("Masukkan nama penuh isteri", "Enter wife's full name")
    }

    BackHandler(enabled = !showDatePicker && !showPhotoSourceDialog) {
        if (!isBusy) onDismiss()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.56f))
            .pointerInput(isBusy) {
                detectTapGestures {
                    if (!isBusy) onDismiss()
                }
            }
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            tonalElevation = 4.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures { /* absorb taps inside card */ }
                }
                .testTag("member_editor_dialog")
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
                            text = when {
                                isSelfRegistration && initial == null -> tr(
                                    "Isi Maklumat Ahli SIB Toburon",
                                    "Fill SIB Toburon Member Details"
                                )
                                isSelfRegistration -> tr(
                                    "Kemas Kini Maklumat Ahli",
                                    "Update Your Member Details"
                                )
                                initial == null -> tr(
                                    "Daftar Ahli Jemaat Baharu",
                                    "Register New Church Member"
                                )
                                else -> tr("Edit Maklumat Ahli", "Edit Member Details")
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isSelfRegistration) {
                                tr(
                                    "Maklumat ini akan melengkapkan Kad Ahli anda serta ditambah & dikemas kini secara automatik ke senarai ahli Admin.",
                                    "These details complete your Member Card and are automatically added & updated in the Admin members list."
                                )
                            } else {
                                tr(
                                    "Jika emel Google diisi, Kad Ahli pengguna akan terpaut secara automatik apabila mereka log masuk dengan akaun Google tersebut.",
                                    "If a Google email is provided, the user's Member Card links automatically when they sign in with that Google account."
                                )
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                // Scrollable Form Body (Exact numbered order 1–7)
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // 1. Gambar User & Nama Penuh
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = tr("1. Gambar Ahli & Nama Penuh", "1. Member Photo & Full Name"),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                // 3:4 Passport Portrait Preview Frame
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(
                                        width = 1.5.dp,
                                        color = if (decodedPassportBitmap != null) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant
                                        }
                                    ),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier
                                        .width(84.dp)
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isBusy && !isEncodingPhoto) {
                                            showPhotoSourceDialog = true
                                        }
                                        .testTag("member_passport_photo_preview")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (decodedPassportBitmap != null) {
                                            Image(
                                                bitmap = decodedPassportBitmap,
                                                contentDescription = tr("Pratonton foto pasport ahli", "Member passport photo preview"),
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                                    .padding(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(34.dp)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = "3:4",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        if (isEncodingPhoto) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
                                            ) {
                                                CircularProgressIndicator(
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    strokeWidth = 2.5.dp,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = tr(
                                            "Gambar saiz pasport untuk Kad Ahli SIB Toburon.",
                                            "Passport-size photo for SIB Toburon Member Card."
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = { launchPassportCamera() },
                                            enabled = !isBusy && !isEncodingPhoto,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("member_take_passport_photo_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoCamera,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = tr("Kamera", "Camera"),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { launchPassportGalleryPicker() },
                                            enabled = !isBusy && !isEncodingPhoto,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("member_pick_passport_photo_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PhotoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = tr("Galeri", "Gallery"),
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = { openDocumentLauncher.launch(arrayOf("image/*")) },
                                            enabled = !isBusy && !isEncodingPhoto,
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("member_pick_passport_file_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FolderOpen,
                                                contentDescription = tr("Pilih daripada fail", "Pick from files"),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    if (passportPhotoBase64 != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = photoStatusNote ?: tr("Foto pasport siap", "Passport photo ready"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            TextButton(
                                                onClick = {
                                                    passportPhotoBase64 = null
                                                    photoStatusNote = null
                                                    photoError = null
                                                },
                                                enabled = !isBusy && !isEncodingPhoto,
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.testTag("member_remove_passport_photo_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = tr("Padam", "Remove"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    if (photoError != null) {
                                        Text(
                                            text = photoError ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                validationError = null
                            },
                            label = { Text(tr("Nama penuh ahli *", "Member full name *")) },
                            placeholder = { Text(tr("Contoh: Johanes bin Lukas", "Example: John Lukas")) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_fullname_input")
                        )
                    }

                    // 2. Nombor MyKad
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("2. Nombor MyKad", "2. MyKad Number"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = myKadNumber,
                            onValueChange = {
                                if (it.length <= 30) {
                                    myKadNumber = it
                                    validationError = null
                                }
                            },
                            label = { Text(tr("Nombor ID (MyKad)", "ID Number (MyKad)")) },
                            placeholder = { Text("950614-12-5431") },
                            leadingIcon = {
                                Icon(Icons.Filled.CreditCard, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_mykad_input")
                        )
                    }

                    // 3. Tarikh Lahir (Date Picker + Auto Umur di sebelah kanan)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("3. Tarikh Lahir *", "3. Date of Birth *"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Clickable Date Picker Selector on the left
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isBirthDateValid) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isBusy) { showDatePicker = true }
                                    .testTag("member_birthdate_picker_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarMonth,
                                        contentDescription = tr("Pilih tarikh lahir", "Pick date of birth"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isBirthDateValid) {
                                                formatBirthDateReadable(birthDateIso, lang)
                                            } else {
                                                tr("Pilih tarikh lahir", "Select date of birth")
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isBirthDateValid) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isBirthDateValid) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isBirthDateValid) {
                                                birthDateIso
                                            } else {
                                                tr("Ketik untuk buka kalendar", "Tap to open calendar")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Auto-calculated Age Box immediately on the right
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (calculatedAge != null) {
                                    MaterialTheme.colorScheme.secondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                },
                                modifier = Modifier
                                    .heightIn(min = 56.dp)
                                    .testTag("member_auto_age_preview")
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = tr("UMUR", "AGE"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (calculatedAge != null) {
                                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Text(
                                        text = if (calculatedAge != null) {
                                            tr("$calculatedAge thn", "$calculatedAge yrs")
                                        } else {
                                            "--"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (calculatedAge != null) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Jantina
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("4. Jantina", "4. Gender"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MemberGender.entries.forEach { option ->
                                FilterChip(
                                    selected = gender == option,
                                    onClick = { gender = option },
                                    label = { Text(option.localizedLabel(lang)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("member_gender_${option.code.lowercase()}")
                                )
                            }
                        }
                    }

                    // 5. Status Perkahwinan (Marital Status) + Conditional Nama Suami / Nama Isteri
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tr("5. Status Perkahwinan", "5. Marital Status"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MaritalStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = maritalStatus == status,
                                    onClick = {
                                        maritalStatus = status
                                        if (status != MaritalStatus.BERKAHWIN) {
                                            spouseName = ""
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = { Text(status.localizedLabel(lang)) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier.testTag("member_marital_${status.code.lowercase()}")
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = maritalStatus == MaritalStatus.BERKAHWIN,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            OutlinedTextField(
                                value = spouseName,
                                onValueChange = {
                                    if (it.length <= 120) {
                                        spouseName = it
                                        validationError = null
                                    }
                                },
                                label = { Text(spouseFieldLabel) },
                                placeholder = { Text(spouseFieldPlaceholder) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Favorite, contentDescription = null)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                                    .testTag("member_spouse_name_input")
                            )
                        }
                    }

                    // 6. Status Baptisan + Conditional Denominasi Gereja (only when Sudah Dibaptis)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tr("6. Status Baptisan", "6. Baptism Status"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BaptismStatus.entries.forEach { status ->
                                FilterChip(
                                    selected = baptismStatus == status,
                                    onClick = {
                                        baptismStatus = status
                                        if (status == BaptismStatus.DIBAPTIS && denomination.isBlank()) {
                                            denomination = "SIB"
                                        }
                                        validationError = null
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.WaterDrop,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = status.localizedLabel(lang),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("member_baptism_${status.code.lowercase()}")
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = baptismStatus == BaptismStatus.DIBAPTIS,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                OutlinedTextField(
                                    value = denomination,
                                    onValueChange = {
                                        if (it.length <= 80) {
                                            denomination = it
                                            validationError = null
                                        }
                                    },
                                    label = { Text(tr("Nama Denominasi Gereja *", "Church Denomination *")) },
                                    placeholder = { Text(tr("Contoh: SIB", "Example: SIB")) },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Church, contentDescription = null)
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("member_denomination_input")
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    COMMON_DENOMINATIONS.forEach { preset ->
                                        FilterChip(
                                            selected = denomination.trim().equals(preset, ignoreCase = true),
                                            onClick = { denomination = preset },
                                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 7. Nombor Keanggotaan SIB (Jika ada; kosong = "Belum ada")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("7. Nombor Keanggotaan SIB", "7. SIB Membership Number"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = sibMembershipNumber,
                            onValueChange = {
                                if (it.length <= 40) {
                                    sibMembershipNumber = it
                                    validationError = null
                                }
                            },
                            label = { Text(tr("Nombor Keanggotaan SIB (Jika ada)", "SIB Membership Number (Optional)")) },
                            placeholder = { Text(tr("Belum ada", "Not yet assigned")) },
                            leadingIcon = {
                                Icon(Icons.Filled.ConfirmationNumber, contentDescription = null)
                            },
                            supportingText = {
                                Text(
                                    tr(
                                        "Biarkan kosong jika belum ada nombor keanggotaan (akan dipaparkan sebagai \"Belum ada\").",
                                        "Leave empty if unassigned (will be displayed as \"Not yet assigned\")."
                                    )
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_sib_number_input")
                        )
                    }

                    if (!isSelfRegistration) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                        // 8. Emel Google Ahli (Pilihan • Pautan Automatik apabila ahli log masuk Google)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = tr(
                                    "8. Emel Google Ahli (Pilihan • Pautan Automatik)",
                                    "8. Member Google Email (Optional • Auto-Link)"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            OutlinedTextField(
                                value = memberEmail,
                                onValueChange = {
                                    memberEmail = it
                                    validationError = null
                                },
                                label = { Text(tr("Emel Google ahli (Pilihan)", "Member Google email (Optional)")) },
                                placeholder = { Text("contoh@gmail.com") },
                                leadingIcon = {
                                    Icon(Icons.Filled.Email, contentDescription = null)
                                },
                                supportingText = {
                                    Text(
                                        if (initial == null) {
                                            tr(
                                                "Jika Admin mendaftarkan ahli terlebih dahulu, masukkan emel Google mereka. Apabila mereka log masuk Google dengan emel ini, Kad Ahli akan terpaut secara automatik.",
                                                "If you register a member first, enter their Google email here. When they sign in with Google using this email, their Member Card links automatically."
                                            )
                                        } else {
                                            tr(
                                                "Kad Ahli terpaut secara automatik dengan akaun Google yang sepadan dengan emel ini.",
                                                "The Member Card links automatically to the Google account matching this email."
                                            )
                                        }
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("member_email_input")
                            )
                        }
                    }

                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Footer Actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    TextButton(
                        onClick = { if (!isBusy) onDismiss() },
                        enabled = !isBusy,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(tr("Batal", "Cancel"))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.trim().length < 2) {
                                validationError = AppLanguageManager.tr(
                                    "Nama penuh minimum 2 aksara.",
                                    "Full name must be at least 2 characters."
                                )
                                return@Button
                            }
                            if (!isBirthDateValid) {
                                validationError = AppLanguageManager.tr(
                                    "Sila pilih tarikh lahir menggunakan kalendar.",
                                    "Please select a valid date of birth from the calendar."
                                )
                                return@Button
                            }
                            if (baptismStatus == BaptismStatus.DIBAPTIS && denomination.trim().isEmpty()) {
                                validationError = AppLanguageManager.tr(
                                    "Sila masukkan nama denominasi gereja.",
                                    "Please enter the church denomination."
                                )
                                return@Button
                            }
                            val effectiveDenomination = if (baptismStatus == BaptismStatus.DIBAPTIS) {
                                denomination.trim()
                            } else {
                                ""
                            }
                            val effectiveSpouse = if (maritalStatus == MaritalStatus.BERKAHWIN) {
                                spouseName.trim().takeIf { it.isNotEmpty() }
                            } else {
                                null
                            }
                            onSave(
                                fullName.trim(),
                                myKadNumber.trim().takeIf { it.isNotEmpty() },
                                gender,
                                birthDateIso.trim(),
                                baptismStatus,
                                effectiveDenomination,
                                maritalStatus,
                                effectiveSpouse,
                                sibMembershipNumber.trim().takeIf { it.isNotEmpty() },
                                passportPhotoBase64,
                                memberEmail.trim().lowercase().takeIf { it.isNotEmpty() }
                            )
                        },
                        enabled = !isBusy && !isEncodingPhoto && isFormValid,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("save_member_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                isSelfRegistration && initial == null -> tr("Daftar & Simpan", "Register & Save")
                                initial == null -> tr("Simpan Ahli", "Save Member")
                                else -> tr("Kemas Kini", "Update")
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val todayMillis = remember {
            Calendar.getInstance().timeInMillis
        }
        val currentYear = remember {
            Calendar.getInstance().get(Calendar.YEAR)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = isoDateToEpochMillis(birthDateIso),
            yearRange = 1900..currentYear,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            birthDateIso = epochMillisToIsoDate(millis)
                            validationError = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(tr("Pilih", "Select"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPhotoSourceDialog) {
        PhotoSourceChooserDialog(
            title = tr("Gambar Kad Ahli", "Member Card Photo"),
            subtitle = tr(
                "Ambil gambar baharu dengan kamera atau pilih foto saiz pasport daripada galeri.",
                "Take a new photo with your camera or choose a passport-style photo from your gallery."
            ),
            hasExistingPhoto = passportPhotoBase64 != null,
            onTakePhotoClick = { launchPassportCamera() },
            onChooseGalleryClick = { launchPassportGalleryPicker() },
            onRemovePhotoClick = {
                passportPhotoBase64 = null
                photoStatusNote = null
                photoError = null
            },
            onDismiss = { showPhotoSourceDialog = false },
            tagPrefix = "member_passport"
        )
    }
}
