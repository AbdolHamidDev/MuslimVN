package com.example.muslimvn.presentation.screens

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Size
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PodcastEpisode
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.formatDurationMs
import com.example.muslimvn.presentation.components.formatSpeedLabel
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import com.example.muslimvn.presentation.viewmodels.PodcastPlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Trình phát podcast hiện đại kiểu YouTube Music:
 * - Nền mờ nghệ thuật từ ảnh bìa.
 * - Ảnh bìa bo góc lớn, tiêu đề căn trái.
 * - Cụm điều khiển playback to, dễ bấm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodcastPlayerScreen(
    onCloseClick: () -> Unit = {},
    viewModel: PodcastPlayerViewModel = hiltViewModel()
) {
    val currentEpisodeId by viewModel.currentEpisodeId.collectAsState()
    val title by viewModel.title.collectAsState()
    val artist by viewModel.artist.collectAsState()
    val artworkPath by viewModel.artworkPath.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val durationMs by viewModel.durationMs.collectAsState()
    val speed by viewModel.playbackSpeed.collectAsState()
    val playlist by viewModel.playlist.collectAsState()

    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(Color.Black) }
    var onDominantColor by remember { mutableStateOf(Color.White) }

    // Trích xuất màu từ ảnh bìa (Sử dụng coroutine an toàn)
    val currentArtworkPath = artworkPath
    LaunchedEffect(currentArtworkPath) {
        if (currentArtworkPath != null) {
            val request = ImageRequest.Builder(context)
                .data(currentArtworkPath.toAndroidAssetUri())
                .allowHardware(false)
                .size(200, 200)
                .build()
            
            val result = context.imageLoader.execute(request)
            if (result is coil.request.SuccessResult) {
                val bitmap = (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate()
                    }
                    palette.dominantSwatch?.let { swatch ->
                        dominantColor = Color(swatch.rgb)
                        onDominantColor = Color(swatch.bodyTextColor)
                    }
                }
            }
        }
    }

    LaunchedEffect(currentEpisodeId) {
        if (currentEpisodeId == null) onCloseClick()
    }

    val initialPage = remember(playlist, currentEpisodeId) {
        val index = playlist.indexOfFirst { it.id == currentEpisodeId }
        if (index == -1) 0 else index
    }
    
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (playlist.isEmpty()) 1 else playlist.size }
    )

    LaunchedEffect(currentEpisodeId, playlist) {
        if (playlist.isNotEmpty()) {
            val targetPage = playlist.indexOfFirst { it.id == currentEpisodeId }
            if (targetPage != -1 && targetPage != pagerState.currentPage) {
                pagerState.animateScrollToPage(targetPage)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (pagerState.isScrollInProgress && playlist.isNotEmpty() && page < playlist.size) {
                val episode = playlist[page]
                if (episode.id != currentEpisodeId) {
                    viewModel.playEpisode(episode)
                }
            }
        }
    }

    if (currentEpisodeId == null || title == null) {
        Scaffold { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                EmptyState(message = stringResource(R.string.podcast_empty_player))
            }
        }
        return
    }

    val animatedBgColor by animateColorAsState(
        targetValue = dominantColor.copy(alpha = 0.8f),
        label = "bgColor"
    )
    
    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.85f,
        label = "artworkScale"
    )

    var showUpNext by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = artworkPath?.toAndroidAssetUri(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 60.dp)
                .alpha(0.4f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            animatedBgColor.copy(alpha = 0.5f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onCloseClick) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = stringResource(R.string.player_close),
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* More options */ }) {
                            Icon(Icons.Default.MoreVert, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .scale(artworkScale),
                    beyondViewportPageCount = 1
                ) { page ->
                    val episode = playlist.getOrNull(page)
                    Surface(
                        modifier = Modifier
                            .size(320.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        tonalElevation = 12.dp,
                        shadowElevation = 8.dp
                    ) {
                        AsyncImage(
                            model = (episode?.artworkUrl ?: artworkPath)?.toAndroidAssetUri(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title.orEmpty(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    artist?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                var dragValue by remember { mutableStateOf<Float?>(null) }
                val shownPosition = dragValue?.let { (it * durationMs).toLong() } ?: positionMs
                Slider(
                    value = if (durationMs > 0) (shownPosition.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                    onValueChange = { dragValue = it },
                    onValueChangeFinished = {
                        dragValue?.let { viewModel.seekTo((it * durationMs).toLong()) }
                        dragValue = null
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = dominantColor,
                        activeTrackColor = dominantColor,
                        inactiveTrackColor = dominantColor.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formatDurationMs(shownPosition),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatDurationMs(durationMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = viewModel::skipToPrevious) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = viewModel::seekBackward) {
                        Icon(Icons.Rounded.Replay10, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                    
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(dominantColor.copy(alpha = 0.2f))
                    ) {
                        IconButton(
                            onClick = viewModel::togglePlayPause,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    color = dominantColor,
                                    modifier = Modifier.size(32.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = stringResource(if (isPlaying) R.string.pause else R.string.play),
                                    modifier = Modifier.size(48.dp),
                                    tint = dominantColor
                                )
                            }
                        }
                    }

                    IconButton(onClick = viewModel::seekForward) {
                        Icon(Icons.Rounded.Forward10, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                    IconButton(onClick = viewModel::skipToNext) {
                        Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(36.dp))
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = viewModel::cyclePlaybackSpeed,
                        label = { Text(formatSpeedLabel(speed)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Surface(
                        onClick = { showUpNext = true },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "UP NEXT",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp),
                            color = dominantColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        if (showUpNext) {
            ModalBottomSheet(
                onDismissRequest = { showUpNext = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                UpNextList(
                    episodes = playlist,
                    currentId = currentEpisodeId,
                    onEpisodeClick = {
                        viewModel.playEpisode(it)
                        showUpNext = false
                    }
                )
            }
        }
    }
}

@Composable
private fun UpNextList(
    episodes: List<PodcastEpisode>,
    currentId: String?,
    onEpisodeClick: (PodcastEpisode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Up Next",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(episodes, key = { it.id }) { episode ->
                val isSelected = episode.id == currentId
                Surface(
                    onClick = { onEpisodeClick(episode) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = episode.artworkUrl?.toAndroidAssetUri(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = episode.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = formatDurationMs(episode.duration),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
