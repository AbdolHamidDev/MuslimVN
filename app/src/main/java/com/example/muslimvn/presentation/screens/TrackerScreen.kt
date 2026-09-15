package com.example.muslimvn.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.core.navigation.LocalFloatingNavigationDockInset
import com.example.muslimvn.R
import com.example.muslimvn.presentation.viewmodels.*
import com.example.muslimvn.presentation.components.ShimmerPlaceholder
import com.example.muslimvn.ui.theme.MuslimVNTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(
    onQuranClick: (Int, Int) -> Unit,
    viewModel: TrackerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TrackerContent(
        uiState = uiState,
        onTogglePrayer = viewModel::togglePrayer,
        onToggleJamaah = viewModel::toggleJamaah,
        onToggleSunnahBefore = viewModel::toggleSunnahBefore,
        onToggleSunnahAfter = viewModel::toggleSunnahAfter,
        onSelectDate = viewModel::selectDate,
        onIncrementAzkar = viewModel::incrementAzkar,
        onResetAzkar = viewModel::resetAzkar,
        onQuranClick = onQuranClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerContent(
    uiState: TrackerUiState,
    onTogglePrayer: (Int) -> Unit,
    onToggleJamaah: (Int) -> Unit,
    onToggleSunnahBefore: (Int) -> Unit,
    onToggleSunnahAfter: (Int) -> Unit,
    onSelectDate: (Date) -> Unit,
    onIncrementAzkar: () -> Unit,
    onResetAzkar: () -> Unit,
    onQuranClick: (Int, Int) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.tracker_title)) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.prayers.isEmpty()) {
            TrackerLoadingSkeleton(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp + LocalFloatingNavigationDockInset.current)
        ) {
            // Weekly Calendar Strip
            item {
                WeeklyCalendarStrip(
                    days = uiState.weeklyDays,
                    onDateClick = onSelectDate
                )
            }

            // Daily Progress Overview
            item {
                DailyProgressHeader(uiState.prayers)
            }

            // Prayer Section Header
            item {
                TrackerSectionHeader(
                    title = stringResource(R.string.tracker_prayer_section),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Enhanced Prayer Items
            itemsIndexed(uiState.prayers) { index, prayer ->
                EnhancedPrayerItem(
                    prayer = prayer,
                    onToggle = { onTogglePrayer(index) },
                    onToggleJamaah = { onToggleJamaah(index) },
                    onToggleSunnahBefore = { onToggleSunnahBefore(index) },
                    onToggleSunnahAfter = { onToggleSunnahAfter(index) }
                )
            }

            // Quran Section
            item {
                TrackerSectionHeader(
                    title = stringResource(R.string.tracker_quran_section),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    QuranTrackerCard(
                        lastSurah = uiState.quran.lastSurahName,
                        lastAyah = uiState.quran.lastAyahNumber,
                        progress = uiState.quran.progress,
                        onClick = { onQuranClick(uiState.quran.lastSurahNumber, uiState.quran.lastAyahNumber) }
                    )
                }
            }

            // Azkar Section
            item {
                TrackerSectionHeader(
                    title = stringResource(R.string.tracker_azkar_section),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                )
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AzkarTrackerCard(
                        count = uiState.azkar.count,
                        onIncrement = onIncrementAzkar,
                        onReset = onResetAzkar
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerLoadingSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(64.dp))
        ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(136.dp))
        repeat(4) {
            ShimmerPlaceholder(modifier = Modifier.fillMaxWidth().height(76.dp))
        }
    }
}

@Composable
fun WeeklyCalendarStrip(
    days: List<DaySelection>,
    onDateClick: (Date) -> Unit
) {
    val dayFormat = SimpleDateFormat("E", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(days) { day ->
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (day.isSelected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .clickable { onDateClick(day.date) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dayFormat.format(day.date).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(day.date),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (day.isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                
                if (day.completionProgress > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (day.completionProgress >= 1f) Color(0xFF4CAF50) // Green if done
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun DailyProgressHeader(prayers: List<PrayerTrackerState>) {
    val completedCount = prayers.count { it.isCompleted }
    val totalCount = prayers.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Text(
                    text = "Tiến độ ngày hôm nay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Đã hoàn thành $completedCount / $totalCount lễ nguyện chính",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EnhancedPrayerItem(
    prayer: PrayerTrackerState,
    onToggle: () -> Unit,
    onToggleJamaah: () -> Unit,
    onToggleSunnahBefore: () -> Unit,
    onToggleSunnahAfter: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (prayer.isCompleted) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = if (prayer.isCompleted) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (prayer.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (prayer.isCompleted) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                            } else {
                                Text(
                                    text = prayer.name.take(1),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = prayer.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = timeFormat.format(prayer.time),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                
                Checkbox(
                    checked = prayer.isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            }

            AnimatedVisibility(visible = prayer.isCompleted) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Jamaah Toggle
                        FilterChip(
                            selected = prayer.isJamaah,
                            onClick = onToggleJamaah,
                            label = { Text("Jama'ah", fontSize = 12.sp) },
                            leadingIcon = if (prayer.isJamaah) {
                                { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )

                        // Sunnah Before
                        if (prayer.hasSunnahBefore) {
                            FilterChip(
                                selected = prayer.isSunnahBeforeCompleted,
                                onClick = onToggleSunnahBefore,
                                label = { Text("Sunnah Trước", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }

                        // Sunnah After
                        if (prayer.hasSunnahAfter) {
                            FilterChip(
                                selected = prayer.isSunnahAfterCompleted,
                                onClick = onToggleSunnahAfter,
                                label = { Text("Sunnah Sau", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackerSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
fun QuranTrackerCard(
    lastSurah: String,
    lastAyah: Int,
    progress: Float,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.tracker_last_read, ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = stringResource(R.string.tracker_surah_ayah, lastSurah, lastAyah),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}% ${stringResource(R.string.tracker_completion)}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun AzkarTrackerCard(
    count: Int,
    onIncrement: () -> Unit,
    onReset: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tracker_azkar_count, count),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                IconButton(onClick = onReset) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset")
                }
                FilledTonalIconButton(onClick = onIncrement) {
                    Icon(Icons.Default.Add, contentDescription = "Increment")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrackerScreenPreview() {
    MuslimVNTheme {
        TrackerContent(
            uiState = TrackerUiState(
                weeklyDays = listOf(
                    DaySelection(Date(), isSelected = true)
                ),
                prayers = listOf(
                    PrayerTrackerState("Fajr", isCompleted = true, hasSunnahBefore = true),
                    PrayerTrackerState("Dhuhr", hasSunnahBefore = true, hasSunnahAfter = true)
                )
            ),
            onTogglePrayer = {},
            onToggleJamaah = {},
            onToggleSunnahBefore = {},
            onToggleSunnahAfter = {},
            onSelectDate = {},
            onIncrementAzkar = {},
            onResetAzkar = {},
            onQuranClick = { _, _ -> }
        )
    }
}
