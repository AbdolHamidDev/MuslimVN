package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.HijriUpcomingEvent
import com.example.muslimvn.domain.repository.HijriCalendarRepository
import com.example.muslimvn.domain.util.HijriCalendarUtils
import com.example.muslimvn.domain.util.HijriMonthNames
import com.example.muslimvn.presentation.viewmodels.HijriCalendarUiState
import com.example.muslimvn.presentation.viewmodels.HijriCalendarViewModel
import java.time.LocalDate
import java.time.YearMonth

private const val INITIAL_PAGE = 1200 // Represents current month

/**
 * Islamic (Hijri) calendar screen:
 *  - Month navigation using HorizontalPager (swipeable).
 *  - "Today" button to jump back to current month.
 *  - Redesigned calendar grid with DOT indicators for events.
 *  - Upcoming events in M3 Cards.
 *  - Offset settings in ModalBottomSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(
    onBackClick: () -> Unit,
    viewModel: HijriCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    val pagerState = rememberPagerState(
        initialPage = INITIAL_PAGE,
        pageCount = { 2400 } // 200 years range
    )

    // Sync Pager -> ViewModel
    LaunchedEffect(pagerState.currentPage) {
        val monthsOffset = pagerState.currentPage - INITIAL_PAGE
        val targetMonth = YearMonth.now().plusMonths(monthsOffset.toLong())
        viewModel.gotoMonth(targetMonth)
    }

    // Sync ViewModel -> Pager (for "Today" button or other programatic jumps)
    val currentViewMonth = YearMonth.of(uiState.gregorianYear, uiState.gregorianMonth)
    LaunchedEffect(uiState.gregorianMonth, uiState.gregorianYear) {
        val monthsOffset = (currentViewMonth.year - YearMonth.now().year) * 12 + 
                          (currentViewMonth.monthValue - YearMonth.now().monthValue)
        val targetPage = INITIAL_PAGE + monthsOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hijri_calendar_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.gotoMonth(YearMonth.now())
                    }) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = stringResource(R.string.today)
                        )
                    }
                    IconButton(onClick = viewModel::openOffsetDialog) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = stringResource(R.string.hijri_offset_title)
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
        ) {
            HijriMonthHeader(
                uiState = uiState,
                onPreviousMonth = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.previousMonth() 
                },
                onNextMonth = { 
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    viewModel.nextMonth() 
                }
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) { _ ->
                // Content remains synced via uiState because the pager triggers viewModel.gotoMonth
                HijriCalendarContent(uiState = uiState)
            }
        }
    }

    if (uiState.showOffsetDialog) {
        HijriOffsetBottomSheet(
            offsetDays = uiState.offsetDays,
            onOffsetChange = viewModel::setOffset,
            onDismiss = viewModel::closeOffsetDialog
        )
    }
}

@Composable
private fun HijriCalendarContent(
    uiState: HijriCalendarUiState
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp)
    ) {
        if (uiState.isUsingFallback) {
            item { FallbackNotice() }
        }
        
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    WeekdayHeaderRow()
                    MonthGrid(uiState)
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.hijri_events_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 4.dp)
            )
        }

        if (uiState.upcomingEvents.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.hijri_empty_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        } else {
            items(uiState.upcomingEvents, key = { it.event.id }) { upcoming ->
                UpcomingEventCard(upcoming)
            }
        }
    }
}

@Composable
private fun HijriMonthHeader(
    uiState: HijriCalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    R.string.hijri_month_year,
                    HijriMonthNames.monthName(uiState.hijriMonthNumber),
                    uiState.hijriYear
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.hijri_gregorian_subtitle,
                    uiState.gregorianMonth,
                    uiState.gregorianYear
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun FallbackNotice() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.hijri_fallback_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun WeekdayHeaderRow() {
    val weekdays = stringArrayResource(R.array.hijri_weekdays_short)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        weekdays.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MonthGrid(uiState: HijriCalendarUiState) {
    val firstDay = LocalDate.of(uiState.gregorianYear, uiState.gregorianMonth, 1)
    val leadingEmptyCells = firstDay.dayOfWeek.value - 1
    val daysInMonth = YearMonth.of(uiState.gregorianYear, uiState.gregorianMonth).lengthOfMonth()
    val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7
    val daysByDate = uiState.days.associateBy { it.gregorianDate }
    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxWidth()) {
        repeat(totalCells / 7) { rowIndex ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { colIndex ->
                    val cellIndex = rowIndex * 7 + colIndex
                    val date = firstDay.minusDays((leadingEmptyCells - cellIndex).toLong())
                    val isInMonth = cellIndex in leadingEmptyCells until (leadingEmptyCells + daysInMonth)
                    val day = daysByDate[date]
                    val hijriDayNumber = day?.hijriDay
                        ?: HijriCalendarUtils.hijriDayNumberFor(date, uiState.offsetDays)

                    DayCell(
                        date = date,
                        isInMonth = isInMonth,
                        hijriDay = hijriDayNumber,
                        isToday = date == today && isInMonth,
                        hasEvents = day?.events?.isNotEmpty() == true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isInMonth: Boolean,
    hijriDay: Int?,
    isToday: Boolean,
    hasEvents: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = modifier
            .aspectRatio(0.85f)
            .padding(2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isInMonth) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        contentAlignment = Alignment.Center
    ) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday) FontWeight.Black else FontWeight.Medium,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary
                    isInMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
            )
            Text(
                text = hijriDay?.toString().orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isToday -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    isInMonth -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
                maxLines = 1
            )
            
            if (hasEvents && isInMonth) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun UpcomingEventCard(upcoming: HijriUpcomingEvent) {
    val event = upcoming.event
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = event.hijriDay.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = HijriMonthNames.monthName(event.hijriMonth).take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(event.nameResId),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = event.nameArabic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            CountdownBadge(countdownDays = upcoming.countdownDays)
        }
    }
}

@Composable
private fun CountdownBadge(countdownDays: Long) {
    val label: String
    val containerColor: Color
    val contentColor: Color
    
    when {
        countdownDays == 0L -> {
            label = stringResource(R.string.hijri_event_today)
            containerColor = MaterialTheme.colorScheme.error
            contentColor = MaterialTheme.colorScheme.onError
        }
        countdownDays > 0L -> {
            label = stringResource(R.string.hijri_event_in, countdownDays)
            containerColor = MaterialTheme.colorScheme.secondaryContainer
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        }
        else -> {
            label = stringResource(R.string.hijri_event_passed, -countdownDays)
            containerColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HijriOffsetBottomSheet(
    offsetDays: Int,
    onOffsetChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = stringResource(R.string.hijri_offset_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.hijri_offset_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))

            (HijriCalendarRepository.MIN_OFFSET_DAYS..HijriCalendarRepository.MAX_OFFSET_DAYS)
                .forEach { offset ->
                    val isSelected = offset == offsetDays
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onOffsetChange(offset) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onOffsetChange(offset) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (offset == 0) {
                                    stringResource(R.string.hijri_offset_none)
                                } else {
                                    stringResource(R.string.hijri_offset_days, offset)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
        }
    }
}
