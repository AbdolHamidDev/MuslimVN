package com.example.muslimvn.presentation.screens.youtube.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.muslimvn.data.util.DownloadStatus

@Composable
fun DownloadOptionDialog(
    videoStatus: DownloadStatus,
    audioStatus: DownloadStatus,
    videoSizeBytes: Long,
    audioSizeBytes: Long,
    isLoadingSizes: Boolean,
    onDownloadVideoClick: () -> Unit,
    onDownloadAudioClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(16.dp),
        title = {
            Text(
                text = "Chọn định dạng tải xuống",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Vui lòng chọn loại tệp bạn muốn tải về thiết bị:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Option 1: Video (MP4)
                DownloadOptionItem(
                    title = "Video (MP4)",
                    subtitle = "Xem video chất lượng cao offline",
                    icon = Icons.Default.VideoLibrary,
                    sizeBytes = videoSizeBytes,
                    isLoadingSize = isLoadingSizes,
                    status = videoStatus,
                    onClick = onDownloadVideoClick
                )

                // Option 2: Audio (M4A)
                DownloadOptionItem(
                    title = "Âm thanh (M4A / Podcast)",
                    subtitle = "Nghe nhạc nền tiết kiệm dung lượng & pin",
                    icon = Icons.Default.Headset,
                    sizeBytes = audioSizeBytes,
                    isLoadingSize = isLoadingSizes,
                    status = audioStatus,
                    onClick = onDownloadAudioClick
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Đóng",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

@Composable
private fun DownloadOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    sizeBytes: Long,
    isLoadingSize: Boolean,
    status: DownloadStatus,
    onClick: () -> Unit
) {
    val isAlreadyDownloaded = status is DownloadStatus.Completed
    val isDownloading = status is DownloadStatus.Downloading
    val isEnabled = !isAlreadyDownloaded && !isDownloading

    val formattedSize = when {
        isLoadingSize -> "Đang tính dung lượng..."
        sizeBytes > 0 -> formatSizeBytes(sizeBytes)
        else -> ""
    }

    val backgroundColor = if (isAlreadyDownloaded) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val borderColor = MaterialTheme.colorScheme.outlineVariant

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(enabled = isEnabled, onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAlreadyDownloaded) Icons.Default.CheckCircle else icon,
                    contentDescription = null,
                    tint = if (isAlreadyDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (formattedSize.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dung lượng: $formattedSize",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (isDownloading) {
                    val progress = status.progress
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(com.example.muslimvn.ui.theme.BadgeShape),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            when {
                isAlreadyDownloaded -> {
                    Text(
                        text = "Đã tải",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                isDownloading -> {
                    val progress = status.progress
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Tải về",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun formatSizeBytes(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) {
        String.format(java.util.Locale("vi", "VN"), "~%.1f MB", mb)
    } else {
        val kb = bytes / 1024.0
        String.format(java.util.Locale("vi", "VN"), "~%.0f KB", kb)
    }
}
