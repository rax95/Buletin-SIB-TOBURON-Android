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
import com.example.data.MemberGender
import com.example.data.calculateAgeFromIsoDate
import com.example.data.epochMillisToIsoDate
import com.example.data.formatBirthDateReadable
import com.example.data.isoDateToEpochMillis
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
        denomination: String
    ) -> Unit
) {
    var fullName by remember(initial) { mutableStateOf(initial?.fullName ?: "") }
    var gender by remember(initial) { mutableStateOf(initial?.resolvedGender() ?: MemberGender.LELAKI) }
    var birthDateIso by remember(initial) { mutableStateOf(initial?.birthDate ?: "") }
    var baptismStatus by remember(initial) {
        mutableStateOf(initial?.resolvedBaptismStatus() ?: BaptismStatus.BELUM_DIBAPTIS)
    }
    var denomination by remember(initial) { mutableStateOf(initial?.denomination?.ifBlank { "SIB" } ?: "SIB") }
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
                            text = if (initial == null) "Daftar Ahli Jemaat Baharu" else "Edit Maklumat Ahli",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Hanya nama penuh dipaparkan kepada jemaat; maklumat peribadi hanya untuk Admin.",
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
                            contentDescription = "Tutup"
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
                            text = "1. Nama Penuh",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                validationError = null
                            },
                            label = { Text("Nama penuh jemaat") },
                            placeholder = { Text("Contoh: Johanes bin Lukas") },
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
                            text = "2. Jantina",
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
                                    label = { Text(option.label) },
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
                            text = "3. Tarikh Lahir (Umur Automatik)",
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
                                placeholder = { Text("Contoh: 1995-06-14") },
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
                                    contentDescription = "Pilih tarikh lahir",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Pilih")
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
                                            formatBirthDateReadable(birthDateIso)
                                        } else {
                                            "Sila pilih atau masukkan tarikh lahir"
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
                                            text = "Gunakan format YYYY-MM-DD (tidak melebihi hari ini).",
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
                                            "Umur: $calculatedAge tahun"
                                        } else {
                                            "Umur: --"
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
                            text = "4. Status Baptisan",
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
                                    label = { Text(status.label) },
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
                            text = "5. Denominasi",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = denomination,
                            onValueChange = {
                                denomination = it
                                validationError = null
                            },
                            label = { Text("Denominasi gereja") },
                            placeholder = { Text("Contoh: SIB") },
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
                        Text("Batal")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (fullName.trim().length < 2) {
                                validationError = "Nama penuh minimum 2 aksara."
                                return@Button
                            }
                            if (!isBirthDateValid) {
                                validationError = "Sila pilih tarikh lahir yang sah (YYYY-MM-DD)."
                                return@Button
                            }
                            if (denomination.trim().isEmpty()) {
                                validationError = "Sila masukkan denominasi."
                                return@Button
                            }
                            onSave(
                                fullName.trim(),
                                gender,
                                birthDateIso.trim(),
                                baptismStatus,
                                denomination.trim()
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
                        Text(if (initial == null) "Simpan Ahli" else "Kemas Kini")
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
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Batal")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
