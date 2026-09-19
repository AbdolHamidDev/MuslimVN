package com.example.muslimvn.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.muslimvn.core.utils.HadithStoryImageExporter
import com.example.muslimvn.domain.models.Hadith
import com.example.muslimvn.presentation.viewmodels.DailyReminderViewModel
import kotlinx.coroutines.launch

/**
 * Bảng màu thiết kế riêng cho màn hình trình chiếu Lời nhắc/Story (độc lập với theme hệ thống).
 */
private object DailyReminderStoryColors {
    val ScreenBackground = Color(0xFF102A27)
    val StoryGradientStart = Color(0xFF17483F)
    val StoryGradientCenter = Color(0xFF0C201E)
    val StoryGradientEnd = Color(0xFF12332E)
    val ControlBackground = Color.Black.copy(alpha = 0.24f)
    val HeaderText = Color(0xFFBDE7D2)
    val SubtitleText = Color(0xFFD5E8DF)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyReminderViewerScreen(initialHadithId: String, onBackClick: () -> Unit, viewModel: DailyReminderViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val initialPage = remember(state.stories, initialHadithId) { state.stories.indexOfFirst { it.id == initialHadithId }.coerceAtLeast(0) }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { state.stories.size })
    fun save() = state.stories.getOrNull(pagerState.currentPage)?.let { story -> scope.launch {
        HadithStoryImageExporter.saveToGallery(context, story).onSuccess { snackbar.showSnackbar("Đã lưu vào thư viện") }
            .onFailure { snackbar.showSnackbar("Không thể lưu hình ảnh. Vui lòng thử lại.") }
    } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) save() else scope.launch { snackbar.showSnackbar("Cần quyền truy cập thư viện ảnh để lưu hình ảnh.") }
    }
    BackHandler(onBack = onBackClick)
    LaunchedEffect(state.stories.size, initialHadithId) {
        val target = state.stories.indexOfFirst { it.id == initialHadithId }
        if (target >= 0 && pagerState.currentPage == 0 && initialHadithId != state.stories.firstOrNull()?.id) pagerState.scrollToPage(target)
    }
    LaunchedEffect(pagerState.currentPage, state.stories.size) { viewModel.preloadIfNeeded(pagerState.currentPage) }
    if (state.stories.isEmpty()) {
        Box(Modifier.fillMaxSize().background(DailyReminderStoryColors.ScreenBackground), contentAlignment = Alignment.Center) { Text("Đang chuẩn bị lời nhắc…", color = Color.White) }
        return
    }
    Box(Modifier.fillMaxSize().background(DailyReminderStoryColors.ScreenBackground)) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { HadithStory(state.stories[it]) }
        Row(
            Modifier.fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Control(onBackClick, Icons.AutoMirrored.Filled.ArrowBack, "Quay lại")
            Control({
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else save()
            }, Icons.Default.Download, "Lưu hình ảnh")
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp))
    }
}

@Composable private fun Control(onClick: () -> Unit, icon: ImageVector, description: String) =
    IconButton(onClick, Modifier.background(DailyReminderStoryColors.ControlBackground, CircleShape).size(48.dp)) { Icon(icon, description, tint = Color.White) }

@Composable
private fun HadithStory(story: Hadith) = BoxWithConstraints(
    Modifier.fillMaxSize()
        .background(
            Brush.verticalGradient(
                listOf(
                    DailyReminderStoryColors.StoryGradientStart,
                    DailyReminderStoryColors.StoryGradientCenter,
                    DailyReminderStoryColors.StoryGradientEnd
                )
            )
        )
        .padding(horizontal = 28.dp, vertical = 76.dp)
) {
    val length = story.text.length
    val font = when { length <= 260 -> 28.sp; length <= 520 -> 24.sp; length <= 850 -> 21.sp; else -> 18.sp }
    val line = when { length <= 260 -> 38.sp; length <= 520 -> 33.sp; length <= 850 -> 29.sp; else -> 25.sp }
    val compact = length > 520
    Column(Modifier.fillMaxSize()) {
        Text(
            "LỜI NHẮC HÔM NAY",
            modifier = Modifier.fillMaxWidth(),
            color = DailyReminderStoryColors.HeaderText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
        Box(Modifier.weight(1f), contentAlignment = if (compact) Alignment.TopStart else Alignment.CenterStart) {
            val readableArea = if (length > 850) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxWidth()
            Text(story.text, readableArea, Color.White, fontSize = font, lineHeight = line, fontWeight = FontWeight.Medium, textAlign = TextAlign.Start)
        }
        Spacer(Modifier.height(if (compact) 12.dp else 24.dp))
        story.attribution?.let { Text(it, color = DailyReminderStoryColors.SubtitleText, style = MaterialTheme.typography.titleSmall, maxLines = 2) }
        story.grade?.let { Text(it, color = DailyReminderStoryColors.HeaderText, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        story.reference?.let { Text(it, color = DailyReminderStoryColors.HeaderText, style = MaterialTheme.typography.bodySmall, maxLines = 1) }
        Spacer(Modifier.height(12.dp))
        Text("Nguồn: HadeethEnc.com", Modifier.align(Alignment.CenterHorizontally), color = DailyReminderStoryColors.HeaderText, style = MaterialTheme.typography.labelSmall)
    }
}
