package com.example.muslimvn.presentation.screens.scholar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.muslimvn.R

/**
 * Nút Quay lại (Back Button) nổi với nền hình tròn bán trong suốt, xử lý khoảng cách an toàn (Window Insets)
 */
@Composable
fun FloatingBackButton(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onBackClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
        modifier = modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Banner nhỏ báo lỗi fetch RSS (offline/feed lỗi) kèm nút thử lại — cache cũ vẫn dùng được. */
@Composable
fun OfflineBanner(onRetry: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.podcast_error_offline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(R.string.retry), color = Color.White)
            }
        }
    }
}

/**
 * Buộc màu chủ đạo từ ảnh luôn ở tone màu đậm/tối chuẩn YouTube Music (brightness max 0.18)
 */
fun Color.toDeepDark(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[1] = (hsv[1] * 1.15f).coerceAtMost(1f)
    hsv[2] = hsv[2].coerceIn(0.08f, 0.18f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

/**
 * Tạo màu nền cho MiniPlayer cao hơn một tông so với màu nền chính của màn hình
 */
fun Color.toMiniPlayerColor(): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(this.toArgb(), hsv)
    hsv[2] = (hsv[2] + 0.10f).coerceAtMost(0.32f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}
