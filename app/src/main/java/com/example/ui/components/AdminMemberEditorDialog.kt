package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BaptismStatus
import com.example.data.ChurchMemberDetail
import com.example.data.MaritalStatus
import com.example.data.MemberGender
import com.example.data.calculateAgeFromIsoDate
import com.example.data.epochMillisToIsoDate
import com.example.data.formatBirthDateReadable
import com.example.data.isoDateToEpochMillis
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
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
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        fullName: String,
        gender: MemberGender,
        birthDateIso: String,
        baptismStatus: BaptismStatus,
        denomination: String,
        maritalStatus: MaritalStatus
    ) -> Unit
) {
    val lang = LocalAppLanguage.current
    var fullName by remember(initial) { mutableStateOf(initial?.fullName ?: "") }
    var gender by remember(initial) { mutableStateOf(initial?.resolvedGender() ?: MemberGender.LELAKI) }
    var birthDateIso by remember(initial) { mutableStateOf(initial?.birthDate ?: "") }
    var baptismStatus by remember(initial) {
        mutableStateOf(initial?.resolvedBaptismStatus() ?: BaptismStatus.BELUM_DIBAPTIS)
    }
    var denomination by remember(initial) { mutableStateOf(initial?.denomination?.ifBlank { "SIB" } ?: "SIB") }
    var maritalStatus by remember(initial) {
        mutableStateOf(initial?.resolvedMaritalStatus() ?: MaritalStatus.BUJANG)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val calculatedAge = remember(birthDateIso) { calculateAgeFromIsoDate(birthDateIso) }
    val isoPattern = remember { Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$") }
    val isBirthDateValid = birthDateIso.trim().matches(isoPattern) && calculatedAge != null

    val isFormValid = fullName.trim().length in 2..120 &&
        isBirthDateValid &&
        denomination.trim().length in 1..80

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
                .padding(horizontal = 16.dp, vertical = 20.dp)
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
                            text = if (initial == null) {
                                tr("Daftar Ahli Jemaat Baharu", "Register New Church Member")
                            } else {
                                tr("Edit Maklumat Ahli", "Edit Member Details")
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tr(
                                "Jemaat hanya dapat melihat nama, jantina, dan status baptisan; maklumat penuh hanya untuk Admin.",
                                "Regular members only see name, gender, and baptism status; full details are Admin-only."
                            ),
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

                // Scrollable Form Body
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // 1. Nama Penuh
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("1. Nama Penuh", "1. Full Name"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                validationError = null
                            },
                            label = { Text(tr("Nama penuh jemaat", "Member full name")) },
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

                    // 2. Jantina
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("2. Jantina", "2. Gender"),
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

                    // 3. Tarikh Lahir (Automatik Show Umur)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tr("3. Tarikh Lahir (Umur Automatik)", "3. Date of Birth (Auto Age)"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = birthDateIso,
                                onValueChange = {
                                    birthDateIso = it
                                    validationError = null
                                },
                                label = { Text("YYYY-MM-DD") },
                                placeholder = { Text(tr("Contoh: 1995-06-14", "Example: 1995-06-14")) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Cake, contentDescription = null)
                                },
                                singleLine = true,
                                isError = birthDateIso.isNotBlank() && !isBirthDateValid,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("member_birthdate_input")
                            )
                            OutlinedButton(
                                onClick = { showDatePicker = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(56.dp)
                                    .testTag("member_birthdate_picker_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = tr("Pilih tarikh lahir", "Pick date of birth"),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(tr("Pilih", "Pick"))
                            }
                        }

                        // Auto-calculated Age Preview Pill
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (calculatedAge != null) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("member_auto_age_preview")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (calculatedAge != null) {
                                            formatBirthDateReadable(birthDateIso, lang)
                                        } else {
                                            tr(
                                                "Sila pilih atau masukkan tarikh lahir",
                                                "Please pick or enter date of birth"
                                            )
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (calculatedAge != null) {
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    if (birthDateIso.isNotBlank() && !isBirthDateValid) {
                                        Text(
                                            text = tr(
                                                "Gunakan format YYYY-MM-DD (tidak melebihi hari ini).",
                                                "Use YYYY-MM-DD format (not in the future)."
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (calculatedAge != null) {
                                        MaterialTheme.colorScheme.secondary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    }
                                ) {
                                    Text(
                                        text = if (calculatedAge != null) {
                                            tr("Umur: $calculatedAge tahun", "Age: $calculatedAge yrs")
                                        } else {
                                            tr("Umur: --", "Age: --")
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (calculatedAge != null) {
                                            MaterialTheme.colorScheme.onSecondary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Status Baptisan
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("4. Status Baptisan", "4. Baptism Status"),
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
                                    onClick = { baptismStatus = status },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.WaterDrop,
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
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("member_baptism_${status.code.lowercase()}")
                                )
                            }
                        }
                    }

                    // 5. Denominasi
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = tr("5. Denominasi", "5. Denomination"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = denomination,
                            onValueChange = {
                                denomination = it
                                validationError = null
                            },
                            label = { Text(tr("Denominasi gereja", "Church denomination")) },
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

                    // 6. Status Perkahwinan / Marital Status
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = tr("6. Status Perkahwinan", "6. Marital Status"),
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
                                    onClick = { maritalStatus = status },
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
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    TextButton(
                        onClick = { if (!isBusy) onDismiss() },
                        enabled = !isBusy
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
                                    "Sila pilih tarikh lahir yang sah (YYYY-MM-DD).",
                                    "Please select a valid date of birth (YYYY-MM-DD)."
                                )
                                return@Button
                            }
                            if (denomination.trim().isEmpty()) {
                                validationError = AppLanguageManager.tr(
                                    "Sila masukkan denominasi.",
                                    "Please enter a denomination."
                                )
                                return@Button
                            }
                            onSave(
                                fullName.trim(),
                                gender,
                                birthDateIso.trim(),
                                baptismStatus,
                                denomination.trim(),
                                maritalStatus
                            )
                        },
                        enabled = !isBusy && isFormValid,
                        modifier = Modifier.testTag("save_member_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (initial == null) tr("Simpan Ahli", "Save Member") else tr("Kemas Kini", "Update"))
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
}
