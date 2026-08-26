package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PrayerName
import com.example.muslimvn.domain.models.PrayerReminder
import com.example.muslimvn.domain.models.ReminderMode
import com.example.muslimvn.domain.models.availableAdhans
import com.example.muslimvn.presentation.components.PreferenceHeader
import com.example.muslimvn.presentation.components.PreferenceItem
import com.example.muslimvn.presentation.viewmodels.PrayerNotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerNotificationsScreen(
    onBackClick: () -> Unit,
    viewModel: PrayerNotificationsViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsState()
    val calculationMethod by viewModel.calculationMethod.collectAsState()
    var selectedPrayerForModeDialog by remember { mutableStateOf<String?>(null) }
    var selectedPrayerForAudioDialog by remember { mutableStateOf<String?>(null) }
    var showCalculationMethodDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thông báo cầu nguyện") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                PreferenceHeader(title = "Cách tính giờ")
                PreferenceItem(
                    title = "Phương pháp tính",
                    subtitle = formatMethodName(calculationMethod),
                    icon = Icons.Default.Settings,
                    onClick = { showCalculationMethodDialog = true }
                )
            }

            item {
                PreferenceHeader(title = "Lời nhắc")
                Text(
                    text = "Tùy chỉnh lời nhắc cho từng giờ cầu nguyện",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            val prayerOrder = listOf(
                PrayerName.FAJR,
                PrayerName.SUNRISE,
                PrayerName.DHUHR,
                PrayerName.ASR,
                PrayerName.MAGHRIB,
                PrayerName.ISHA
            )

            items(prayerOrder) { prayerType ->
                val reminder = reminders[prayerType] ?: PrayerReminder(prayerType)
                PrayerReminderItem(
                    prayerType = prayerType,
                    reminder = reminder,
                    onModeClick = { selectedPrayerForModeDialog = prayerType },
                    onAudioClick = { selectedPrayerForAudioDialog = prayerType }
                )
            }
        }
    }

    selectedPrayerForModeDialog?.let { prayerType ->
        val currentMode = reminders[prayerType]?.mode ?: ReminderMode.NOTIFICATION
        ReminderModeDialog(
            prayerName = prayerNameLocale(prayerType),
            currentMode = currentMode,
            onModeSelected = { mode ->
                viewModel.onReminderModeChanged(prayerType, mode)
                selectedPrayerForModeDialog = null
            },
            onDismiss = { selectedPrayerForModeDialog = null }
        )
    }

    selectedPrayerForAudioDialog?.let { prayerType ->
        val currentAudioFile = reminders[prayerType]?.adhanFileName ?: "Mishary-Alafasi.mp3"
        AdhanAudioSelectionDialog(
            prayerName = prayerNameLocale(prayerType),
            isFajr = prayerType == PrayerName.FAJR,
            currentAudioFile = currentAudioFile,
            onAudioSelected = { fileName ->
                viewModel.onAdhanFileChanged(prayerType, fileName)
                selectedPrayerForAudioDialog = null
            },
            onDismiss = { selectedPrayerForAudioDialog = null }
        )
    }

    if (showCalculationMethodDialog) {
        CalculationMethodDialog(
            currentMethod = calculationMethod,
            onMethodSelected = {
                viewModel.onCalculationMethodChanged(it)
                showCalculationMethodDialog = false
            },
            onDismiss = { showCalculationMethodDialog = false }
        )
    }
}

