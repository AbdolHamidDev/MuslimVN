package com.example.muslimvn.presentation.screens.scholar.components

import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.muslimvn.ui.theme.ExtraLarge2Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.muslimvn.R
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

/**
 * Nút Thư viện Podcast (Library Button) nổi ở góc trên bên phải với nền hình tròn bán trong suốt.
 */
@Composable
fun FloatingLibraryButton(
    onLibraryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onLibraryClick,
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        contentColor = Color.White,
        modifier = modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Bookmarks,
                contentDescription = "Thư viện Podcast",
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

/**
 * Trích xuất và nhớ màu động (Dynamic Color) tối đậm từ đường dẫn ảnh asset
 */
@Composable
fun rememberDynamicBackgroundColor(imagePath: String): Color {
    val context = LocalContext.current
    var dynamicBackgroundColor by remember(imagePath) { mutableStateOf<Color?>(null) }
    val imageUri = remember(imagePath) { imagePath.toAndroidAssetUri() }

    LaunchedEffect(imageUri) {
        if (!imageUri.isNullOrEmpty()) {
            runCatching {
                val request = ImageRequest.Builder(context)
                    .data(imageUri)
                    .allowHardware(false)
                    .size(200, 200)
                    .build()

                val result = context.imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap).generate()
                        }
                        val swatch = palette.darkVibrantSwatch
                            ?: palette.darkMutedSwatch
                            ?: palette.dominantSwatch
                        swatch?.let {
                            dynamicBackgroundColor = Color(it.rgb).toDeepDark()
                        }
                    }
                }
            }
        }
    }

    val defaultBg = MaterialTheme.colorScheme.surface
    val backgroundColor by animateColorAsState(
        targetValue = dynamicBackgroundColor ?: defaultBg,
        animationSpec = tween(durationMillis = 600),
        label = "libraryDynamicBackground"
    )

    return backgroundColor
}

/**
 * Header tràn viền Edge-to-Edge dùng chung cho các màn chi tiết Thư viện (Yêu thích, Danh sách phát, Đã tải xuống, Đã theo dõi):
 * - Hero Image chiếm ~42% chiều cao màn hình.
 * - Gradient overlay chuyển tiếp mượt xuống màu nền ứng dụng.
 * - Tiêu đề, mô tả và Pill Bar chứa 3 nút thao tác (Play, Shuffle, Search).
 */
@Composable
fun LibraryDetailHeader(
    title: String,
    subtitle: String,
    imagePath: String,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    isPlaying: Boolean = false,
    onPlayAllClick: () -> Unit = {},
    onShuffleClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    extraContent: (@Composable () -> Unit)? = null
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val headerHeight = screenHeight * 0.42f
    val bgColor = backgroundColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        // 1. Hero Image tràn viền
        AsyncImage(
            model = imagePath.toAndroidAssetUri(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gradient Overlay tối dần xuống chân Header
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.75f),
                            bgColor.copy(alpha = 0.95f),
                            bgColor
                        )
                    )
                )
        )

        // 3. Nội dung thông tin & Pill Bar 3 nút
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(top = 48.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(10.dp))
                extraContent()
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pill Bar 3 Nút UI (Play, Shuffle, Search)
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.22f),
                contentColor = Color.White
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    IconButton(
                        onClick = onPlayAllClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Phát tuần tự",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = onShuffleClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Phát ngẫu nhiên",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Tìm kiếm",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Thanh tìm kiếm dùng chung cho các màn hình card Thư viện Podcast
 */
@Composable
fun PodcastSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        placeholder = {
            Text(
                text = placeholderText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.5f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Tìm kiếm",
                tint = Color.Black.copy(alpha = 0.7f)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Xóa nội dung",
                        tint = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
        },
        singleLine = true,
        shape = ExtraLarge2Shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black
        )
    )
}

