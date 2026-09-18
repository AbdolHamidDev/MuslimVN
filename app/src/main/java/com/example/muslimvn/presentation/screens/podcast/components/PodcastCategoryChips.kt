package com.example.muslimvn.presentation.screens.podcast.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.PodcastCategory

/**
 * Hàng chip lọc phân loại podcast phong cách Apple Podcasts / Spotify:
 * - Hỗ trợ cuộn ngang mượt mà.
 * - Bo cong hình viên thuốc (CircleShape).
 */
@Composable
fun CategoryChipsRow(
    categories: List<PodcastCategory>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "all_chip") {
            val isSelected = selectedCategoryId == null
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(null) },
                label = {
                    Text(
                        text = stringResource(R.string.podcast_category_all),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
        items(categories, key = { it.id }) { category ->
            val isSelected = selectedCategoryId == category.id
            FilterChip(
                selected = isSelected,
                onClick = {
                    onCategorySelected(if (isSelected) null else category.id)
                },
                label = {
                    Text(
                        text = category.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}
