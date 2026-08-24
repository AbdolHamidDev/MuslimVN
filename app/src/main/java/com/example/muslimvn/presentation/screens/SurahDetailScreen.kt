package com.example.muslimvn.presentation.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.Ayah
import com.example.muslimvn.domain.usecases.SurahDetail
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.viewmodels.SurahDetailState
import com.example.muslimvn.presentation.viewmodels.SurahDetailViewModel
import com.example.muslimvn.ui.theme.extendedTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    onBackClick: () -> Unit,
    viewModel: SurahDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val currentMediaId by viewModel.currentMediaId.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state is SurahDetailState.Success) {
                        Text((state as SurahDetailState.Success).surahDetail.surah.nameVietnamese)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state is SurahDetailState.Success) {
                        IconButton(onClick = { viewModel.playContinuous(1) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_all))
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (currentMediaId != null) {
                PlayerControlBar(
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    currentMediaId = currentMediaId!!,
                    onPlayPauseClick = {
                        val ayahNumber = currentMediaId!!.split(":")[1].toInt()
                        viewModel.playAyah(ayahNumber)
                    },
                    onStopClick = { viewModel.stopAudio() }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val currentState = state) {
                is SurahDetailState.Loading -> {
                    LoadingIndicator(
                        label = stringResource(R.string.loading_please_wait),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SurahDetailState.Error -> {
                    ErrorState(
                        message = currentState.message,
                        onRetry = viewModel::retry,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is SurahDetailState.Success -> {
                    val surahDetail = currentState.surahDetail
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        item {
                            SurahHeader(surahDetail.surah.nameArabic, surahDetail.surah.nameVietnamese)
                        }
                        
                        if (surahDetail.surah.number != 1 && surahDetail.surah.number != 9) {
                            item {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.extendedTypography.arabicHeading,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        items(surahDetail.ayahs) { ayah ->
                            val isAyahPlaying = currentMediaId == "${surahDetail.surah.number}:${ayah.ayahNumber}"
                            AyahItem(
                                ayah = ayah,
                                isPlaying = isAyahPlaying && isPlaying,
                                isBuffering = isAyahPlaying && isBuffering,
                                onPlayClick = { viewModel.playAyah(ayah.ayahNumber) },
                                onBookmarkClick = { viewModel.toggleBookmark(ayah.id, !ayah.isBookmarked) },
                                onShareClick = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "${ayah.textArabic}\n\n${ayah.textVietnamese}")
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerControlBar(
    isPlaying: Boolean,
    isBuffering: Boolean,
    currentMediaId: String,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    val ayahNumber = currentMediaId.split(":")[1]
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Tự trừ navigation bar: bar này là bottomBar của Scaffold chi tiết,
                // root Scaffold không còn chèn inset đáy (đã zero ở MainActivity).
                .navigationBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.playing_ayah, ayahNumber),
                    style = MaterialTheme.typography.titleSmall
                )
                if (isBuffering) {
                    Text(
                        text = stringResource(R.string.buffering),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            IconButton(onClick = onPlayPauseClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play)
                )
            }

            IconButton(onClick = onStopClick) {
                Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.stop))
            }
        }
    }
}

@Composable
fun SurahHeader(nameArabic: String, nameVietnamese: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = nameArabic,
            style = MaterialTheme.extendedTypography.arabicDisplay,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = nameVietnamese,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AyahItem(
    ayah: Ayah,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onShareClick: () -> Unit
) {
    // Hàng phẳng không Card: khi đang phát, nền dền nhẹ màu primaryContainer
    // chuyển động mượt bằng spring thay vì viền đóng khung.
    val containerColor by animateColorAsState(
        targetValue = if (isPlaying) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ayahPlayingTint"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Số ayah theo kiểu kinh điển: dấu ngoặc trang trí Ả Rập + chữ số Ả Rập
            Text(
                text = ayah.ayahNumber.toArabicOrnate(),
                style = MaterialTheme.extendedTypography.arabicInline,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(4.dp)
            )
            
            Spacer(modifier = Modifier.weight(1f))

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onPlayClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                        tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onBookmarkClick) {
                Icon(
                    imageVector = if (ayah.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = stringResource(R.string.bookmark),
                    tint = if (ayah.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.share),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = ayah.textArabic,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = MaterialTheme.extendedTypography.arabicAyah,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = ayah.textVietnamese,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Đổi số sang chữ số Ả Rập và bọc trong dấu ngoặc trang trí, ví dụ: 5 → ﴿٥﴾ */
private fun Int.toArabicOrnate(): String = buildString {
    append('﴿')
    this@toArabicOrnate.toString().forEach { ch ->
        if (ch.isDigit()) append('٠' + ch.digitToInt()) else append(ch)
    }
    append('﴾')
}
