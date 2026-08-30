package com.example.muslimvn.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun MushafView(
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, 603),
        pageCount = { 604 }
    )

    // Đồng bộ từ bên ngoài vào Pager
    LaunchedEffect(initialPage) {
        val target = (initialPage - 1).coerceIn(0, 603)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    // Đồng bộ từ Pager ra ngoài khi người dùng dừng vuốt
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            onPageChanged(pagerState.currentPage + 1)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFBF8EF))) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                pageSpacing = 0.dp
            ) { pageIndex ->
                MushafPage(
                    pageNumber = pageIndex + 1
                )
            }
        }

        // Chỉ báo trang
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Trang ${pagerState.currentPage + 1}",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun MushafPage(
    pageNumber: Int
) {
    var retryKey by remember { mutableIntStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val formattedPage = pageNumber.toString().padStart(3, '0')
        
        val localFile = remember(pageNumber) {
            java.io.File(context.filesDir, "mushaf/page$formattedPage.png")
        }
        
        val imageData = remember(pageNumber, retryKey) {
            if (localFile.exists()) {
                localFile
            } else {
                "https://android.quran.com/data/width_1260/page$formattedPage.png"
            }
        }
        
        val imageRequest = remember(imageData, retryKey) {
            coil.request.ImageRequest.Builder(context)
                .data(imageData)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36")
                .crossfade(true)
                .build()
        }
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "Quran Page $pageNumber",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix(floatArrayOf(
                        0.97f, 0f, 0f, 0f, 0f,
                        0f, 0.95f, 0f, 0f, 0f,
                        0f, 0f, 0.9f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                ),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                    }
                },
                error = {
                    Button(onClick = { retryKey++ }) { Text("Thử lại trang $pageNumber") }
                }
            )

            // Lớp Highlight (Đã gỡ bỏ theo yêu cầu: Dừng lại ở màu nền vàng là đủ rồi)
        }
    }
}
