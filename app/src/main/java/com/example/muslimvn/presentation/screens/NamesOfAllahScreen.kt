package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.AllahName
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.viewmodels.NamesOfAllahViewModel
import com.example.muslimvn.ui.theme.extendedTypography

/**
 * Màn hình 99 Danh Xưng của Allah (Asmaul Husna).
 *
 * Thiết kế Material 3 theo ngôn ngữ Google:
 * - Thanh tìm kiếm nổi bo tròn (pill) lọc thời gian thực.
 * - Lưới 2 cột các card danh xưng với chữ Ả Rập lớn dùng font Amiri.
 * - Chạm vào card mở ModalBottomSheet chi tiết kèm phần phát âm & ý nghĩa.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NamesOfAllahScreen(
    onBackClick: () -> Unit = {},
    viewModel: NamesOfAllahViewModel = hiltViewModel()
) {
    val names by viewModel.namesList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedName by viewModel.selectedName.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_names_of_allah)) },
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
            // Thanh tìm kiếm nổi kiểu Google: pill bo tròn hoàn toàn, nền nhạt
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                placeholder = { Text(stringResource(R.string.names_of_allah_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.action_clear_search)
                            )
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

            when {
                isLoading -> LoadingIndicator(
                    label = stringResource(R.string.loading_please_wait),
                    modifier = Modifier.fillMaxSize()
                )

                names.isEmpty() && searchQuery.isNotBlank() -> EmptyState(
                    message = stringResource(R.string.search_no_results),
                    hint = stringResource(R.string.names_of_allah_search_no_results_hint),
                    modifier = Modifier.fillMaxSize()
                )

                // Đã tải xong nhưng danh sách trống → lỗi đọc dữ liệu
                names.isEmpty() -> ErrorState(
                    message = stringResource(R.string.names_of_allah_load_error),
                    onRetry = viewModel::retryLoading,
                    modifier = Modifier.fillMaxSize()
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.names_of_allah_count, names.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = names,
                            key = { it.number }
                        ) { name ->
                            AllahNameCard(
                                name = name,
                                onClick = { viewModel.onNameSelected(name) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom sheet chi tiết: giữ tham chiếu non-null trong biến local để smart-cast
    selectedName?.let { name ->
        AllahNameDetailSheet(
            name = name,
            onDismiss = viewModel::dismissDetail
        )
    }
}

/**
 * Card một danh xưng trong lưới: huy hiệu số thứ tự, chữ Ả Rập lớn (font Amiri),
 * phiên âm và ý nghĩa tiếng Việt. Nhấn có hiệu ứng "bouncy" + haptic nhẹ.
 */
@Composable
private fun AllahNameCard(
    name: AllahName,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.bouncyClick(pressedScale = 0.97f) { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Huy hiệu số thứ tự (1 → 99)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = name.number.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name.nameArabic,
                style = MaterialTheme.extendedTypography.arabicDisplay,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = name.transliteration,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name.meaningVietnamese,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Bottom sheet chi tiết một danh xưng: chữ Ả Rập cỡ lớn, phần phát âm (phiên âm),
 * ý nghĩa tiếng Việt và ghi chú ngắn về Asmaul Husna.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllahNameDetailSheet(
    name: AllahName,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Huy hiệu số thứ tự
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = stringResource(R.string.names_of_allah_number_badge, name.number),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Chữ Ả Rập cỡ lớn — điểm nhấn thị giác của sheet
            Text(
                text = name.nameArabic,
                style = MaterialTheme.extendedTypography.arabicDisplay.copy(
                    fontSize = MaterialTheme.extendedTypography.arabicDisplay.fontSize * 2,
                    lineHeight = MaterialTheme.extendedTypography.arabicDisplay.lineHeight * 2
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = name.transliteration,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            DetailInfoRow(
                icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null) },
                label = stringResource(R.string.names_of_allah_pronunciation),
                value = name.transliteration
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailInfoRow(
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null) },
                label = stringResource(R.string.names_of_allah_meaning),
                value = name.meaningVietnamese
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Ghi chú chung về Asmaul Husna
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.names_of_allah_about_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.names_of_allah_about_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Một hàng thông tin trong sheet chi tiết: icon tròn + nhãn + giá trị. */
@Composable
private fun DetailInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    icon()
                }
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}



