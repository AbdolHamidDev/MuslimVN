package com.example.muslimvn.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PrayerName
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.PrayerTimes
import com.example.muslimvn.domain.models.ReminderMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PrayerList(prayerTimes: PrayerTimes, reminders: Map<String, PrayerReminder>, onReminderClick: (String) -> Unit) {
    val prayers = listOf(
        PrayerItemData(stringResource(R.string.prayer_fajr), prayerTimes.fajr, Icons.Default.NightsStay, PrayerName.FAJR),
        PrayerItemData(stringResource(R.string.prayer_sunrise), prayerTimes.sunrise, Icons.Default.WbTwilight, PrayerName.SUNRISE),
        PrayerItemData(stringResource(R.string.prayer_dhuhr), prayerTimes.dhuhr, Icons.Default.WbSunny, PrayerName.DHUHR),
        PrayerItemData(stringResource(R.string.prayer_asr), prayerTimes.asr, Icons.Default.WbCloudy, PrayerName.ASR),
        PrayerItemData(stringResource(R.string.prayer_maghrib), prayerTimes.maghrib, Icons.Default.WbTwilight, PrayerName.MAGHRIB),
        PrayerItemData(stringResource(R.string.prayer_isha), prayerTimes.isha, Icons.Default.Bedtime, PrayerName.ISHA)
    )

    LazyColumn(
        modifier = Modifier.height(400.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(prayers) { prayer ->
            val reminder = reminders[prayer.id] ?: PrayerReminder(prayer.id)
            PrayerItemRow(prayer, reminder, onReminderClick)
        }
    }
}

data class PrayerItemData(
    val name: String,
    val time: Date,
    val icon: ImageVector,
    val id: String
)

@Composable
fun PrayerItemRow(prayer: PrayerItemData, reminder: PrayerReminder, onReminderClick: (String) -> Unit) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val isEnabled = reminder.mode != ReminderMode.SILENT
    val backgroundImage = getPrayerImage(prayer.id)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onReminderClick(prayer.id) },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = backgroundImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.6f
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = prayer.icon, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = prayer.name, 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timeFormat.format(prayer.time), 
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                IconButton(
                    onClick = { onReminderClick(prayer.id) },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        CircleShape
                    )
                ) {
                    Icon(
                        imageVector = when (reminder.mode) {
                            ReminderMode.SILENT -> Icons.Default.NotificationsOff
                            ReminderMode.NOTIFICATION -> Icons.Default.Notifications
                            ReminderMode.ADHAN -> Icons.AutoMirrored.Filled.VolumeUp
                        },
                        contentDescription = stringResource(R.string.toggle_adhan),
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

fun getPrayerImage(prayerId: String): String {
    val base = "file:///android_asset/images/praytime/"
    return when (prayerId) {
        PrayerName.FAJR -> "${base}fajr.webp"
        PrayerName.DHUHR -> "${base}dhuhr.jpg"
        PrayerName.ASR -> "${base}asr.jpg"
        PrayerName.MAGHRIB -> "${base}maghrib.jpg"
        PrayerName.ISHA -> "${base}isha.jpg"
        else -> "${base}vietnammosque.jpg"
    }
}
