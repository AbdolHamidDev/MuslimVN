package com.example.muslimvn.presentation.screens.youtube.components

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.muslimvn.domain.models.YoutubeVideo
import com.example.muslimvn.presentation.viewmodels.YoutubePlayerUiState
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PortraitLayout(
    uiState: YoutubePlayerUiState,
    videoTitle: String,
    isLoadingPlayer: Boolean,
    playerError: String?,
    exoPlayer: ExoPlayer,
    onToggleFullscreen: () -> Unit,
    onVideoClick: (YoutubeVideo) -> Unit,
    onDownloadClick: () -> Unit,
    currentUrl: String,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    var isControllerVisible by remember { mutableStateOf(false) }

    val onFeatureNotReady: (String) -> Unit = { featureName ->
        coroutineScope.launch {
            if (featureName == "Phát nền") {
                snackbarHostState.showSnackbar("Chế độ phát nền đã được kích hoạt. Bạn có thể thoát ứng dụng và vẫn nghe được âm thanh.")
            } else if (featureName == "Popup") {
                if (activity?.packageManager?.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE) == true) {
                    activity.enterPictureInPictureMode(
                        android.app.PictureInPictureParams.Builder().build()
                    )
                } else {
                    snackbarHostState.showSnackbar("Thiết bị không hỗ trợ chế độ Popup")
                }
            } else {
                snackbarHostState.showSnackbar("Tính năng $featureName đang được phát triển")
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Video Player Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLoadingPlayer) {
                CircularProgressIndicator(color = Color.White)
            } else if (playerError != null) {
                Text(
                    text = "Lỗi: $playerError",
                    color = Color.Red,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                            setBackgroundColor(android.graphics.Color.BLACK)
                            setControllerVisibilityListener(
                                PlayerView.ControllerVisibilityListener { visibility ->
                                    isControllerVisible = (visibility == View.VISIBLE)
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isControllerVisible,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Toàn màn hình",
                        tint = Color.White
                    )
                }
            }
        }

        // Content Area
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                VideoInfoSection(
                    detail = uiState.videoDetail,
                    fallbackTitle = uiState.videoDetail?.title ?: videoTitle.ifEmpty { "Đang tải..." }
                )
            }

            item {
                ActionButtonsRow(
                    downloadStatus = uiState.downloadStatus,
                    onDownloadClick = onDownloadClick,
                    onActionClick = onFeatureNotReady,
                    onShareClick = {
                        val videoUrl = uiState.videoDetail?.videoUrl ?: currentUrl
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, videoUrl)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            item {
                ChannelSection(
                    uploaderName = uiState.videoDetail?.uploaderName ?: uiState.channelName.ifEmpty { "..." },
                    uploaderAvatarUrl = uiState.videoDetail?.uploaderAvatarUrl ?: ""
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            item {
                Text(
                    text = "Video gợi ý",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val suggested = uiState.suggestedVideos.filter { it.videoUrl != currentUrl }
            items(suggested) { video ->
                SuggestedVideoItem(
                    video = video,
                    onClick = { onVideoClick(video) }
                )
            }
        }
    }
}
