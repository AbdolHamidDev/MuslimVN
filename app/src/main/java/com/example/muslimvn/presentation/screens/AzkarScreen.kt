package com.example.muslimvn.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.muslimvn.data.local.entities.AzkarEntity
import com.example.muslimvn.presentation.viewmodels.AzkarViewModel
import androidx.compose.runtime.CompositionLocalProvider
import com.example.muslimvn.ui.theme.extendedTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    viewModel: AzkarViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val azkarList by viewModel.azkarList.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            // Thanh tìm kiếm Google Pill với gợi ý thông minh
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::updateSearchQuery,
                    onSearch = { isSearchActive = false },
                    active = isSearchActive,
                    onActiveChange = { isSearchActive = it },
                    placeholder = { Text("Tìm bài Azkar & Dua...") },
                    leadingIcon = {
                        IconButton(onClick = { 
                            if (isSearchActive) {
                                isSearchActive = false
                                viewModel.updateSearchQuery("")
                            } else {
                                onBackClick()
                            }
                        }) {
                            Icon(
                                imageVector = if (isSearchActive) Icons.Default.Search else Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Xoá tìm kiếm")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    // HIỂN THỊ GỢI Ý: Chỉ hiện khi người dùng ĐANG NHẬP (query không trống)
                    if (searchQuery.trim().isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(azkarList, key = { it.id }) { azkar ->
                                ListItem(
                                    headlineContent = { 
                                        Text(
                                            text = azkar.title,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = azkar.contentVietnamese,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            imageVector = Icons.Default.Book, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    },
                                    modifier = Modifier
                                        .animateItem()
                                        .clickable {
                                            viewModel.updateSearchQuery(azkar.title)
                                            isSearchActive = false
                                        }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    } else {
                        // Trạng thái chờ khi chưa nhập gì
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Search, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Nhập để tìm kiếm toàn bộ Azkar", 
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            if (!isSearchActive) {
                // Danh mục chọn bài (Category Selector)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("Tất cả") }
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.selectCategory(category) },
                            label = { Text(category) }
                        )
                    }
                }

                if (azkarList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "Đang tải dữ liệu..." else "Không tìm thấy kết quả",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    // Danh sách chính (Full Cards)
                    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(azkarList, key = { it.id }) { azkar ->
                                AzkarItem(
                                    azkar = azkar,
                                    onFavoriteClick = { viewModel.toggleFavorite(azkar) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AzkarItem(
    azkar: AzkarEntity,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var count by remember { mutableIntStateOf(0) }
    val haptic = LocalHapticFeedback.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = azkar.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (azkar.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (azkar.isFavorite) "Bỏ yêu thích" else "Thêm vào yêu thích",
                        tint = if (azkar.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Text(
                    text = azkar.contentArabic,
                    style = MaterialTheme.extendedTypography.arabicHeading.copy(
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = azkar.contentTransliteration,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = azkar.contentVietnamese,
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(16.dp))

            // Counter Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Nguồn: ${azkar.reference}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Lặp lại: ${azkar.repeatCount} lần",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    onClick = {
                        if (count < azkar.repeatCount) {
                            count++
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = if (count >= azkar.repeatCount) 
                        MaterialTheme.colorScheme.surfaceContainerHighest 
                    else 
                        MaterialTheme.colorScheme.secondaryContainer,
                    enabled = count < azkar.repeatCount
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$count/${azkar.repeatCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (count >= azkar.repeatCount)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}
