package com.example.muslimvn.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

import androidx.compose.ui.tooling.preview.Preview

/**
 * Dữ liệu tạm thời cho các học giả Việt Nam để dễ dàng cập nhật trong code.
 */
data class VietnamScholar(
    val id: String,
    val name: String,
    val role: String,
    val avatarPath: String,
    val youtubeUrl: String? = null
)

val vietnamScholarsList = listOf(
    VietnamScholar(
        id = "mach_zen",
        name = "Mách Zên",
        role = "Học giả Islam",
        avatarPath = "images/featured_scholars_vietnam/mach_zen.webp",
        youtubeUrl = "https://www.youtube.com/@islamlavn/videos"
    ),
    VietnamScholar(
        id = "gosaly_ahmad",
        name = "Gosaly Ahmad",
        role = "Học giả Islam",
        avatarPath = "images/featured_scholars_vietnam/Gosaly_Ahmad.webp",
        youtubeUrl = "https://www.youtube.com/@gosalyahmad-Unofficial"
    )
)

@Composable
fun VietnamScholarsSection(
    modifier: Modifier = Modifier,
    onScholarClick: (VietnamScholar) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Học giả Việt Nam",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(vietnamScholarsList) { scholar ->
                VietnamScholarCard(
                    scholar = scholar,
                    onClick = { onScholarClick(scholar) }
                )
            }
        }
    }
}

@Composable
fun VietnamScholarCard(
    scholar: VietnamScholar,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .width(160.dp)
            .bouncyClick(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier
                .size(160.dp)
                .aspectRatio(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp
        ) {
            AsyncImage(
                model = scholar.avatarPath.toAndroidAssetUri(),
                contentDescription = scholar.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = scholar.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = scholar.role,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VietnamScholarsSectionPreview() {
    MaterialTheme {
        VietnamScholarsSection()
    }
}
