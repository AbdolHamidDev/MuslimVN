package com.example.muslimvn.presentation.components

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.example.muslimvn.data.util.YouTubePlayerManager

@OptIn(UnstableApi::class)
@Composable
fun YouTubeMiniPlayer(
    playerManager: YouTubePlayerManager,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUrl by playerManager.currentVideoUrl.collectAsState()
    val isMinimized by playerManager.isMinimized.collectAsState()
    val isFullscreen by playerManager.isFullscreen.collectAsState()
    val exoPlayer = playerManager.exoPlayer

    if (currentUrl != null && isMinimized && !isFullscreen) {
        Surface(
            modifier = modifier
                .width(180.dp)
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .bouncyClick(onClick = onExpand),
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16/9f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        view.player = exoPlayer
                    }
                )

                // Close button overlay at top right of card
                IconButton(
                    onClick = { playerManager.stop() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
