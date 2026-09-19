package com.example.muslimvn.presentation.screens.podcast.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.muslimvn.ui.theme.CategoryColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.muslimvn.domain.models.Scholar
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.components.toAndroidAssetUri

/** Tiêu đề Section chuẩn Podcast App */
@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/** Tiêu đề Section thương hiệu: Logo lớn bên trái + Tên thương hiệu (Muslim Central) + Nút Mũi tên xem tất cả */
@Composable
fun BrandSectionTitle(
    title: String,
    logoPath: String,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        AsyncImage(
            model = logoPath.toAndroidAssetUri(),
            contentDescription = title,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (onSeeAllClick != null) {
            IconButton(onClick = onSeeAllClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Xem tất cả",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Trả về màu sắc đậm riêng biệt cho từng tag phân loại (Aqidah, Fiqh, Tafsir...)
 */
private fun getCategoryBadgeColor(tag: String): Color = CategoryColors.getPodcastCategoryBadgeColor(tag)

private fun getCategoryDisplayName(tag: String): String = when (tag.lowercase()) {
    "aqidah" -> "Aqidah"
    "fiqh" -> "Fiqh"
    "tafsir" -> "Tafsir"
    "tazkiyah" -> "Tazkiyah"
    "contemporary" -> "Đương đại"
    "inspiration" -> "Cảm hứng"
    else -> tag.replaceFirstChar { it.uppercase() }
}

/**
 * Phần danh sách học giả Muslim Central dạng LazyRow cuộn ngang:
 * - Kích thước Card 155dp x 225dp giúp hiển thị 2 học giả nguyên vẹn và để LỘ NHẸ Card thứ 3 ở góc phải màn hình.
 * - Nhìn vào là người dùng lập tức nhận biết còn học giả tiếp theo để vuốt qua.
 */
@Composable
fun MuslimCentralSection(
    scholars: List<Scholar>,
    onScholarClick: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(scholars, key = { it.id }) { scholar ->
            MuslimCentralScholarCard(
                scholar = scholar,
                modifier = Modifier.width(155.dp),
                onClick = { onScholarClick(scholar.id) }
            )
        }
    }
}

/**
 * Card Học giả Muslim Central dạng Hero Card tràn viền:
 * - Dùng được cho cả Carousel cuộn ngang và Lưới 2 cột (LazyVerticalGrid).
 * - Badge phân loại màu đậm chữ trắng ở góc trên.
 * - Tên học giả & Chức danh có hiệu ứng tự động trôi chữ marquee mượt mà nếu quá dài.
 */
@Composable
fun MuslimCentralScholarCard(
    scholar: Scholar,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(225.dp)
            .bouncyClick(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Ảnh học giả làm cover tràn toàn bộ Card
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
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // 3. Category Tag Badges màu đậm chữ trắng ở góc trên bên trái
            if (scholar.tags.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    scholar.tags.take(1).forEach { tag ->
                        Surface(
                            shape = CircleShape,
                            color = getCategoryBadgeColor(tag),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = getCategoryDisplayName(tag),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 4. Thông tin tên & chức danh đè lên chân Card có hiệu ứng tự động trôi chữ marquee nếu dài
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = scholar.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1200
                    )
                )
                if (scholar.title.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = scholar.title,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.82f),
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
