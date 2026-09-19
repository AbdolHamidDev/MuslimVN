package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.muslimvn.R
import com.example.muslimvn.data.preferences.QuranDisplayMode
import com.example.muslimvn.data.preferences.QuranViewMode
import com.example.muslimvn.domain.models.DownloadStatus
import com.example.muslimvn.domain.models.Reciter
import com.example.muslimvn.domain.models.availableReciters
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.viewmodels.QuranSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: QuranSettingsViewModel = hiltViewModel()
) {
    val reciterIdentifier by viewModel.reciterIdentifier.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val displayMode by viewModel.displayMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val mushafProgress by viewModel.mushafDownloadProgress.collectAsState()
    val isMushafOffline by viewModel.isMushafOffline.collectAsState()
    val mushafStatus by viewModel.mushafDownloadStatus.collectAsState()
    val confirmDownloadReciter by viewModel.showDownloadConfirmDialog.collectAsState()
    val confirmClearReciter by viewModel.showClearConfirmDialog.collectAsState()
    val showMushafDownloadConfirm by viewModel.showMushafDownloadConfirm.collectAsState()
    val showMushafClearConfirm by viewModel.showMushafClearConfirm.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt Quran") },
                scrollBehavior = scrollBehavior,
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
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SettingsSectionTitle(title = "Cách trình bày", icon = Icons.AutoMirrored.Filled.MenuBook)
                ViewModeSelector(
                    currentMode = viewMode,
                    onModeSelected = viewModel::onViewModeChanged
                )
            }

            item {
                SettingsSectionTitle(title = "Dữ liệu Mushaf (Offline)", icon = Icons.Default.CloudDownload)
                MushafDownloadItem(
                    isOffline = isMushafOffline,
                    progress = mushafProgress,
                    status = mushafStatus,
                    onDownloadClick = {
                        if (mushafStatus == DownloadStatus.IDLE) {
                            viewModel.onMushafDownloadClick()
                        } else {
                            viewModel.startMushafDownload()
                        }
                    },
                    onPauseClick = viewModel::pauseMushafDownload,
                    onClearClick = viewModel::onMushafClearClick
                )
            }

            item {
                SettingsSectionTitle(title = "Chế độ hiển thị", icon = Icons.Default.Visibility)
                DisplayModeSelector(
                    currentMode = displayMode,
                    onModeSelected = viewModel::onDisplayModeChanged
                )
            }

            item {
                SettingsSectionTitle(title = "Kích thước chữ", icon = Icons.Default.FontDownload)
                FontSizeSlider(
                    currentSize = fontSize,
                    onSizeChanged = viewModel::onFontSizeChanged
                )
            }

            items(availableReciters) { reciter ->
                val downloadProgress by viewModel.getDownloadProgress(reciter.quranComId).collectAsState(initial = null)
                val downloadStatus by viewModel.getDownloadStatus(reciter.quranComId).collectAsState(initial = DownloadStatus.IDLE)
                
                ReciterItem(
                    reciter = reciter,
                    isSelected = reciter.identifier == reciterIdentifier,
                    onClick = { viewModel.onReciterSelected(reciter) },
                    onDownloadClick = { viewModel.startDownload(reciter) },
                    onPauseClick = { viewModel.pauseDownload(reciter) },
                    onClearClick = { viewModel.onClearClick(reciter) },
                    downloadProgress = downloadProgress,
                    downloadStatus = downloadStatus
                )
            }
        }
    }

    confirmDownloadReciter?.let { reciter ->
        DownloadConfirmationDialog(
            reciter = reciter,
            onConfirm = { viewModel.startFullQuranDownload(reciter) },
            onDismiss = { viewModel.dismissDownloadConfirmDialog() }
        )
    }

    confirmClearReciter?.let { reciter ->
        ClearConfirmationDialog(
            reciter = reciter,
            onConfirm = { viewModel.confirmClearAudio(reciter) },
            onDismiss = { viewModel.dismissClearConfirmDialog() }
        )
    }

    if (showMushafDownloadConfirm) {
        MushafDownloadConfirmationDialog(
            onConfirm = { viewModel.startMushafDownload() },
            onDismiss = { viewModel.dismissMushafDownloadConfirm() }
        )
    }

    if (showMushafClearConfirm) {
        MushafClearConfirmationDialog(
            onConfirm = { viewModel.clearMushafData() },
            onDismiss = { viewModel.dismissMushafClearConfirm() }
        )
    }
}

