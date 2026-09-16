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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.AllahName
import com.example.muslimvn.presentation.components.EmptyState
import com.example.muslimvn.presentation.components.ErrorState
import com.example.muslimvn.presentation.components.LoadingIndicator
import com.example.muslimvn.presentation.components.bouncyClick
import com.example.muslimvn.presentation.viewmodels.NamesOfAllahViewModel
import com.example.muslimvn.ui.theme.extendedTypography
import kotlin.math.absoluteValue

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
    val isError by viewModel.isError.collectAsState()

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

                isError -> ErrorState(
                    message = stringResource(R.string.names_of_allah_load_error),
                    onRetry = viewModel::retryLoading,
                    modifier = Modifier.fillMaxSize()
                )

                // Khi đang đợi dữ liệu ban đầu từ namesList hoặc danh sách rỗng thực sự
                names.isEmpty() -> {
                    // Tránh hiện ErrorState nhấp nháy, hiện Box trống hoặc loading nhẹ
                    Box(modifier = Modifier.fillMaxSize())
                }

                else -> {
                    val pagerState = rememberPagerState(pageCount = { names.size })

                    // Reset về trang đầu khi danh sách thay đổi (do tìm kiếm)
                    LaunchedEffect(names) {
                        if (names.isNotEmpty()) {
                            pagerState.scrollToPage(0)
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Indicator vị trí: 1 / 99
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
                        ) {
                            Text(
                                text = if (names.isEmpty()) "0 / 0" 
                                       else "${pagerState.currentPage + 1} / ${names.size}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }

                        HorizontalPager(
                            state = pagerState,
                            contentPadding = PaddingValues(horizontal = 48.dp),
                            pageSpacing = 16.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) { page ->
                            val name = names[page]
                            
                            // Hiệu ứng scale & alpha cho các card bên cạnh
                            val pageOffset = (
                                (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            ).absoluteValue

                            AllahNamePagerItem(
                                name = name,
                                onClick = { viewModel.onNameSelected(name) },
                                modifier = Modifier
                                    .graphicsLayer {
                                        // Scale từ 1.0 (chính giữa) xuống 0.85 (bên cạnh)
                                        val scale = lerp(
                                            start = 0.85f,
                                            stop = 1f,
                                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                        )
                                        scaleX = scale
                                        scaleY = scale
                                        
                                        // Alpha từ 1.0 xuống 0.5
                                        alpha = lerp(
                                            start = 0.5f,
                                            stop = 1f,
                                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                                        )
                                    }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
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
 * Card hiển thị danh xưng Allah trong Carousel (HorizontalPager).
 * Tối ưu hóa cho việc hiển thị một tên chính giữa màn hình với chữ Ả Rập rất lớn.
 */
@Composable
private fun AllahNamePagerItem(
    name: AllahName,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp) // Chiều cao cố định để tạo sự cân đối trong carousel
            .bouncyClick(pressedScale = 0.98f) { onClick() },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Huy hiệu số thứ tự (1 → 99)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            ) {
                Text(
                    text = name.number.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Chữ Ả Rập là thành phần nổi bật nhất
            Text(
                text = name.nameArabic,
                style = MaterialTheme.extendedTypography.arabicDisplay.copy(
                    fontSize = 64.sp,
                    lineHeight = 80.sp
                ),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Phiên âm
            Text(
                text = name.transliteration,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Ý nghĩa tiếng Việt
            Text(
                text = name.meaningVietnamese,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Hint để người dùng biết có thể xem chi tiết
            Text(
                text = stringResource(R.string.action_view_detail).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Giữ lại card cũ nếu cần dùng ở nơi khác, nhưng ở màn hình này đã chuyển sang PagerItem.
 * Tao comment lại để tránh unused warning hoặc nếu mày muốn xoá hẳn cũng được.
 */
@Composable
private fun AllahNameCard(
    name: AllahName,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Component này hiện không còn được dùng trong Screen mới
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
                color = MaterialTheme.colorScheme.onSurface
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

            if (name.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                DetailInfoRow(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = stringResource(R.string.names_of_allah_description),
                    value = name.description
                )
            }

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