@Composable
private fun CalculationMethodDialog(
    currentMethod: String,
    onMethodSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val methods = listOf(
        "MUSLIM_WORLD_LEAGUE" to "Liên đoàn Thế giới Hồi giáo",
        "EGYPTIAN" to "Ai Cập",
        "KARACHI" to "Karachi",
        "UMM_AL_QURA" to "Umm al-Qura",
        "DUBAI" to "Dubai",
        "MOON_SIGHTING_COMMITTEE" to "Ủy ban Quan sát Trăng",
        "NORTH_AMERICA" to "Bắc Mỹ (ISNA)",
        "KUWAIT" to "Kuwait",
        "QATAR" to "Qatar",
        "SINGAPORE" to "Singapore",
        "TURKEY" to "Thổ Nhĩ Kỳ"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn phương pháp tính") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(methods) { (id, name) ->
                    ReminderOption(
                        title = name,
                        selected = currentMethod == id,
                        onClick = { onMethodSelected(id) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

private fun formatMethodName(id: String): String {
    return when (id) {
        "MUSLIM_WORLD_LEAGUE" -> "Liên đoàn Thế giới Hồi giáo"
        "EGYPTIAN" -> "Ai Cập"
        "KARACHI" -> "Karachi"
        "UMM_AL_QURA" -> "Umm al-Qura"
        "DUBAI" -> "Dubai"
        "MOON_SIGHTING_COMMITTEE" -> "Ủy ban Quan sát Trăng"
        "NORTH_AMERICA" -> "Bắc Mỹ (ISNA)"
        "KUWAIT" -> "Kuwait"
        "QATAR" -> "Qatar"
        "SINGAPORE" -> "Singapore"
        "TURKEY" -> "Thổ Nhĩ Kỳ"
        else -> id
    }
}

@Composable
fun PrayerReminderItem(
    prayerType: String,
    reminder: PrayerReminder,
    onModeClick: () -> Unit,
    onAudioClick: () -> Unit
) {
    val mode = reminder.mode
    val icon = when (mode) {
        ReminderMode.SILENT -> Icons.Default.NotificationsOff
        ReminderMode.NOTIFICATION -> Icons.Default.Notifications
        ReminderMode.ADHAN -> Icons.AutoMirrored.Filled.VolumeUp
    }

    val modeText = when (mode) {
        ReminderMode.SILENT -> "Tắt thông báo"
        ReminderMode.NOTIFICATION -> "Thông báo (Mặc định)"
        ReminderMode.ADHAN -> {
            val adhanName = availableAdhans.find { it.fileName == reminder.adhanFileName }?.name ?: "Mặc định"
            "Adhan: $adhanName"
        }
    }

    Column {
        ListItem(
            headlineContent = { Text(prayerNameLocale(prayerType), fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(modeText) },
            leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable(onClick = onModeClick)
        )
        if (mode == ReminderMode.ADHAN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 72.dp, end = 16.dp, bottom = 8.dp)
                    .clickable(onClick = onAudioClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Thay đổi âm thanh Adhan",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
private fun AdhanAudioSelectionDialog(
    prayerName: String,
    isFajr: Boolean,
    currentAudioFile: String,
    onAudioSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Nếu là Fajr, ưu tiên hiện các Adhan có isFajrSpecific = true lên đầu
    val sortedAdhans = if (isFajr) {
        availableAdhans.sortedByDescending { it.isFajrSpecific }
    } else {
        // Nếu không phải Fajr, có thể ẩn các Adhan chỉ dành cho Fajr để tránh nhầm lẫn
        availableAdhans.filter { !it.isFajrSpecific }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Âm thanh Adhan cho $prayerName") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(sortedAdhans) { audio ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAudioSelected(audio.fileName) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentAudioFile == audio.fileName, onClick = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = audio.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (audio.isFajrSpecific) FontWeight.Bold else FontWeight.Normal
                            )
                            if (audio.isFajrSpecific) {
                                Text(
                                    text = "Bản Adhan đặc biệt có câu 'Cầu nguyện tốt hơn giấc ngủ'",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

private fun prayerNameLocale(name: String): String {
    return when (name) {
        PrayerName.FAJR -> "Fajr (Bình minh)"
        PrayerName.SUNRISE -> "Sunrise (Mặt trời mọc)"
        PrayerName.DHUHR -> "Dhuhr (Trưa)"
        PrayerName.ASR -> "Asr (Chiều)"
        PrayerName.MAGHRIB -> "Maghrib (Hoàng hôn)"
        PrayerName.ISHA -> "Isha (Tối)"
        else -> name
    }
}

@Composable
fun ReminderModeDialog(
    prayerName: String,
    currentMode: ReminderMode,
    onModeSelected: (ReminderMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lời nhắc cho $prayerName") },
        text = {
            Column {
                ReminderOption(
                    title = "Tắt",
                    selected = currentMode == ReminderMode.SILENT,
                    onClick = { onModeSelected(ReminderMode.SILENT) }
                )
                ReminderOption(
                    title = "Thông báo",
                    selected = currentMode == ReminderMode.NOTIFICATION,
                    onClick = { onModeSelected(ReminderMode.NOTIFICATION) }
                )
                ReminderOption(
                    title = "Adhan",
                    selected = currentMode == ReminderMode.ADHAN,
                    onClick = { onModeSelected(ReminderMode.ADHAN) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
fun ReminderOption(
    title: String,
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
        RadioButton(selected = selected, onClick = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
