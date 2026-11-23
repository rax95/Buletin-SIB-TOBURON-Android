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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr
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
    val lang = LocalAppLanguage.current
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
                photoStatusNote = AppLanguageManager.tr(
                    "Foto sedia dimuat naik (${encoded.sizeKb} KB • ${encoded.width}×${encoded.height})",
                    "Photo ready to upload (${encoded.sizeKb} KB • ${encoded.width}×${encoded.height})"
                )
            }.onFailure { err ->
                validationError = err.localizedMessage ?: AppLanguageManager.tr(
                    "Gagal memproses foto daripada peranti.",
                    "Failed to process photo from device."
                )
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
                text = if (initial == null) {
                    tr("Tulis Berita Baharu", "Write New Article")
                } else {
                    tr("Edit Berita", "Edit Article")
                },
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
                    label = { Text(tr("Tajuk Berita *", "Article Title *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("news_title_input")
                )
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it; validationError = null },
                    label = { Text(tr("Ringkasan Singkat *", "Short Summary *")) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("news_summary_input")
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it; validationError = null },
                    label = { Text(tr("Isi Lengkap Warta / Berita *", "Full Bulletin / News Content *")) },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("news_content_input")
                )
                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text(tr("Nama Penulis / Sidang Editorial", "Author / Editorial Team")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(tr("Kategori", "Category"), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NewsCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.localizedLabel(lang), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text(tr("Gambar Sampul", "Cover Image"), style = MaterialTheme.typography.labelLarge)
                BulletinCoverImage(
                    coverPreset = coverPreset,
                    imageUrl = imageUrl.takeIf { it.isNotBlank() },
                    imageBase64 = imageBase64,
                    contentDescription = tr("Pratonton sampul", "Cover preview"),
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
                        Text(tr("Galeri Telefon", "Phone Gallery"))
                    }
                    OutlinedButton(
                        onClick = { openDocumentLauncher.launch(arrayOf("image/*")) },
                        enabled = !isEncodingPhoto
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = tr("Pilih fail gambar", "Choose image file"),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Fail", "File"))
                    }
                    if (!imageBase64.isNullOrBlank()) {
                        OutlinedButton(onClick = {
                            imageBase64 = null
                            photoStatusNote = null
                        }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = tr("Padam foto dimuat naik", "Remove uploaded photo"),
                                modifier = Modifier.size(18.dp)
                            )
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
                            label = { Text(preset.localizedLabel(lang), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text(tr("Atau URL Gambar HTTPS (Pilihan)", "Or HTTPS Image URL (Optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Sematkan sebagai Berita Utama", "Pin as Featured News"), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = pinned, onCheckedChange = { pinned = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Terbitkan terus kepada Jemaat", "Publish directly to Congregation"), style = MaterialTheme.typography.bodyMedium)
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
                        t.length < 3 -> validationError = AppLanguageManager.tr(
                            "Tajuk minimum 3 aksara.",
                            "Title must be at least 3 characters."
                        )
                        s.length < 5 -> validationError = AppLanguageManager.tr(
                            "Ringkasan minimum 5 aksara.",
                            "Summary must be at least 5 characters."
                        )
                        c.length < 10 -> validationError = AppLanguageManager.tr(
                            "Isi berita minimum 10 aksara.",
                            "Article content must be at least 10 characters."
                        )
                        else -> onSave(
                            t, s, c, category, authorName, coverPreset,
                            imageUrl.trim().takeIf { it.isNotBlank() },
                            imageBase64, pinned, published
                        )
                    }
                },
                modifier = Modifier.testTag("save_news_button")
            ) {
                Text(if (isBusy) tr("Menyimpan…", "Saving…") else tr("Simpan & Segerak", "Save & Sync"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}

private fun formatScheduleDateLabel(
    utcMillis: Long,
    lang: AppLanguage = AppLanguageManager.current()
): String {
    val fmt = SimpleDateFormat("EEEE, dd MMM yyyy", lang.locale).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return fmt.format(Date(utcMillis))
}

private fun parseInitialTimeRange(raw: String?): List<Int> {
    if (raw.isNullOrBlank()) return listOf(8, 0, 10, 0, 1)
    val matches = Regex("(\\d{1,2})[:.](\\d{2})(?:\\s*(AM|PM))?", RegexOption.IGNORE_CASE).findAll(raw).toList()
    if (matches.isEmpty()) return listOf(8, 0, 10, 0, 1)

    fun extractHour24(match: MatchResult, fallback: Int): Int {
        val rawHour = match.groupValues[1].toIntOrNull() ?: return fallback
        val meridiem = match.groupValues[3].uppercase(Locale.US)
        return when (meridiem) {
            "AM" -> if (rawHour == 12) 0 else rawHour.coerceIn(0, 11)
            "PM" -> if (rawHour == 12) 12 else (rawHour.coerceIn(1, 11) + 12)
            else -> rawHour.coerceIn(0, 23)
        }
    }

    val sh = extractHour24(matches[0], 8)
    val sm = matches[0].groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
    return if (matches.size >= 2) {
        val eh = extractHour24(matches[1], (sh + 2) % 24)
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
        recurring: Boolean,
        published: Boolean
    ) -> Unit
) {
    val lang = LocalAppLanguage.current
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
            initial?.dateLabel?.ifBlank { formatScheduleDateLabel(defaultUtcToday, lang) }
                ?: formatScheduleDateLabel(defaultUtcToday, lang)
        )
    }
    var startHour by remember { mutableIntStateOf(parsedTimes[0]) }
    var startMinute by remember { mutableIntStateOf(parsedTimes[1]) }
    var endHour by remember { mutableIntStateOf(parsedTimes[2]) }
    var endMinute by remember { mutableIntStateOf(parsedTimes[3]) }
    var useEndTime by remember { mutableStateOf(parsedTimes[4] == 1) }

    val formattedTimeLabel = remember(startHour, startMinute, endHour, endMinute, useEndTime) {
        val startStr = formatTimeAmPm(startHour, startMinute)
        if (useEndTime) {
            "$startStr - ${formatTimeAmPm(endHour, endMinute)}"
        } else {
            startStr
        }
    }

    var location by remember {
        mutableStateOf(
            initial?.location ?: AppLanguageManager.tr("Bangunan Utama SIB Toburon", "SIB Toburon Main Hall")
        )
    }
    var speaker by remember { mutableStateOf(initial?.speaker ?: "") }
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
                text = if (initial == null) {
                    tr("Tambah Jadual Kegiatan", "Add Event Schedule")
                } else {
                    tr("Edit Jadual Kegiatan", "Edit Event Schedule")
                },
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
                    label = { Text(tr("Nama Kegiatan *", "Event Name *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("schedule_title_input")
                )

                // Hari / Tarikh — Material 3 Date Picker
                Text(tr("Hari / Tarikh *", "Day / Date *"), style = MaterialTheme.typography.labelLarge)
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
                                    text = tr("Ketik untuk pilih tarikh", "Tap to select date"),
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
                                text = tr("Pilih", "Pick"),
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
                    Text(tr("Waktu Kegiatan *", "Event Time *"), style = MaterialTheme.typography.labelLarge)
                    FilterChip(
                        selected = useEndTime,
                        onClick = { useEndTime = !useEndTime },
                        label = {
                            Text(
                                text = if (useEndTime) {
                                    tr("Ada Waktu Tamat", "Has End Time")
                                } else {
                                    tr("Waktu Mula Sahaja", "Start Time Only")
                                },
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
                                    text = tr("Mula", "Start"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatTimeAmPm(startHour, startMinute),
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
                                        text = tr("Tamat", "End"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatTimeAmPm(endHour, endMinute),
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
                    text = "${tr("Paparan waktu", "Displayed time")}: $formattedTimeLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it; validationError = null },
                    label = { Text(tr("Lokasi *", "Location *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = speaker,
                    onValueChange = { speaker = it },
                    label = { Text(tr("Pelayan Firman / Penyelaras", "Speaker / Coordinator")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it; validationError = null },
                    label = { Text(tr("Keterangan Kegiatan *", "Event Details *")) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(tr("Kategori Kegiatan", "Event Category"), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ScheduleCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.localizedLabel(lang), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Jadual Berkala Mingguan", "Weekly Recurring Schedule"), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = recurring, onCheckedChange = { recurring = it })
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Terbitkan terus kepada Jemaat", "Publish directly to Congregation"), style = MaterialTheme.typography.bodyMedium)
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
                    when {
                        t.length < 3 -> validationError = AppLanguageManager.tr(
                            "Nama kegiatan minimum 3 aksara.",
                            "Event name must be at least 3 characters."
                        )
                        dt.length < 3 -> validationError = AppLanguageManager.tr(
                            "Sila pilih hari / tarikh kegiatan.",
                            "Please select the event date."
                        )
                        tm.length < 3 -> validationError = AppLanguageManager.tr(
                            "Sila pilih waktu kegiatan.",
                            "Please select the event time."
                        )
                        loc.length < 2 -> validationError = AppLanguageManager.tr(
                            "Lokasi kegiatan minimum 2 aksara.",
                            "Location must be at least 2 characters."
                        )
                        d.isEmpty() -> validationError = AppLanguageManager.tr(
                            "Keterangan kegiatan wajib diisi.",
                            "Event details are required."
                        )
                        else -> onSave(t, d, category, dt, tm, loc, speaker.trim(), recurring, published)
                    }
                },
                modifier = Modifier.testTag("save_schedule_button")
            ) {
                Text(if (isBusy) tr("Menyimpan…", "Saving…") else tr("Simpan & Segerak", "Save & Sync"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
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
                            dateLabel = formatScheduleDateLabel(millis, lang)
                            validationError = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(tr("Pilih", "Select"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(tr("Batal", "Cancel")) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text(tr("Pilih Waktu Mula", "Select Start Time"), style = MaterialTheme.typography.titleLarge) },
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
                        showStartTimePicker = false
                    }
                ) {
                    Text(tr("Pilih", "Select"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) { Text(tr("Batal", "Cancel")) }
            }
        )
    }

    if (showEndTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text(tr("Pilih Waktu Tamat", "Select End Time"), style = MaterialTheme.typography.titleLarge) },
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
                    Text(tr("Pilih", "Select"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) { Text(tr("Batal", "Cancel")) }
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
        extraImagesBase64: List<String>,
        photographerName: String,
        published: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val scope = rememberCoroutineScope()

    val defaultTodayDate = remember(lang) {
        SimpleDateFormat("dd MMM yyyy", lang.locale).format(Date())
    }

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var caption by remember { mutableStateOf(initial?.caption ?: "") }
    var category by remember { mutableStateOf(initial?.resolvedCategory() ?: GalleryCategory.IBADAH) }
    var eventDate by remember { mutableStateOf(initial?.eventDate?.ifBlank { defaultTodayDate } ?: defaultTodayDate) }
    var photographerName by remember { mutableStateOf(initial?.photographerName?.ifBlank { defaultPhotographer } ?: defaultPhotographer) }
    var coverPreset by remember { mutableStateOf(initial?.resolvedCoverPreset() ?: CoverPreset.CUSTOM) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var selectedImagesBase64 by remember { mutableStateOf(initial?.allBase64Images() ?: emptyList()) }
    var previewIndex by remember { mutableIntStateOf(0) }
    var presetExplicitlyChosen by remember {
        mutableStateOf(
            initial != null && initial.allBase64Images().isEmpty() && initial.imageUrl.isNullOrBlank()
        )
    }
    var published by remember { mutableStateOf(initial?.published ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }
    var encodingProgressText by remember { mutableStateOf<String?>(null) }
    var photoStatusNote by remember {
        mutableStateOf(
            initial?.allBase64Images()?.takeIf { it.isNotEmpty() }?.let { imgs ->
                if (imgs.size > 1) {
                    AppLanguageManager.tr("${imgs.size} foto peranti disimpan", "${imgs.size} device photos saved")
                } else {
                    AppLanguageManager.tr("1 foto peranti disimpan", "1 device photo saved")
                }
            }
        )
    }

    fun handlePickedPhotoUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val maxAllowed = ImageUtils.MAX_GALLERY_PHOTOS_PER_UPLOAD
        val remainingSlots = (maxAllowed - selectedImagesBase64.size).coerceAtLeast(0)
        if (remainingSlots <= 0) {
            validationError = AppLanguageManager.tr(
                "Maksimum $maxAllowed gambar bagi setiap muat naik galeri.",
                "Maximum $maxAllowed photos per gallery upload."
            )
            return
        }

        val urisToProcess = uris.take(remainingSlots)
        val trimmedCount = uris.size - urisToProcess.size

        isEncodingPhoto = true
        validationError = null
        scope.launch {
            val updatedList = selectedImagesBase64.toMutableList()
            var usedBytes = updatedList.sumOf { it.length }
            var failedCount = 0
            var skippedForBudget = 0

            val plannedTotal = updatedList.size + urisToProcess.size
            val targetDim = when {
                plannedTotal <= 1 -> 960
                plannedTotal <= 3 -> 840
                else -> 720
            }

            urisToProcess.forEachIndexed { idx, uri ->
                encodingProgressText = AppLanguageManager.tr(
                    "Memproses foto ${idx + 1} / ${urisToProcess.size}…",
                    "Processing photo ${idx + 1} / ${urisToProcess.size}…"
                )
                val remainingToEncode = (urisToProcess.size - idx).coerceAtLeast(1)
                val remainingBudget = (ImageUtils.MAX_GALLERY_TOTAL_BASE64_LENGTH - usedBytes).coerceAtLeast(0)
                if (remainingBudget < 48_000) {
                    skippedForBudget++
                    return@forEachIndexed
                }
                val perPhotoBudget = if (plannedTotal == 1) {
                    240_000
                } else {
                    (remainingBudget / remainingToEncode).coerceIn(55_000, 165_000)
                }

                val res = ImageUtils.encodeUriToBase64(
                    context = context,
                    uri = uri,
                    maxBase64Length = perPhotoBudget,
                    initialMaxDimensionPx = targetDim
                )
                res.onSuccess { encoded ->
                    if (usedBytes + encoded.base64.length <= ImageUtils.MAX_GALLERY_TOTAL_BASE64_LENGTH + 40_000) {
                        updatedList.add(encoded.base64)
                        usedBytes += encoded.base64.length
                    } else {
                        skippedForBudget++
                    }
                }.onFailure {
                    failedCount++
                }
            }

            isEncodingPhoto = false
            encodingProgressText = null

            if (updatedList.isNotEmpty()) {
                selectedImagesBase64 = updatedList
                previewIndex = previewIndex.coerceIn(0, updatedList.lastIndex)
                imageUrl = ""
                coverPreset = CoverPreset.CUSTOM
                presetExplicitlyChosen = false
                val totalKb = (usedBytes * 3 / 4 / 1024).coerceAtLeast(1)
                photoStatusNote = AppLanguageManager.tr(
                    "${updatedList.size} foto sedia dimuat naik (≈$totalKb KB)",
                    "${updatedList.size} photos ready to upload (≈$totalKb KB)"
                )
            }

            validationError = when {
                failedCount > 0 && updatedList.isEmpty() -> AppLanguageManager.tr(
                    "Gagal memproses foto daripada peranti.",
                    "Failed to process selected photos from device."
                )
                skippedForBudget > 0 -> AppLanguageManager.tr(
                    "$skippedForBudget foto dilangkau kerana melebihi had saiz album.",
                    "$skippedForBudget photo(s) skipped due to album size limit."
                )
                trimmedCount > 0 -> AppLanguageManager.tr(
                    "Hanya $maxAllowed gambar pertama dipilih (had maksimum per muat naik).",
                    "Only the first $maxAllowed photos were added (max per upload)."
                )
                failedCount > 0 -> AppLanguageManager.tr(
                    "$failedCount foto gagal diproses; foto selebihnya sedia dimuat naik.",
                    "$failedCount photo(s) could not be processed; remaining photos are ready."
                )
                else -> null
            }
        }
    }

    val openMultipleDocumentsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> handlePickedPhotoUris(uris) }

    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(ImageUtils.MAX_GALLERY_PHOTOS_PER_UPLOAD)
    ) { uris -> handlePickedPhotoUris(uris) }

    fun launchGalleryPhotoPicker() {
        try {
            multiPhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } catch (_: ActivityNotFoundException) {
            openMultipleDocumentsLauncher.launch(arrayOf("image/*"))
        }
    }

    val safePreviewIdx = previewIndex.coerceIn(0, (selectedImagesBase64.size - 1).coerceAtLeast(0))
    val activePreviewBase64 = selectedImagesBase64.getOrNull(safePreviewIdx)
    val hasSelectedPhoto = selectedImagesBase64.isNotEmpty() || imageUrl.trim().isNotBlank() || presetExplicitlyChosen

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial == null) {
                    tr("Muat Naik Foto Galeri", "Upload Gallery Photos")
                } else {
                    tr("Edit Maklumat Foto", "Edit Photo Album")
                },
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
                                imageBase64 = activePreviewBase64,
                                contentDescription = tr("Pratonton foto galeri", "Gallery photo preview"),
                                modifier = Modifier.fillMaxWidth().height(170.dp)
                            )
                            if (selectedImagesBase64.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.Black.copy(alpha = 0.62f),
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "${safePreviewIdx + 1} / ${selectedImagesBase64.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
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
                                    text = tr(
                                        "Ketik untuk Pilih 1 atau Banyak Foto",
                                        "Tap to Choose 1 or Multiple Photos"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = tr(
                                        "Pilih sehingga ${ImageUtils.MAX_GALLERY_PHOTOS_PER_UPLOAD} gambar sekaligus (dimampatkan automatik)",
                                        "Select up to ${ImageUtils.MAX_GALLERY_PHOTOS_PER_UPLOAD} photos at once (auto-compressed)"
                                    ),
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
                                    .background(Color.Black.copy(alpha = 0.58f))
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = encodingProgressText ?: tr("Memproses foto…", "Processing photos…"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Multi-photo thumbnail strip (when >= 1 device photo selected)
                if (selectedImagesBase64.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("selected_photos_thumbnail_strip")
                    ) {
                        itemsIndexed(selectedImagesBase64) { idx, b64 ->
                            val isActive = idx == safePreviewIdx
                            Box(modifier = Modifier.size(66.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (isActive) 2.5.dp else 1.dp,
                                        color = if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { previewIndex = idx }
                                ) {
                                    Box {
                                        BulletinCoverImage(
                                            coverPreset = CoverPreset.CUSTOM,
                                            imageUrl = null,
                                            imageBase64 = b64,
                                            contentDescription = tr("Foto ${idx + 1}", "Photo ${idx + 1}"),
                                            modifier = Modifier.size(66.dp)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(topEnd = 6.dp),
                                            color = Color.Black.copy(alpha = 0.65f),
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        ) {
                                            Text(
                                                text = if (idx == 0) tr("1", "1") else "${idx + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(2.dp)
                                        .size(20.dp)
                                        .clickable {
                                            val updated = selectedImagesBase64.toMutableList().apply { removeAt(idx) }
                                            selectedImagesBase64 = updated
                                            previewIndex = previewIndex.coerceIn(0, (updated.size - 1).coerceAtLeast(0))
                                            photoStatusNote = if (updated.isEmpty()) null else {
                                                val totalKb = (updated.sumOf { it.length } * 3 / 4 / 1024).coerceAtLeast(1)
                                                AppLanguageManager.tr(
                                                    "${updated.size} foto sedia dimuat naik (≈$totalKb KB)",
                                                    "${updated.size} photos ready to upload (≈$totalKb KB)"
                                                )
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = tr("Buang foto", "Remove photo"),
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedImagesBase64.size < ImageUtils.MAX_GALLERY_PHOTOS_PER_UPLOAD) {
                            item(key = "add_more_photo_tile") {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .size(66.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isEncodingPhoto) { launchGalleryPhotoPicker() }
                                        .testTag("add_more_photos_tile")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = tr("Tambah foto lagi", "Add more photos"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = tr("Tambah", "Add"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
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
                        enabled = !isEncodingPhoto && selectedImagesBase64.size < ImageUtils.MAX_GALLERY_PHOTOS_PER_UPLOAD,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pick_gallery_photo_button")
                    ) {
                        Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (selectedImagesBase64.isNotEmpty()) {
                                tr("Tambah Foto", "Add Photos")
                            } else {
                                tr("Pilih Foto", "Pick Photos")
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = { openMultipleDocumentsLauncher.launch(arrayOf("image/*")) },
                        enabled = !isEncodingPhoto && selectedImagesBase64.size < ImageUtils.MAX_GALLERY_PHOTOS_PER_UPLOAD,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pick_file_photo_button")
                    ) {
                        Icon(
                            Icons.Filled.FolderOpen,
                            contentDescription = tr("Pilih foto daripada fail peranti", "Choose photos from device files"),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(tr("Fail", "Files"))
                    }
                    if (selectedImagesBase64.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                selectedImagesBase64 = emptyList()
                                previewIndex = 0
                                photoStatusNote = null
                            },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = tr("Padam semua foto", "Clear all photos"),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = {
                        imageUrl = it
                        validationError = null
                        if (it.isNotBlank()) {
                            selectedImagesBase64 = emptyList()
                            previewIndex = 0
                            photoStatusNote = null
                            coverPreset = CoverPreset.CUSTOM
                        }
                    },
                    label = { Text(tr("Atau URL Foto HTTPS (Pilihan)", "Or HTTPS Photo URL (Optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; validationError = null },
                    label = { Text(tr("Tajuk Album / Foto *", "Album / Photo Title *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("photo_title_input")
                )
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text(tr("Cerita / Keterangan Foto", "Story / Photo Caption")) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = { eventDate = it; validationError = null },
                    label = { Text(tr("Tarikh Dokumentasi *", "Event Date *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = photographerName,
                    onValueChange = { photographerName = it },
                    label = { Text(tr("Jurugambar / Pasukan Multimedia", "Photographer / Media Team")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(tr("Kategori Album", "Album Category"), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    GalleryCategory.entries.forEach { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text(item.localizedLabel(lang), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(tr("Terbitkan ke Galeri Jemaat", "Publish to Congregation Gallery"), style = MaterialTheme.typography.bodyMedium)
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
                    val primaryBase64 = selectedImagesBase64.firstOrNull()
                    val extraBase64 = if (selectedImagesBase64.size > 1) selectedImagesBase64.drop(1) else emptyList()
                    when {
                        primaryBase64.isNullOrBlank() && cleanUrl == null && !presetExplicitlyChosen ->
                            validationError = AppLanguageManager.tr(
                                "Sila pilih sekurang-kurangnya satu foto daripada peranti atau masukkan URL foto.",
                                "Please pick at least one photo from your device or enter a photo URL."
                            )
                        t.length < 2 -> validationError = AppLanguageManager.tr(
                            "Tajuk foto minimum 2 aksara.",
                            "Photo title must be at least 2 characters."
                        )
                        dt.length < 2 -> validationError = AppLanguageManager.tr(
                            "Tarikh dokumentasi minimum 2 aksara.",
                            "Event date must be at least 2 characters."
                        )
                        else -> onSave(
                            t, caption.trim(), category, dt, coverPreset,
                            cleanUrl,
                            primaryBase64,
                            extraBase64,
                            photographerName, published
                        )
                    }
                },
                modifier = Modifier.testTag("save_photo_button")
            ) {
                Text(if (isBusy) tr("Memuat naik…", "Uploading…") else tr("Muat Naik & Segerak", "Upload & Sync"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
        }
    )
}

@Composable
fun AdminMemberAddDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onAdd: (uid: String, displayName: String, roleTitle: String) -> Unit
) {
    val defaultRoleTitle = tr("Setiausaha Jemaat", "Church Secretary")
    var targetUid by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var roleTitle by remember { mutableStateOf(defaultRoleTitle) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Tambah Pentadbir Admin", "Add Admin Manager"), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tr(
                        "Masukkan UID akaun Google pentadbir gereja (boleh disalin daripada tab Akaun mereka) untuk mengemas kini berita, jadual, galeri, dan ahli.",
                        "Enter the church manager's Google account UID (can be copied from their Account tab) to manage news, schedules, gallery, and members."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = targetUid,
                    onValueChange = { targetUid = it; validationError = null },
                    label = { Text(tr("UID Pengguna *", "User UID *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; validationError = null },
                    label = { Text(tr("Nama Pentadbir *", "Manager Name *")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roleTitle,
                    onValueChange = { roleTitle = it; validationError = null },
                    label = { Text(tr("Jawatan Pelayanan *", "Ministry Role *")) },
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
                        !u.matches(Regex("^[a-zA-Z0-9_\\-]+$")) -> validationError = AppLanguageManager.tr(
                            "Format UID tidak sah.",
                            "Invalid UID format."
                        )
                        n.isEmpty() -> validationError = AppLanguageManager.tr(
                            "Nama pentadbir wajib diisi.",
                            "Manager name is required."
                        )
                        r.isEmpty() -> validationError = AppLanguageManager.tr(
                            "Jawatan pelayanan wajib diisi.",
                            "Ministry role is required."
                        )
                        else -> onAdd(u, n, r)
                    }
                }
            ) {
                Text(if (isBusy) tr("Menyimpan…", "Saving…") else tr("Tambahkan", "Add"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Batal", "Cancel")) }
        }
    )
}
