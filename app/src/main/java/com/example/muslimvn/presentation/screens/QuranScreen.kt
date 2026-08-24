package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.Surah
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.viewmodels.QuranViewModel
import com.example.muslimvn.ui.theme.extendedTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    onBackClick: () -> Unit = {},
    onSurahClick: (Int) -> Unit,
    viewModel: QuranViewModel = hiltViewModel()
) {
    val surahs by viewModel.surahs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_quran)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Thanh tìm kiếm: phần tử "card" DUY NHẤT được giữ khung trên màn danh sách
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                placeholder = { Text(stringResource(R.string.search_surah_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            )

            if (surahs.isEmpty()) {
                // Danh sách rỗng: đang tải lần đầu HOẶC tìm kiếm không có kết quả
                EmptyState(
                    message = if (searchQuery.isBlank()) {
                        stringResource(R.string.loading_please_wait)
                    } else {
                        stringResource(R.string.search_no_results)
                    },
                    hint = if (searchQuery.isBlank()) {
                        null
                    } else {
                        stringResource(R.string.search_no_results_hint)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Danh sách phẳng: các item không đóng khung, phân tách bằng divider mảnh
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(surahs) { surah ->
                        SurahItem(
                            surah = surah,
                            onClick = { onSurahClick(surah.number) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurahItem(
    surah: Surah,
    onClick: () -> Unit
) {
    // Hàng phẳng kiểu danh sách Google (Files/Gmail): KHÔNG đóng khung từng item,
    // chỉ phân tách bằng divider mảnh, nhạt ở dưới mỗi hàng.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClick(pressedScale = 0.99f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chấm số trung tính — điểm nhấn màu duy nhất của hàng là tên Ả Rập primary
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = surah.nameVietnamese,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.ayahs_revelation, surah.totalAyahs, surah.revelationType),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = surah.nameArabic,
                style = MaterialTheme.extendedTypography.arabicInline,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}
