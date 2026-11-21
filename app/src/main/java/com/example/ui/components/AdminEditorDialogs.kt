package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isEncodingPhoto = true
            validationError = null
            scope.launch {
                val res = ImageUtils.encodeUriToBase64(context, uri)
                isEncodingPhoto = false
                res.onSuccess { b64 ->
                    imageBase64 = b64
                    coverPreset = CoverPreset.CUSTOM
                }.onFailure { err ->
                    validationError = err.localizedMessage ?: "Gagal memproses foto."
                }
            }
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
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isEncodingPhoto,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isEncodingPhoto) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih Foto")
                    }
                    if (!imageBase64.isNullOrBlank()) {
                        OutlinedButton(onClick = { imageBase64 = null }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus foto terunggah", modifier = Modifier.size(18.dp))
                        }
                    }
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

@OptIn(ExperimentalLayoutApi::class)
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
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var category by remember { mutableStateOf(initial?.resolvedCategory() ?: ScheduleCategory.IBADAH) }
    var dateLabel by remember { mutableStateOf(initial?.dateLabel ?: "Setiap Minggu") }
    var timeLabel by remember { mutableStateOf(initial?.timeLabel ?: "08:00 - 10:00 WITA") }
    var location by remember { mutableStateOf(initial?.location ?: "Gedung Utama SIB Toburon") }
    var speaker by remember { mutableStateOf(initial?.speaker ?: "") }
    var sortOrderText by remember { mutableStateOf((initial?.sortOrder ?: 100L).toString()) }
    var recurring by remember { mutableStateOf(initial?.recurring ?: true) }
    var published by remember { mutableStateOf(initial?.published ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
                OutlinedTextField(
                    value = dateLabel,
                    onValueChange = { dateLabel = it; validationError = null },
                    label = { Text("Hari / Tanggal * (cth: Minggu, 22 Nov)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeLabel,
                    onValueChange = { timeLabel = it; validationError = null },
                    label = { Text("Waktu * (cth: 08:00 - 10:00 WITA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
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
                    val tm = timeLabel.trim()
                    val loc = location.trim()
                    val order = sortOrderText.toLongOrNull() ?: 500L
                    when {
                        t.length < 3 -> validationError = "Nama kegiatan minimal 3 karakter."
                        dt.length < 3 -> validationError = "Hari/tanggal minimal 3 karakter."
                        tm.length < 3 -> validationError = "Waktu kegiatan minimal 3 karakter."
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

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var caption by remember { mutableStateOf(initial?.caption ?: "") }
    var category by remember { mutableStateOf(initial?.resolvedCategory() ?: GalleryCategory.IBADAH) }
    var eventDate by remember { mutableStateOf(initial?.eventDate ?: "November 2026") }
    var photographerName by remember { mutableStateOf(initial?.photographerName?.ifBlank { defaultPhotographer } ?: defaultPhotographer) }
    var coverPreset by remember { mutableStateOf(initial?.resolvedCoverPreset() ?: CoverPreset.FELLOWSHIP) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }
    var imageBase64 by remember { mutableStateOf(initial?.imageBase64) }
    var published by remember { mutableStateOf(initial?.published ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var isEncodingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isEncodingPhoto = true
            validationError = null
            scope.launch {
                val res = ImageUtils.encodeUriToBase64(context, uri)
                isEncodingPhoto = false
                res.onSuccess { b64 ->
                    imageBase64 = b64
                    coverPreset = CoverPreset.CUSTOM
                }.onFailure { err ->
                    validationError = err.localizedMessage ?: "Gagal memproses foto."
                }
            }
        }
    }

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
                BulletinCoverImage(
                    coverPreset = coverPreset,
                    imageUrl = imageUrl.takeIf { it.isNotBlank() },
                    imageBase64 = imageBase64,
                    contentDescription = "Pratinjau foto galeri",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(14.dp))
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = !isEncodingPhoto,
                        modifier = Modifier.weight(1f).testTag("pick_gallery_photo_button")
                    ) {
                        if (isEncodingPhoto) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih dari Galeri HP")
                    }
                    if (!imageBase64.isNullOrBlank()) {
                        OutlinedButton(onClick = { imageBase64 = null }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Hapus foto", modifier = Modifier.size(18.dp))
                        }
                    }
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
                    when {
                        t.length < 2 -> validationError = "Judul foto minimal 2 karakter."
                        dt.length < 2 -> validationError = "Tanggal dokumentasi minimal 2 karakter."
                        else -> onSave(
                            t, caption.trim(), category, dt, coverPreset,
                            imageUrl.trim().takeIf { it.isNotBlank() },
                            imageBase64, photographerName, published
                        )
                    }
                },
                modifier = Modifier.testTag("save_photo_button")
            ) {
                Text(if (isBusy) "Mengunggah…" else "Simpan & Sinkron")
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
