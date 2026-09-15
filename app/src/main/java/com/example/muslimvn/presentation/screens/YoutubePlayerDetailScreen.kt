package com.example.muslimvn.presentation.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.example.muslimvn.presentation.screens.youtube.components.DownloadOptionDialog
import com.example.muslimvn.presentation.screens.youtube.components.FullscreenPlayer
import com.example.muslimvn.presentation.screens.youtube.components.PortraitLayout
import com.example.muslimvn.presentation.viewmodels.YoutubePlayerViewModel
import kotlinx.coroutines.launch

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

    val context = LocalContext.current
    val activity = context as? Activity
    val exoPlayer = playerManager.exoPlayer

    // Handle orientation & fullscreen
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    val handleMinimize: () -> Unit = {
        if (isFullscreen) {
            playerManager.setFullscreen(false)
        } else {
            playerManager.minimize()
            onBackClick()
        }
    }

    BackHandler {
        handleMinimize()
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

    var dragOffset by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else padding)
                .offset(y = dragOffset.dp)
                .graphicsLayer {
                    val progress = (dragOffset / 400f).coerceIn(0f, 1f)
                    alpha = 1f - (progress * 0.4f)
                    scaleX = 1f - (progress * 0.08f)
                    scaleY = 1f - (progress * 0.08f)
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            if (!isFullscreen) {
                                dragOffset = (dragOffset + dragAmount).coerceAtLeast(0f)
                            }
                        },
                        onDragEnd = {
                            if (!isFullscreen) {
                                if (dragOffset > 140f) {
                                    handleMinimize()
                                } else {
                                    coroutineScope.launch {
                                        val anim = Animatable(dragOffset)
                                        anim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessLow
                                            )
                                        ) {
                                            dragOffset = value
                                        }
                                    }
                                }
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            if (!isFullscreen) {
                                coroutineScope.launch {
                                    val anim = Animatable(dragOffset)
                                    anim.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(stiffness = Spring.StiffnessLow)
                                    ) {
                                        dragOffset = value
                                    }
                                }
                            }
                            dragOffset = 0f
                        }
                    )
                }
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