@Composable
fun MushafClearConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Xóa dữ liệu Mushaf") },
        text = {
            Text("Bạn có chắc chắn muốn xóa toàn bộ ảnh trang Quran (Mushaf) đã tải? Bạn sẽ cần kết nối mạng để đọc lại ở chế độ này.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Xóa ngay")
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
fun MushafDownloadConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
        title = { Text("Tải dữ liệu Mushaf") },
        text = {
            Column {
                Text("Bạn có muốn tải toàn bộ 604 trang Quran (bản gốc 1024px) để đọc offline không?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dung lượng ước tính: ~120 MB",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Khuyến nghị sử dụng Wi-Fi để tiết kiệm dữ liệu di động.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Tải ngay")
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
fun ClearConfirmationDialog(
    reciter: Reciter,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Xóa dữ liệu Quran") },
        text = {
            Text("Bạn có chắc chắn muốn xóa dữ liệu audio đã tải của học giả ${reciter.name}? Hành động này không thể hoàn tác.")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Xóa ngay")
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
fun DownloadConfirmationDialog(
    reciter: Reciter,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Download, contentDescription = null) },
        title = { Text("Tải audio Quran") },
        text = {
            Column {
                Text("Bạn có muốn tải toàn bộ audio (114 Surah) của học giả ${reciter.name} không?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Dung lượng ước tính: ~${reciter.estimatedSizeMb} MB",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Khuyến nghị sử dụng Wi-Fi để tránh phát sinh chi phí dữ liệu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Tải ngay")
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
private fun SettingsSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MushafDownloadItem(
    isOffline: Boolean,
    progress: Float?,
    status: DownloadStatus,
    onDownloadClick: () -> Unit,
    onPauseClick: () -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when(status) {
                        DownloadStatus.COMPLETED -> "Đã sẵn sàng offline"
                        DownloadStatus.DOWNLOADING -> "Đang tải dữ liệu..."
                        DownloadStatus.PAUSED -> "Đã tạm dừng tải"
                        else -> "Chưa tải bản gốc (1024px)"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isOffline) "Bạn có thể đọc Quran dạng Mushaf không cần mạng." 
                           else "Tải về để lật trang mượt hơn và tiết kiệm dung lượng 4G.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (progress != null && progress < 1.0f && status != DownloadStatus.IDLE) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    val progressText = "${(progress * 100).toInt()}%"
                    Text(
                        text = if (status == DownloadStatus.DOWNLOADING) "Tiến độ: $progressText" else "Đã tải: $progressText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Box(contentAlignment = Alignment.Center) {
                if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
                    CircularProgressIndicator(
                        progress = { progress ?: 0f },
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row {
                    when (status) {
                        DownloadStatus.IDLE -> {
                            Button(
                                onClick = onDownloadClick,
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Tải về")
                            }
                        }
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPauseClick) {
                                Icon(Icons.Default.Pause, contentDescription = "Tạm dừng", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onDownloadClick) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Tiếp tục", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onClearClick) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewModeSelector(
    currentMode: QuranViewMode,
    onModeSelected: (QuranViewMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        QuranViewMode.values().forEachIndexed { index, mode ->
            val label = when (mode) {
                QuranViewMode.LIST -> "Dạng danh sách"
                QuranViewMode.MUSHAF -> "Dạng Mushaf"
            }
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = QuranViewMode.values().size)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DisplayModeSelector(
    currentMode: QuranDisplayMode,
    onModeSelected: (QuranDisplayMode) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        QuranDisplayMode.values().forEachIndexed { index, mode ->
            val label = when (mode) {
                QuranDisplayMode.BOTH -> "Cả hai"
                QuranDisplayMode.ARABIC_ONLY -> "Chỉ Ả Rập"
                QuranDisplayMode.TRANSLATION_ONLY -> "Chỉ dịch"
            }
            SegmentedButton(
                selected = currentMode == mode,
                onClick = { onModeSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = QuranDisplayMode.values().size)
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FontSizeSlider(
    currentSize: Float,
    onSizeChanged: (Float) -> Unit
) {
    Column {
        Slider(
            value = currentSize,
            onValueChange = onSizeChanged,
            valueRange = 14f..32f,
            steps = 8
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Nhỏ", style = MaterialTheme.typography.bodySmall)
            Text("${currentSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Lớn", style = MaterialTheme.typography.bodySmall)
        }
        
        // Preview text
        Surface(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontSize = (currentSize * 1.4).sp,
                    lineHeight = (currentSize * 2.2).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhân danh Allah, Đấng Rất Mực Độ Lượng, Đấng Rất Mực Khoan Dung",
                    fontSize = (currentSize * 0.8).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun ReciterItem(
    reciter: Reciter,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit = {},
    onPauseClick: () -> Unit = {},
    onClearClick: () -> Unit = {},
    downloadProgress: Float? = null,
    downloadStatus: DownloadStatus = DownloadStatus.IDLE
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                val imageModel = remember(reciter.imageUrl) { reciter.imageUrl.toAndroidAssetUri() }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurfaceVariant else Color.Transparent,
                            shape = CircleShape
                        )
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reciter.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = reciter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (downloadStatus != DownloadStatus.IDLE && downloadStatus != DownloadStatus.COMPLETED) {
                    val progressText = if (downloadProgress != null) "${(downloadProgress * 100).toInt()}%" else "..."
                    Text(
                        text = if (downloadStatus == DownloadStatus.DOWNLOADING) "Đang tải: $progressText" else "Đã tạm dừng: $progressText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Nút tải xuống / điều khiển
            Box(contentAlignment = Alignment.Center) {
                if (downloadStatus == DownloadStatus.DOWNLOADING || downloadStatus == DownloadStatus.PAUSED) {
                    CircularProgressIndicator(
                        progress = { downloadProgress ?: 0f },
                        modifier = Modifier.size(40.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row {
                    when (downloadStatus) {
                        DownloadStatus.IDLE -> {
                            IconButton(onClick = onDownloadClick) {
                                Icon(Icons.Default.Download, contentDescription = "Tải xuống", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        DownloadStatus.DOWNLOADING -> {
                            IconButton(onClick = onPauseClick) {
                                Icon(Icons.Default.Pause, contentDescription = "Tạm dừng", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DownloadStatus.PAUSED -> {
                            IconButton(onClick = onDownloadClick) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Tiếp tục", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        DownloadStatus.COMPLETED -> {
                            IconButton(onClick = onClearClick) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa audio", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
