package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DynamicCategoryItem
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.tr

@Composable
fun AdminCategoryManagerView(
    title: String,
    subtitle: String,
    categories: List<DynamicCategoryItem>,
    isBusy: Boolean,
    pageTestTag: String,
    onBack: () -> Unit,
    onSaveCategory: (existingCode: String?, labelMs: String, labelEn: String, onDone: () -> Unit) -> Unit,
    onDeleteCategory: (code: String, onDone: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<DynamicCategoryItem?>(null) }
    var pendingDeleteCategory by remember { mutableStateOf<DynamicCategoryItem?>(null) }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag(pageTestTag)
        ) {
            item(key = "category_mgr_header") {
                AdminSubPageHeader(
                    title = title,
                    subtitle = subtitle,
                    badgeText = tr("${categories.size} Kategori", "${categories.size} Categories"),
                    onBack = onBack
                )
            }

            item(key = "category_mgr_add_card") {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Category,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tr("Urus Senarai Kategori", "Manage Category List"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = tr(
                                        "Tambah, kemas kini, atau padam kategori. Perubahan disegerakkan secara langsung kepada semua pengguna.",
                                        "Add, edit, or delete categories. Changes sync in real time for all users."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = {
                                editingCategory = null
                                showEditorDialog = true
                            },
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("admin_add_category_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Tambah Kategori Baharu", "Add New Category"))
                        }
                    }
                }
            }

            itemsIndexed(
                items = categories,
                key = { _, item -> item.resolvedCode() }
            ) { index, category ->
                val code = category.resolvedCode()
                val primaryLabel = category.localizedLabel(lang)
                val secondaryLabel = if (category.labelMs.equals(category.labelEn, ignoreCase = true)) {
                    code
                } else {
                    "${category.labelMs} / ${category.labelEn}"
                }
                val canDelete = categories.size > 1 && !isBusy

                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_category_item_$index")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = primaryLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = secondaryLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                editingCategory = category
                                showEditorDialog = true
                            },
                            enabled = !isBusy,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("edit_category_$code")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = tr("Edit $primaryLabel", "Edit $primaryLabel"),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { pendingDeleteCategory = category },
                            enabled = canDelete,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("delete_category_$code")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = tr("Padam $primaryLabel", "Delete $primaryLabel"),
                                tint = if (canDelete) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                        }
                    }
                }
            }

            if (categories.size <= 1) {
                item(key = "category_mgr_min_hint") {
                    Text(
                        text = tr(
                            "Sekurang-kurangnya satu kategori mesti dikekalkan.",
                            "At least one category must remain."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    if (showEditorDialog) {
        AdminCategoryEditorDialog(
            initial = editingCategory,
            isBusy = isBusy,
            onDismiss = { showEditorDialog = false },
            onSave = { labelMs, labelEn ->
                onSaveCategory(editingCategory?.resolvedCode(), labelMs, labelEn) {
                    showEditorDialog = false
                }
            }
        )
    }

    val deleteTarget = pendingDeleteCategory
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { if (!isBusy) pendingDeleteCategory = null },
            title = { Text(tr("Padam Kategori?", "Delete Category?")) },
            text = {
                Text(
                    tr(
                        "Adakah anda pasti mahu memadam kategori \"${deleteTarget.localizedLabel(lang)}\"? Item sedia ada yang menggunakan kategori ini tidak akan dipadam.",
                        "Are you sure you want to delete the \"${deleteTarget.localizedLabel(lang)}\" category? Existing posts using this category will not be deleted."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = deleteTarget.resolvedCode()
                        onDeleteCategory(code) {
                            pendingDeleteCategory = null
                        }
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_category_button")
                ) {
                    Text(tr("Padam", "Delete"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteCategory = null },
                    enabled = !isBusy
                ) {
                    Text(tr("Batal", "Cancel"))
                }
            }
        )
    }
}

@Composable
private fun AdminCategoryEditorDialog(
    initial: DynamicCategoryItem?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (labelMs: String, labelEn: String) -> Unit
) {
    var labelMs by remember(initial) { mutableStateOf(initial?.labelMs.orEmpty()) }
    var labelEn by remember(initial) { mutableStateOf(initial?.labelEn.orEmpty()) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = {
            Text(
                text = if (initial == null) {
                    tr("Tambah Kategori Baharu", "Add New Category")
                } else {
                    tr("Edit Kategori", "Edit Category")
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = labelMs,
                    onValueChange = {
                        labelMs = it.take(60)
                        validationError = null
                    },
                    label = { Text(tr("Nama Kategori (Bahasa Melayu) *", "Category Name (Malay) *")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_label_ms_input")
                )
                OutlinedTextField(
                    value = labelEn,
                    onValueChange = {
                        labelEn = it.take(60)
                        validationError = null
                    },
                    label = { Text(tr("Nama Kategori (Inggeris - Pilihan)", "Category Name (English - Optional)")) },
                    placeholder = {
                        Text(tr("Guna nama BM jika dikosongkan", "Uses Malay name if left blank"))
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("category_label_en_input")
                )
                if (validationError != null) {
                    Text(
                        text = validationError.orEmpty(),
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
                    val cleanMs = labelMs.trim()
                    val cleanEn = labelEn.trim().ifBlank { cleanMs }
                    if (cleanMs.isEmpty()) {
                        validationError = AppLanguageManager.tr(
                            "Nama kategori wajib diisi.",
                            "Category name is required."
                        )
                    } else {
                        onSave(cleanMs, cleanEn)
                    }
                },
                modifier = Modifier.testTag("save_category_button")
            ) {
                Text(if (isBusy) tr("Menyimpan…", "Saving…") else tr("Simpan", "Save"))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isBusy
            ) {
                Text(tr("Batal", "Cancel"))
            }
        }
    )
}
