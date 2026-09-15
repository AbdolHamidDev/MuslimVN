package com.example.muslimvn.presentation.screens.youtube.components

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
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
    videoAspectRatio: Float,
    isPortraitVideo: Boolean,
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
    val listState = rememberLazyListState()

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        // Video ngang vẫn dùng đúng aspect ratio stream; video dọc thu về 16:9.
        val collapsedAspectRatio = if (isPortraitVideo) 16f / 9f else videoAspectRatio.coerceIn(1f, 4f)
        val collapsedHeightPx = with(density) { (maxWidth / collapsedAspectRatio).toPx() }
        val expandedHeightPx = maxOf(
            collapsedHeightPx,
            with(density) { maxHeight.toPx() } * PORTRAIT_EXPANDED_HEIGHT_FRACTION
        )
        var playerHeightPx by remember(
            isPortraitVideo,
            expandedHeightPx,
            collapsedHeightPx
        ) {
            mutableFloatStateOf(if (isPortraitVideo) expandedHeightPx else collapsedHeightPx)
        }

        val playerScrollConnection = remember(
            isPortraitVideo,
            expandedHeightPx,
            collapsedHeightPx,
            listState
        ) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (!isPortraitVideo) return Offset.Zero

                    val deltaY = available.y
                    val consumedY = when {
                        // Cuộn lên: thu player trước rồi mới cuộn nội dung.
                        deltaY < 0f && playerHeightPx > collapsedHeightPx ->
                            maxOf(deltaY, collapsedHeightPx - playerHeightPx)
                        // Chỉ giãn player khi danh sách đã quay lại vị trí đầu.
                        deltaY > 0f &&
                            listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0 &&
                            playerHeightPx < expandedHeightPx ->
                            minOf(deltaY, expandedHeightPx - playerHeightPx)
                        else -> 0f
                    }
                    if (consumedY != 0f) {
                        playerHeightPx = (playerHeightPx + consumedY)
                            .coerceIn(collapsedHeightPx, expandedHeightPx)
                    }
                    return Offset(0f, consumedY)
                }
            }
        }

        val playerHeight = with(density) { playerHeightPx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(playerScrollConnection)
        ) {
        // Video Player Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(playerHeight)
                .zIndex(1f)
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
                            resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
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
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            state = listState,
            contentPadding = PaddingValues(top = playerHeight, bottom = 16.dp)
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
}

private const val PORTRAIT_EXPANDED_HEIGHT_FRACTION = 0.70f
