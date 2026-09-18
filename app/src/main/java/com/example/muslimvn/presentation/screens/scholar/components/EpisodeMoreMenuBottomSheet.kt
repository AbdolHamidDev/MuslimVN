package com.example.muslimvn.presentation.screens.scholar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.toAndroidAssetUri

/**
 * BottomSheet menu tùy chọn cho từng tập podcast:
 * 1. Header: Ảnh bìa tập podcast + Tiêu đề + Tên học giả
 * 2. Thêm vào yêu thích
 * 3. Tải xuống
 * 4. Thêm vào danh sách phát
 * 5. Đánh dấu đã nghe
 * 6. Chia sẻ
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeMoreMenuBottomSheet(
    episode: PodcastEpisode,
    scholar: Scholar?,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header: Ảnh bìa + Tiêu đề + Tên học giả
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                val artworkUri = (episode.artworkUrl ?: scholar?.avatarPath ?: "").toAndroidAssetUri()
                AsyncImage(
                    model = artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!scholar?.name.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = scholar.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.5.dp,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 1. Thêm vào yêu thích
            BottomSheetMenuItem(
                icon = Icons.Default.FavoriteBorder,
                label = "Thêm vào yêu thích",
                onClick = onDismiss
            )

            // 2. Tải xuống
            BottomSheetMenuItem(
                icon = Icons.Default.Download,
                label = "Tải xuống",
                onClick = onDismiss
            )

            // 3. Thêm vào danh sách phát
            BottomSheetMenuItem(
                icon = Icons.AutoMirrored.Filled.PlaylistAdd,
                label = "Thêm vào danh sách phát",
                onClick = onDismiss
            )

            // 4. Đánh dấu đã nghe
            BottomSheetMenuItem(
                icon = Icons.Default.CheckCircleOutline,
                label = "Đánh dấu đã nghe",
                onClick = onDismiss
            )

            // 5. Chia sẻ
            BottomSheetMenuItem(
                icon = Icons.Default.Share,
                label = "Chia sẻ",
                onClick = onDismiss
            )
        }
    }
}

/** Hàng tùy chọn trong BottomSheet */
@Composable
private fun BottomSheetMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}
