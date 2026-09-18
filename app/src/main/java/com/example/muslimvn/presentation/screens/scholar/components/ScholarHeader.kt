package com.example.muslimvn.presentation.screens.scholar.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.presentation.components.toAndroidAssetUri

/**
 * Header học giả tràn viền (Edge-to-Edge & Immersive):
 * - Chiếm 60% chiều cao màn hình.
 * - Ảnh học giả chính tràn viền phần trên cùng.
 * - Gradient overlay đồng bộ hòa quyện mượt mà vào màu động (Dynamic Color) từ Palette ảnh.
 * - Hàng 3 nút thao tác: 1. Nghe ngẫu nhiên (tròn), 2. Nghe (trắng chữ đen ở giữa), 3. Download (tròn).
 */
@Composable
fun ScholarHeader(
    scholar: Scholar?,
    backgroundColor: Color,
    onShuffleClick: () -> Unit = {},
    onPlayAllClick: () -> Unit = {},
    onDownloadAllClick: () -> Unit = {}
) {
    if (scholar == null) return
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val headerHeight = screenHeight * 0.6f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        // 1. Ảnh học giả tràn viền chính chiếm 60% chiều cao màn hình
        AsyncImage(
            model = scholar.avatarPath.toAndroidAssetUri(),
            contentDescription = scholar.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Lớp phủ Gradient Overlay chuyển màu tối dần hòa vào màu nền dynamic đậm của ảnh
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.15f),
                            Color.Black.copy(alpha = 0.75f),
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor
                        )
                    )
                )
        )

        // 3. Nội dung thông tin học giả & 3 nút thao tác đè trực tiếp lên gradient
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 52.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Tên học giả (chữ trắng)
            Text(
                text = scholar.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Chức danh / Title (chữ trắng)
            if (scholar.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = scholar.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Bio / Mô tả ngắn (chữ trắng)
            if (scholar.bio.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = scholar.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hàng 3 nút thao tác: 1. Nghe ngẫu nhiên | 2. Nút "Nghe" màu trắng chữ đen | 3. Download
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Nút 1: Nghe ngẫu nhiên (tròn, không text)
                Surface(
                    onClick = onShuffleClick,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.22f),
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Nghe ngẫu nhiên",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Nút 2: Nút "Nghe" chính ở giữa - NỀN TRẮNG CHỮ ĐEN chuẩn YouTube Music
                Button(
                    onClick = onPlayAllClick,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nghe",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                // Nút 3: Download (tròn, không text)
                Surface(
                    onClick = onDownloadAllClick,
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.22f),
                    contentColor = Color.White,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Tải xuống",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
