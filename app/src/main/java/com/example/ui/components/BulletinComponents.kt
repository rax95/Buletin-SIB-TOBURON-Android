package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.CoverPreset
import com.example.data.ImageUtils
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.AppLanguageManager
import com.example.ui.i18n.tr
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat

@Composable
fun BulletinCoverImage(
    coverPreset: CoverPreset,
    imageUrl: String?,
    imageBase64: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val decodedBitmap = remember(imageBase64) {
        ImageUtils.decodeBase64ToImageBitmap(imageBase64)
    }
    val fallbackRes = when (coverPreset) {
        CoverPreset.SANCTUARY -> R.drawable.img_hero_sanctuary
        CoverPreset.WORSHIP -> R.drawable.img_news_worship
        CoverPreset.FELLOWSHIP -> R.drawable.img_gallery_fellowship
        CoverPreset.CUSTOM -> R.drawable.img_hero_sanctuary
    }

    when {
        decodedBitmap != null -> {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
        !imageUrl.isNullOrBlank() -> {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                placeholder = painterResource(fallbackRes),
                error = painterResource(fallbackRes),
                contentScale = contentScale,
                modifier = modifier
            )
        }
        else -> {
            Image(
                painter = painterResource(id = fallbackRes),
                contentDescription = contentDescription,
                contentScale = contentScale,
                modifier = modifier
            )
        }
    }
}

@Composable
fun EditorialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTagName: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = tr("Padam carian", "Clear search")
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTagName)
    )
}

@Composable
fun <T> CategoryFilterRow(
    items: List<T>,
    selected: T?,
    labelProvider: (T) -> String,
    onSelect: (T?) -> Unit,
    modifier: Modifier = Modifier,
    allLabel: String = tr("Semua", "All")
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        item(key = "all_chip") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(allLabel, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
        items(items, key = { labelProvider(it) }) { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(if (selected == category) null else category) },
                label = { Text(labelProvider(category), style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    }
}

@Composable
fun EmptyBulletinState(
    icon: ImageVector,
    title: String,
    description: String,
    isAdmin: Boolean,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isAdmin && primaryActionLabel != null && onPrimaryAction != null) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = onPrimaryAction) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

@Composable
fun ErrorStateCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = tr("Masalah Penyegerakan", "Sync Issue"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun formatIndonesianTimestamp(
    timestamp: Timestamp?,
    lang: AppLanguage = AppLanguageManager.current()
): String {
    val date = (timestamp ?: Timestamp.now()).toDate()
    val datePart = SimpleDateFormat("dd MMM yyyy", lang.locale).format(date)
    val timePart = SimpleDateFormat("h:mm a", java.util.Locale.US).format(date)
    return "$datePart • $timePart"
}

fun formatTimeAmPm(hour24: Int, minute: Int): String {
    val safeHour = hour24.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val period = if (safeHour < 12) "AM" else "PM"
    val hour12 = when {
        safeHour == 0 -> 12
        safeHour > 12 -> safeHour - 12
        else -> safeHour
    }
    return String.format(java.util.Locale.US, "%d:%02d %s", hour12, safeMinute, period)
}

fun formatScheduleTimeDisplay(rawTimeLabel: String): String {
    val trimmed = rawTimeLabel.trim()
    if (trimmed.isEmpty()) return trimmed
    val timeRegex = Regex("(\\d{1,2})[:.](\\d{2})(?:\\s*(AM|PM))?", RegexOption.IGNORE_CASE)
    val matches = timeRegex.findAll(trimmed).toList()
    if (matches.isEmpty()) {
        return trimmed
            .replace(Regex("\\s*(MYT|WITA|WIB)\\b", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun matchToAmPm(m: MatchResult): String {
        val rawHour = m.groupValues[1].toIntOrNull() ?: 8
        val minute = (m.groupValues[2].toIntOrNull() ?: 0).coerceIn(0, 59)
        val meridiem = m.groupValues[3].uppercase(java.util.Locale.US)
        val hour24 = when (meridiem) {
            "AM" -> if (rawHour == 12) 0 else rawHour.coerceIn(0, 11)
            "PM" -> if (rawHour == 12) 12 else (rawHour.coerceIn(1, 11) + 12)
            else -> rawHour.coerceIn(0, 23)
        }
        return formatTimeAmPm(hour24, minute)
    }

    return if (matches.size >= 2) {
        "${matchToAmPm(matches[0])} - ${matchToAmPm(matches[1])}"
    } else {
        matchToAmPm(matches[0])
    }
}

