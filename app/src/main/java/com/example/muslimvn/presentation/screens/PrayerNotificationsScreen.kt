package com.example.muslimvn.presentation.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.AsrMethod
import com.example.muslimvn.domain.models.PrayerAdjustments
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
    onNavigateToCalculationDetails: () -> Unit = {},
    viewModel: PrayerNotificationsViewModel = hiltViewModel()
) {
    val reminders by viewModel.reminders.collectAsState()
    val calculationMethod by viewModel.calculationMethod.collectAsState()
    val asrMethod by viewModel.asrMethod.collectAsState()
    val prayerAdjustments by viewModel.prayerAdjustments.collectAsState()
    val isSystemNotificationEnabled by viewModel.isSystemNotificationEnabled.collectAsState()
    val context = LocalContext.current
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncNotificationState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedPrayerForModeDialog by remember { mutableStateOf<String?>(null) }
    var selectedPrayerForAudioDialog by remember { mutableStateOf<String?>(null) }
    var showCalculationMethodDialog by remember { mutableStateOf(false) }
    var showAsrMethodDialog by remember { mutableStateOf(false) }
    var showAdjustmentsDialog by remember { mutableStateOf(false) }

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
            if (!isSystemNotificationEnabled) {
                item {
                    SystemNotificationWarningBanner(
                        onOpenSettings = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                }
            }

            item {
                PreferenceHeader(title = "Cách tính giờ")
                PreferenceItem(
                    title = "Phương pháp tính",
                    subtitle = formatMethodName(calculationMethod),
                    icon = Icons.Default.Settings,
                    trailingContent = {
                        IconButton(onClick = onNavigateToCalculationDetails) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "Chi tiết cách tính",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    onClick = { showCalculationMethodDialog = true }
                )
                PreferenceItem(
                    title = "Chi tiết cách tính giờ",
                    subtitle = "Giải thích phương pháp, mốc thời gian và công thức Nửa đêm Islam",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToCalculationDetails
                )
                PreferenceItem(
                    title = "Phương pháp tính Asr (Madhhab)",
                    subtitle = if (asrMethod == AsrMethod.STANDARD) "Tiêu chuẩn / Shafi'i (Bóng = 1)" else "Hanafi (Bóng = 2)",
                    icon = Icons.Default.Schedule,
                    onClick = { showAsrMethodDialog = true }
                )
                PreferenceItem(
                    title = "Điều chỉnh phút thủ công",
                    subtitle = formatAdjustmentsSummary(prayerAdjustments),
                    icon = Icons.Default.AccessTime,
                    onClick = { showAdjustmentsDialog = true }
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

    if (showAsrMethodDialog) {
        AsrMethodDialog(
            currentMethod = asrMethod,
            onMethodSelected = {
                viewModel.onAsrMethodChanged(it)
                showAsrMethodDialog = false
            },
            onDismiss = { showAsrMethodDialog = false }
        )
    }

    if (showAdjustmentsDialog) {
        PrayerAdjustmentsDialog(
            currentAdjustments = prayerAdjustments,
            onSave = {
                viewModel.onPrayerAdjustmentsChanged(it)
                showAdjustmentsDialog = false
            },
            onDismiss = { showAdjustmentsDialog = false }
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
        "MUSLIMVN_DEFAULT" to "MuslimVN mặc định (18° / 18°)",
        "MUSLIM_WORLD_LEAGUE" to "Liên đoàn Thế giới Hồi giáo (MWL)",
        "EGYPTIAN" to "Ai Cập (Egyptian General Authority)",
        "KARACHI" to "Karachi (Univ. of Islamic Sciences)",
        "UMM_AL_QURA" to "Umm al-Qura (Makkah)",
        "DUBAI" to "Dubai",
        "MOON_SIGHTING_COMMITTEE" to "Ủy ban Quan sát Trăng",
        "NORTH_AMERICA" to "Bắc Mỹ (ISNA)",
        "KUWAIT" to "Kuwait",
        "QATAR" to "Qatar",
        "SINGAPORE" to "Singapore (MUIS)",
        "TURKEY" to "Thổ Nhĩ Kỳ (Diyanet)"
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

@Composable
private fun AsrMethodDialog(
    currentMethod: AsrMethod,
    onMethodSelected: (AsrMethod) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Phương pháp tính Asr") },
        text = {
            Column {
                ReminderOption(
                    title = "Tiêu chuẩn (Shafi'i, Maliki, Hanbali - Bóng = 1)",
                    selected = currentMethod == AsrMethod.STANDARD,
                    onClick = { onMethodSelected(AsrMethod.STANDARD) }
                )
                ReminderOption(
                    title = "Hanafi (Bóng = 2)",
                    selected = currentMethod == AsrMethod.HANAFI,
                    onClick = { onMethodSelected(AsrMethod.HANAFI) }
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
private fun PrayerAdjustmentsDialog(
    currentAdjustments: PrayerAdjustments,
    onSave: (PrayerAdjustments) -> Unit,
    onDismiss: () -> Unit
) {
    var fajr by remember { mutableStateOf(currentAdjustments.fajr) }
    var dhuhr by remember { mutableStateOf(currentAdjustments.dhuhr) }
    var asr by remember { mutableStateOf(currentAdjustments.asr) }
    var maghrib by remember { mutableStateOf(currentAdjustments.maghrib) }
    var isha by remember { mutableStateOf(currentAdjustments.isha) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Điều chỉnh phút thủ công") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AdjustmentRow(name = "Fajr", value = fajr, onChange = { fajr = it })
                AdjustmentRow(name = "Dhuhr", value = dhuhr, onChange = { dhuhr = it })
                AdjustmentRow(name = "Asr", value = asr, onChange = { asr = it })
                AdjustmentRow(name = "Maghrib", value = maghrib, onChange = { maghrib = it })
                AdjustmentRow(name = "Isha", value = isha, onChange = { isha = it })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(PrayerAdjustments(fajr, dhuhr, asr, maghrib, isha))
            }) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

@Composable
private fun AdjustmentRow(name: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(value - 1) }) {
                Text("-", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = if (value >= 0) "+$value phút" else "$value phút",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(onClick = { onChange(value + 1) }) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

private fun formatMethodName(id: String): String {
    return when (id) {
        "MUSLIMVN_DEFAULT" -> "MuslimVN mặc định (18° / 18°)"
        "MUSLIM_WORLD_LEAGUE" -> "Liên đoàn Thế giới Hồi giáo (MWL)"
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

private fun formatAdjustmentsSummary(adjustments: PrayerAdjustments): String {
    val items = mutableListOf<String>()
    if (adjustments.fajr != 0) items.add("Fajr: ${adjustments.fajr}m")
    if (adjustments.dhuhr != 0) items.add("Dhuhr: ${adjustments.dhuhr}m")
    if (adjustments.asr != 0) items.add("Asr: ${adjustments.asr}m")
    if (adjustments.maghrib != 0) items.add("Maghrib: ${adjustments.maghrib}m")
    if (adjustments.isha != 0) items.add("Isha: ${adjustments.isha}m")
    return if (items.isEmpty()) "Mặc định (0 phút)" else items.joinToString(", ")
}

@Composable
fun SystemNotificationWarningBanner(onOpenSettings: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Thông báo hệ thống đang tắt",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Bạn cần bật thông báo trong cài đặt Android để nhận được lời nhắc Adhan đúng giờ.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Mở Cài đặt hệ thống")
            }
        }
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
    val sortedAdhans = if (isFajr) {
        availableAdhans.sortedByDescending { it.isFajrSpecific }
    } else {
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
