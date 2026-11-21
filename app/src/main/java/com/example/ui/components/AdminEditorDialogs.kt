package com.example.ui.components

import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.ChurchSchedule
import com.example.data.CoverPreset
import com.example.data.GalleryCategory
import com.example.data.GalleryPhoto
import com.example.data.ImageUtils
import com.example.data.NewsArticle
import com.example.data.NewsCategory
import com.example.data.ScheduleCategory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminNewsEditorDialog(
    initial: NewsArticle?,
    defaultAuthorName: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        summary: String,
        content: String,
        category: NewsCategory,
        authorName: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        pinned: Boolean,
        published: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var summary by remember { mutableStateOf(initial?.summary ?: "") }
    var content by remember { mutableStateOf(initial?.content ?: "") }
    var category by remember { mutableStateOf(initial?.resolvedCategory() ?: NewsCategory.WARTA) }
    var authorName by remember { mutableStateOf(initial?.authorName?.ifBlank { defaultAuthorName } ?: defaultAuthorName) }
    var coverPreset by remember { mutableStateOf(initial?.resolvedCoverPreset() ?: CoverPreset.SANCTUARY) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var imageBase64 by remember { mutableStateOf(initial?.imageBase64) }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }
    var published by remember { mutableStateOf(initial?.published ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var photoStatusNote by remember { mutableStateOf<String?>(null) }

    fun handlePickedNewsUri(uri: Uri?) {
        if (uri == null) return
        isEncodingPhoto = true
        validationError = null
        scope.launch {
            val res = ImageUtils.encodeUriToBase64(context, uri)
            isEncodingPhoto = false
            res.onSuccess { encoded ->
                imageBase64 = encoded.base64
                imageUrl = ""
                coverPreset = CoverPreset.CUSTOM
                photoStatusNote = "Foto siap diunggah (${encoded.sizeKb} KB • ${encoded.width}×${encoded.height})"
            }.onFailure { err ->
                validationError = err.localizedMessage ?: "Gagal memproses foto dari perangkat."
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedNewsUri(uri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> handlePickedNewsUri(uri) }

    fun launchNewsPhotoPicker() {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: ActivityNotFoundException) {
            openDocumentLauncher.launch(arrayOf("image/*"))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Tulis Berita Baru" else "Ubah Berita",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("Judul Berita *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("news_title_input")
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it; validationError = null },
                    label = { Text("Ringkasan Singkat *") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("news_summary_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it; validationError = null },
                    label = { Text("Isi Lengkap Warta / Berita *") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("news_content_input")
                )
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("Nama Penulis / Redaksi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Kategori", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NewsCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Sampul Gambar", style = MaterialTheme.typography.labelLarge)
                BulletinCoverImage(
                    coverPreset = coverPreset,
                    imageUrl = imageUrl.takeIf { it.isNotBlank() },
                    imageBase64 = imageBase64,
                    contentDescription = "Pratinjau sampul",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { launchNewsPhotoPicker() },
                        enabled = !isEncodingPhoto,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isEncodingPhoto) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Galeri HP")
                    }
                    OutlinedButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("image/*")) },
                        enabled = !isEncodingPhoto
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Pilih file gambar", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("File")
                    }
                    if (!imageBase64.isNullOrBlank()) {
                        OutlinedButton(onClick = {
                            imageBase64 = null
                            photoStatusNote = null
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus foto terunggah", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                if (photoStatusNote != null) {
                    Text(
                        text = photoStatusNote ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(CoverPreset.SANCTUARY, CoverPreset.WORSHIP, CoverPreset.FELLOWSHIP).forEach { preset ->
                        FilterChip(
                            selected = coverPreset == preset && imageBase64 == null,
                            onClick = {
                                coverPreset = preset
                                imageBase64 = null
                            },
                            label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Atau URL Gambar HTTPS (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sematkan sebagai Berita Utama", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publikasikan langsung ke Jemaat", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = published, onCheckedChange = { published = it })
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && !isEncodingPhoto,
                onClick = {
                    val t = title.trim()
                    val s = summary.trim()
                    val c = content.trim()
                    when {
                        t.length < 3 -> validationError = "Judul minimal 3 karakter."
                        s.length < 5 -> validationError = "Ringkasan minimal 5 karakter."
                        c.length < 10 -> validationError = "Isi berita minimal 10 karakter."
                        else -> onSave(
                            t, s, c, category, authorName, coverPreset,
                            imageUrl.trim().takeIf { it.isNotBlank() },
                            imageBase64, pinned, published
                        )
                    }
                },
                modifier = Modifier.testTag("save_news_button")
            ) {
                Text(if (isBusy) "Menyimpan…" else "Simpan & Sinkron")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

private fun formatScheduleDateLabel(utcMillis: Long): String {
    val fmt = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID")).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(Date(utcMillis))
}

private fun buildScheduleSortOrder(utcMillis: Long, startHour: Int, startMinute: Int): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    val year = cal.get(Calendar.YEAR).toLong()
    val month = (cal.get(Calendar.MONTH) + 1).toLong()
    val day = cal.get(Calendar.DAY_OF_MONTH).toLong()
    return year * 100000000L + month * 1000000L + day * 10000L + startHour * 100L + startMinute
}

private fun formatHourMinute(hour: Int, minute: Int): String =
    String.format(Locale.US, "%02d:%02d", hour, minute)

private fun parseInitialTimeRange(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return listOf(8, 0, 10, 0, 1)
    val matches = Regex("(\\d{1,2})[:.](\\d{2})").findAll(raw).toList()
    if (matches.isEmpty()) return listOf(8, 0, 10, 0, 1)
    val sh = matches[0].groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 8
    val sm = matches[0].groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
    return if (matches.size >= 2) {
        val eh = matches[1].groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: ((sh + 2) % 24)
        val em = matches[1].groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: sm
        listOf(sh, sm, eh, em, 1)
    } else {
        listOf(sh, sm, (sh + 2) % 24, sm, 0)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdminScheduleEditorDialog(
    initial: ChurchSchedule?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: ScheduleCategory,
        dateLabel: String,
        timeLabel: String,
        location: String,
        speaker: String,
        sortOrder: Long,
        recurring: Boolean,
        published: Boolean
    ) -> Unit
) {
    val defaultUtcToday = remember {
        val local = Calendar.getInstance()
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val parsedTimes = remember(initial?.timeLabel) { parseInitialTimeRange(initial?.timeLabel) }

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.resolvedCategory() ?: ScheduleCategory.IBADAH) }
    var selectedDateMillis by remember { mutableStateOf(defaultUtcToday) }
    var dateLabel by remember {
        mutableStateOf(
            initial?.dateLabel?.ifBlank { formatScheduleDateLabel(defaultUtcToday) }
                ?: formatScheduleDateLabel(defaultUtcToday)
        )
    }
    var startHour by remember { mutableIntStateOf(parsedTimes[0]) }
    var startMinute by remember { mutableIntStateOf(parsedTimes[1]) }
    var endHour by remember { mutableIntStateOf(parsedTimes[2]) }
    var endMinute by remember { mutableIntStateOf(parsedTimes[3]) }
    var useEndTime by remember { mutableStateOf(parsedTimes[4] == 1) }

    val formattedTimeLabel = remember(startHour, startMinute, endHour, endMinute, useEndTime) {
        val startStr = formatHourMinute(startHour, startMinute)
        if (useEndTime) {
            "$startStr - ${formatHourMinute(endHour, endMinute)} WITA"
        } else {
            "$startStr WITA"
        }
    }

    var location by remember { mutableStateOf(initial?.location ?: "Gedung Utama SIB Toburon") }
    var speaker by remember { mutableStateOf(initial?.speaker ?: "") }
    var sortOrderText by remember {
        mutableStateOf(
            (initial?.sortOrder ?: buildScheduleSortOrder(defaultUtcToday, parsedTimes[0], parsedTimes[1])).toString()
        )
    }
    var recurring by remember { mutableStateOf(initial?.recurring ?: false) }
    var published by remember { mutableStateOf(initial?.published ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Tambah Jadwal Kegiatan" else "Ubah Jadwal Kegiatan",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("Nama Kegiatan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("schedule_title_input")
                )

                // Hari / Tanggal — Material 3 Date Picker
                Text("Hari / Tanggal *", style = MaterialTheme.typography.labelLarge)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .testTag("schedule_date_picker_field")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = dateLabel,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Ketuk untuk pilih tarikh",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Pilih",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Waktu — Material 3 Time Picker (Mula & Tamat)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Waktu Kegiatan *", style = MaterialTheme.typography.labelLarge)
                    FilterChip(
                        selected = useEndTime,
                        onClick = { useEndTime = !useEndTime },
                        label = {
                            Text(
                                text = if (useEndTime) "Ada Waktu Tamat" else "Waktu Mula Sahaja",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showStartTimePicker = true }
                            .testTag("schedule_start_time_picker")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Mula",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatHourMinute(startHour, startMinute),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    if (useEndTime) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showEndTimePicker = true }
                                .testTag("schedule_end_time_picker")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Tamat",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatHourMinute(endHour, endMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
                Text(
                    text = "Paparan waktu: $formattedTimeLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it; validationError = null },
                    label = { Text("Lokasi *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = speaker,
                    onValueChange = { speaker = it },
                    label = { Text("Pelayan Firman / Koordinator") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; validationError = null },
                    label = { Text("Keterangan Kegiatan *") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sortOrderText,
                    onValueChange = { sortOrderText = it.filter { ch -> ch.isDigit() }.take(12) },
                    label = { Text("Angka Urutan Tampil (kecil di atas)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Kategori Kegiatan", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScheduleCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Jadwal Rutin Mingguan", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = recurring, onCheckedChange = { recurring = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publikasikan langsung ke Jemaat", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = published, onCheckedChange = { published = it })
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    val t = title.trim()
                    val d = description.trim()
                    val dt = dateLabel.trim()
                    val tm = formattedTimeLabel.trim()
                    val loc = location.trim()
                    val order = sortOrderText.toLongOrNull() ?: 500L
                    when {
                        t.length < 3 -> validationError = "Nama kegiatan minimal 3 karakter."
                        dt.length < 3 -> validationError = "Sila pilih hari / tarikh kegiatan."
                        tm.length < 3 -> validationError = "Sila pilih waktu kegiatan."
                        loc.length < 2 -> validationError = "Lokasi kegiatan minimal 2 karakter."
                        d.isEmpty() -> validationError = "Keterangan kegiatan wajib diisi."
                        else -> onSave(t, d, category, dt, tm, loc, speaker.trim(), order, recurring, published)
                    }
                },
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text(if (isBusy) "Menyimpan…" else "Simpan & Sinkron")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDateMillis = millis
                            dateLabel = formatScheduleDateLabel(millis)
                            sortOrderText = buildScheduleSortOrder(millis, startHour, startMinute).toString()
                            validationError = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Pilih Waktu Mula", style = MaterialTheme.typography.titleLarge) },
            text = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        startHour = timeState.hour
                        startMinute = timeState.minute
                        sortOrderText = buildScheduleSortOrder(selectedDateMillis, startHour, startMinute).toString()
                        showStartTimePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text("Batal") }
            }
        )
    }

    if (showEndTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Pilih Waktu Tamat", style = MaterialTheme.typography.titleLarge) },
            text = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        endHour = timeState.hour
                        endMinute = timeState.minute
                        showEndTimePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text("Batal") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPhotoEditorDialog(
    initial: GalleryPhoto?,
    defaultPhotographer: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        caption: String,
        category: GalleryCategory,
        eventDate: String,
        coverPreset: CoverPreset,
        imageUrl: String?,
        imageBase64: String?,
        photographerName: String,
        published: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val defaultTodayDate = remember {
        SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")).format(Date())
    }

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var caption by remember { mutableStateOf(initial?.caption ?: "") }
    var category by remember { mutableStateOf(initial?.resolvedCategory() ?: GalleryCategory.IBADAH) }
    var eventDate by remember { mutableStateOf(initial?.eventDate?.ifBlank { defaultTodayDate } ?: defaultTodayDate) }
    var photographerName by remember { mutableStateOf(initial?.photographerName?.ifBlank { defaultPhotographer } ?: defaultPhotographer) }
    var coverPreset by remember { mutableStateOf(initial?.resolvedCoverPreset() ?: CoverPreset.CUSTOM) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var imageBase64 by remember { mutableStateOf(initial?.imageBase64) }
    var presetExplicitlyChosen by remember { mutableStateOf(initial != null && initial.imageBase64.isNullOrBlank() && initial.imageUrl.isNullOrBlank()) }
    var published by remember { mutableStateOf(initial?.published ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var photoStatusNote by remember {
        mutableStateOf(
            if (!initial?.imageBase64.isNullOrBlank()) "Foto perangkat tersimpan" else null
        )
    }

    fun handlePickedPhotoUri(uri: Uri?) {
        if (uri == null) return
        isEncodingPhoto = true
        validationError = null
        scope.launch {
            val res = ImageUtils.encodeUriToBase64(context, uri)
            isEncodingPhoto = false
            res.onSuccess { encoded ->
                imageBase64 = encoded.base64
                imageUrl = ""
                coverPreset = CoverPreset.CUSTOM
                presetExplicitlyChosen = false
                photoStatusNote = "Foto perangkat siap diunggah (${encoded.sizeKb} KB • ${encoded.width}×${encoded.height})"
            }.onFailure { err ->
                validationError = err.localizedMessage ?: "Gagal memproses foto dari perangkat."
            }
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> handlePickedPhotoUri(uri) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> handlePickedPhotoUri(uri) }

    fun launchGalleryPhotoPicker() {
        try {
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: ActivityNotFoundException) {
            openDocumentLauncher.launch(arrayOf("image/*"))
        }
    }

    val hasSelectedPhoto = !imageBase64.isNullOrBlank() || imageUrl.trim().isNotBlank() || presetExplicitlyChosen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) "Unggah Foto Galeri" else "Ubah Info Foto",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Clickable photo preview or empty upload dropzone
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (hasSelectedPhoto) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                        else MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = !isEncodingPhoto) { launchGalleryPhotoPicker() }
                        .testTag("photo_preview_picker_box")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (hasSelectedPhoto) {
                            BulletinCoverImage(
                                coverPreset = coverPreset,
                                imageUrl = imageUrl.trim().takeIf { it.isNotBlank() },
                                imageBase64 = imageBase64,
                                contentDescription = "Pratinjau foto galeri",
                                modifier = Modifier.fillMaxWidth().height(170.dp)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Ketuk untuk Pilih Foto dari Perangkat",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Mendukung JPG, PNG, WebP, & HEIC (dikompresi otomatis)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (isEncodingPhoto) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .background(Color.Black.copy(alpha = 0.55f))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Memproses foto…",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                if (photoStatusNote != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = photoStatusNote ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { launchGalleryPhotoPicker() },
                        enabled = !isEncodingPhoto,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_gallery_photo_button")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (imageBase64 != null) "Ganti Foto" else "Pilih dari Galeri")
                    }
                    OutlinedButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("image/*")) },
                        enabled = !isEncodingPhoto,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pick_file_photo_button")
                    ) {
                        Icon(Icons.Filled.FolderOpen, contentDescription = "Pilih foto dari file perangkat", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("File")
                    }
                    if (!imageBase64.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                imageBase64 = null
                                photoStatusNote = null
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus foto", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = {
                        imageUrl = it
                        validationError = null
                        if (it.isNotBlank()) {
                            imageBase64 = null
                            photoStatusNote = null
                            coverPreset = CoverPreset.CUSTOM
                        }
                    },
                    label = { Text("Atau URL Foto HTTPS (Opsional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text("Judul Foto *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("photo_title_input")
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Cerita / Keterangan Foto") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it; validationError = null },
                    label = { Text("Tanggal Dokumentasi *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = photographerName,
                    onValueChange = { photographerName = it },
                    label = { Text("Fotografer / Tim Multimedia") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Kategori Album", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GalleryCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Publikasikan ke Galeri Jemaat", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = published, onCheckedChange = { published = it })
                }

                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && !isEncodingPhoto,
                onClick = {
                    val t = title.trim()
                    val dt = eventDate.trim()
                    val cleanUrl = imageUrl.trim().takeIf { it.isNotBlank() }
                    when {
                        imageBase64.isNullOrBlank() && cleanUrl == null && !presetExplicitlyChosen ->
                            validationError = "Pilih foto dari perangkat atau masukkan URL foto terlebih dahulu."
                        t.length < 2 -> validationError = "Judul foto minimal 2 karakter."
                        dt.length < 2 -> validationError = "Tanggal dokumentasi minimal 2 karakter."
                        else -> onSave(
                            t, caption.trim(), category, dt, coverPreset,
                            cleanUrl,
                            imageBase64, photographerName, published
                        )
                    }
                },
                modifier = Modifier.testTag("save_photo_button")
            ) {
                Text(if (isBusy) "Mengunggah…" else "Unggah & Sinkron")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
fun AdminMemberAddDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onAdd: (uid: String, displayName: String, roleTitle: String) -> Unit
) {
    var targetUid by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var roleTitle by remember { mutableStateOf("Sekretaris Jemaat") }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Pengurus Admin", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Masukkan UID akun Google pengurus gereja (dapat disalin dari tab Akun mereka) agar dapat memperbarui berita, jadwal, dan galeri.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = targetUid,
                    onValueChange = { targetUid = it; validationError = null },
                    label = { Text("UID Pengguna *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; validationError = null },
                    label = { Text("Nama Pengurus *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it; validationError = null },
                    label = { Text("Jabatan Pelayanan *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    val u = targetUid.trim()
                    val n = displayName.trim()
                    val r = roleTitle.trim()
                    when {
                        !u.matches(Regex("^[a-zA-Z0-9_\\-]+$")) -> validationError = "Format UID tidak valid."
                        n.isEmpty() -> validationError = "Nama pengurus wajib diisi."
                        r.isEmpty() -> validationError = "Jabatan pelayanan wajib diisi."
                        else -> onAdd(u, n, r)
                    }
                }
            ) {
                Text(if (isBusy) "Menyimpan…" else "Tambahkan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
