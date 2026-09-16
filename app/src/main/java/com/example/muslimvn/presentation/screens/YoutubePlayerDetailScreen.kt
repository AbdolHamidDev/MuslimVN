package com.example.muslimvn.presentation.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.example.muslimvn.presentation.screens.youtube.components.DownloadOptionDialog
import com.example.muslimvn.presentation.screens.youtube.components.FullscreenPlayer
import com.example.muslimvn.presentation.screens.youtube.components.PortraitLayout
import com.example.muslimvn.presentation.viewmodels.YoutubePlayerViewModel

@OptIn(UnstableApi::class)
@ExperimentalMaterial3Api
@Composable
fun YoutubePlayerDetailScreen(
    onBackClick: () -> Unit,
    viewModel: YoutubePlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerManager = viewModel.playerManager
    val currentUrl by playerManager.currentVideoUrl.collectAsState()
    val title by playerManager.videoTitle.collectAsState()
    val isFullscreen by playerManager.isFullscreen.collectAsState()
    val isLoadingPlayer by playerManager.isLoading.collectAsState()
    val playerError by playerManager.error.collectAsState()
    val videoAspectRatio by playerManager.videoAspectRatio.collectAsState()
    val isPortraitVideo by playerManager.isPortraitVideo.collectAsState()

    val context = LocalContext.current
    val activity = context as? Activity
    val exoPlayer = playerManager.exoPlayer

    // Video dọc dùng toàn màn hình theo chiều dọc; video ngang mới xoay ngang.
    LaunchedEffect(isFullscreen, isPortraitVideo) {
        if (isFullscreen && !isPortraitVideo) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val handleBack: () -> Unit = {
        if (isFullscreen) {
            playerManager.setFullscreen(false)
        } else {
            playerManager.stop()
            onBackClick()
        }
    }

    BackHandler {
        handleBack()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {}
                Lifecycle.Event.ON_RESUME -> {}
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else padding)
        ) {
            if (isFullscreen) {
                FullscreenPlayer(
                    isLoading = isLoadingPlayer,
                    exoPlayer = exoPlayer,
                    onToggleFullscreen = { playerManager.toggleFullscreen() }
                )
            } else {
                PortraitLayout(
                    uiState = uiState,
                    videoTitle = title,
                    isLoadingPlayer = isLoadingPlayer,
                    playerError = playerError,
                    exoPlayer = exoPlayer,
                    videoAspectRatio = videoAspectRatio,
                    isPortraitVideo = isPortraitVideo,
                    onToggleFullscreen = { playerManager.toggleFullscreen() },
                    onVideoClick = { video ->
                        viewModel.selectVideo(video.videoUrl, video.title)
                    },
                    onDownloadClick = { viewModel.onDownloadClick() },
                    currentUrl = currentUrl ?: "",
                    snackbarHostState = snackbarHostState
                )
            }

            if (uiState.showDownloadDialog) {
                DownloadOptionDialog(
                    videoStatus = uiState.videoDownloadStatus,
                    audioStatus = uiState.audioDownloadStatus,
                    videoSizeBytes = uiState.videoSizeBytes,
                    audioSizeBytes = uiState.audioSizeBytes,
                    isLoadingSizes = uiState.isLoadingStreamInfo,
                    onDownloadVideoClick = { viewModel.startDownloadVideo() },
                    onDownloadAudioClick = { viewModel.startDownloadAudio() },
                    onDismiss = { viewModel.dismissDownloadDialog() }
                )
            }
        }
    }
}
