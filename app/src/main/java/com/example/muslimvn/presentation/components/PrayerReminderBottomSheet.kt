package com.example.muslimvn.presentation.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.ReminderMode
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerReminderBottomSheet(
    prayerName: String,
    currentReminder: PrayerReminder,
    onDismiss: () -> Unit,
    onSave: (PrayerReminder) -> Unit
) {
    var selectedMode by remember { mutableStateOf(currentReminder.mode) }
    var selectedAdhan by remember { mutableStateOf(currentReminder.adhanFileName ?: "Mishary-Alafasi.mp3") }

    val context = LocalContext.current
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var previewingFile by remember { mutableStateOf<String?>(null) }

    val stopPreview = {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        previewingFile = null
    }

    val playPreview = { fileName: String ->
        if (previewingFile == fileName) {
            stopPreview()
        } else {
            stopPreview()
            try {
                val afd = context.assets.openFd("audio/adhan/$fileName")
                val player = MediaPlayer().apply {
                    setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    prepare()
                    start()
                    setOnCompletionListener { 
                        previewingFile = null
                        mediaPlayer = null
                    }
                }
                mediaPlayer = player
                previewingFile = fileName
                afd.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            stopPreview()
            onDismiss()
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "${stringResource(R.string.reminder_settings_title)}: $prayerName",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Mode Selection
            ReminderModeItem(
                title = stringResource(R.string.reminder_mode_none),
                icon = Icons.Default.NotificationsOff,
                selected = selectedMode == ReminderMode.SILENT,
                onClick = { selectedMode = ReminderMode.SILENT }
            )
            ReminderModeItem(
                title = stringResource(R.string.reminder_mode_default),
                icon = Icons.Default.Notifications,
                selected = selectedMode == ReminderMode.NOTIFICATION,
                onClick = { selectedMode = ReminderMode.NOTIFICATION }
            )
            ReminderModeItem(
                title = stringResource(R.string.reminder_mode_adhan),
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                selected = selectedMode == ReminderMode.ADHAN,
                onClick = { selectedMode = ReminderMode.ADHAN }
            )

            if (selectedMode == ReminderMode.ADHAN) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.select_adhan_voice),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                AdhanList(
                    prayerName = prayerName,
                    selectedAdhan = selectedAdhan,
                    previewingAdhan = previewingFile,
                    onSelected = { 
                        selectedAdhan = it
                        playPreview(it) // Tự động phát khi chọn
                    },
                    onPreviewClick = { playPreview(it) }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    onSave(currentReminder.copy(mode = selectedMode, adhanFileName = selectedAdhan))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.save_settings))
            }
        }
    }
}

@Composable
fun ReminderModeItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AdhanList(
    prayerName: String,
    selectedAdhan: String,
    previewingAdhan: String?,
    onSelected: (String) -> Unit,
    onPreviewClick: (String) -> Unit
) {
    val isFajr = prayerName.contains("Fajr", ignoreCase = true)
    
    val adhans = remember(isFajr) {
        val list = mutableListOf(
            "Mishary-Alafasi.mp3" to "Mishary Alafasi",
            "hamad_daghriry.mp3" to "Hamad Daghriry",
            "Ahmed-El-Kourdi.mp3" to "Ahmed El Kourdi",
            "Nasser-Alqatami.mp3" to "Nasser Alqatami",
            "Mansoor-Az-Zahrani.mp3" to "Mansoor Az Zahrani",
            "Rabeh-Ibn-Darah-Al-Jazairi.mp3" to "Rabeh Ibn Darah"
        )
        if (isFajr) {
            list.add(0, "Fajaz_Azan.mp3" to "Fajr Special Adhan")
        }
        list
    }

    Column {
        adhans.forEach { (file, name) ->
            val isPreviewing = previewingAdhan == file
            val isSpecialFajr = file == "Fajaz_Azan.mp3"
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(file) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedAdhan == file,
                    onClick = { onSelected(file) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name, 
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSpecialFajr) {
                        Text(
                            text = "Bản Adhan đặc biệt cho giờ Fajr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(onClick = { onPreviewClick(file) }) {
                    Icon(
                        imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isPreviewing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
