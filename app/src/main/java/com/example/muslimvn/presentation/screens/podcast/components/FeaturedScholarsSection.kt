package com.example.muslimvn.presentation.screens.podcast.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.toAndroidAssetUri
import kotlin.math.absoluteValue

/**
 * Phần "Học giả nổi bật" phong cách Hero Carousel cao cấp (Apple Music / Spotify Featured Banner):
 * - Mỗi Card học giả được căn giữa màn hình với `contentPadding` 44.dp.
 * - Các Card bên trái & bên phải tự động thu nhỏ (scale 0.88f) và làm mờ/làm tối (alpha 0.45f).
 * - Khi vuốt qua, Card mới mượt mà phóng to 100% và làm sáng ở vị trí trung tâm.
 */
@Composable
fun FeaturedScholarsSection(
    scholars: List<Scholar>,
    onScholarClick: (String) -> Unit
) {
    if (scholars.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { scholars.size })

    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(horizontal = 44.dp),
        pageSpacing = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) { page ->
        val scholar = scholars[page]

        FeaturedScholarHeroCard(
            scholar = scholar,
            pageOffsetProvider = {
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            },
            onClick = { onScholarClick(scholar.id) }
        )
    }
}

@Composable
private fun FeaturedScholarHeroCard(
    scholar: Scholar,
    pageOffsetProvider: () -> Float,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp)
            .graphicsLayer {
                val offset = pageOffsetProvider().coerceIn(0f, 1f)
                val scale = 0.88f + (1f - offset) * 0.12f
                val alpha = 0.45f + (1f - offset) * 0.55f

                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .bouncyClick(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Ảnh học giả làm cover tràn toàn bộ Hero Card
            AsyncImage(
                model = scholar.avatarPath.toAndroidAssetUri(),
                contentDescription = scholar.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Lớp phủ Gradient Overlay tối dần xuống chân Card
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // 3. Thông tin tên & chức danh đè lên chân Card có hiệu ứng trôi chữ marquee mượt mà nếu quá dài
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = scholar.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
                )
                if (scholar.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = scholar.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 1200
                        )
                    )
                }
            }
        }
    }
}
