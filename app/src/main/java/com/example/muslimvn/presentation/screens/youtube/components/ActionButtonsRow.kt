package com.example.muslimvn.presentation.screens.youtube.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.muslimvn.ui.theme.extendedColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.muslimvn.data.util.DownloadStatus

@Composable
fun ActionButtonsRow(
    downloadStatus: DownloadStatus,
    onDownloadClick: () -> Unit,
    onActionClick: (String) -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ActionButton(
            icon = Icons.Default.Headset,
            label = "Phát nền",
            onClick = { onActionClick("Phát nền") }
        )
        ActionButton(
            icon = Icons.Default.PictureInPictureAlt,
            label = "Popup",
            onClick = { onActionClick("Popup") }
        )
        DownloadActionButton(
            status = downloadStatus,
            onClick = onDownloadClick
        )
        ActionButton(
            icon = Icons.Default.Share,
            label = "Chia sẻ",
            onClick = onShareClick
        )
    }
}

@Composable
fun DownloadActionButton(
    status: DownloadStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, label, tint) = when (status) {
        is DownloadStatus.Idle -> Triple(Icons.Default.Download, "Tải về", MaterialTheme.colorScheme.onSurface)
        is DownloadStatus.Downloading -> Triple(Icons.Default.Pause, "${(status.progress * 100).toInt()}%", MaterialTheme.colorScheme.primary)
        is DownloadStatus.Paused -> Triple(Icons.Default.PlayArrow, "Tiếp tục", MaterialTheme.colorScheme.secondary)
        is DownloadStatus.Completed -> Triple(Icons.Default.CheckCircle, "Đã tải", MaterialTheme.extendedColors.success)
        is DownloadStatus.Failed -> Triple(Icons.Default.Error, "Thử lại", MaterialTheme.colorScheme.error)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(24.dp)) {
            if (status is DownloadStatus.Downloading) {
                CircularProgressIndicator(
                    progress = { status.progress },
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
