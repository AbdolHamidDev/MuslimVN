package com.example.muslimvn.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.muslimvn.R
import com.example.muslimvn.domain.models.HijriUpcomingEvent
import com.example.muslimvn.domain.repository.HijriCalendarRepository
import com.example.muslimvn.domain.util.HijriCalendarUtils
import com.example.muslimvn.domain.util.HijriMonthNames
import com.example.muslimvn.presentation.viewmodels.HijriCalendarUiState
import com.example.muslimvn.presentation.viewmodels.HijriCalendarViewModel
import java.time.LocalDate
import java.time.YearMonth
/**
 * Islamic (Hijri) calendar screen:
 *  - month navigation header ("Ramadan 1447 AH") with an offset settings icon;
 *  - a full Gregorian month grid showing both Gregorian & Hijri day numbers,
 *    highlighting today and days carrying Islamic events;
 *  - an upcoming-events list with Vietnamese translations and countdown badges;
 *  - an adjustment dialog that updates the grid behind it live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(
    onBackClick: () -> Unit,
    viewModel: HijriCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    IconButton(onClick = viewModel::openOffsetDialog) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = stringResource(R.string.hijri_offset_title)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.days.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            HijriCalendarContent(
                uiState = uiState,
                onPreviousMonth = viewModel::previousMonth,
                onNextMonth = viewModel::nextMonth,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }

    if (uiState.showOffsetDialog) {
        HijriOffsetDialog(
            offsetDays = uiState.offsetDays,
            onOffsetChange = viewModel::setOffset,
            onDismiss = viewModel::closeOffsetDialog
        )
    }
}

@Composable
private fun HijriCalendarContent(
    uiState: HijriCalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            HijriMonthHeader(
                uiState = uiState,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
        }
        if (uiState.isUsingFallback) {
            item { FallbackNotice() }
        }
        item { WeekdayHeaderRow() }
        item { MonthGrid(uiState) }
        item {
            Text(
                text = stringResource(R.string.hijri_events_title).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
        }
        if (uiState.upcomingEvents.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.hijri_empty_events),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.upcomingEvents, key = { it.event.id }) { upcoming ->
                UpcomingEventRow(upcoming)
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
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.hijri_prev_month)
            )
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
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(
                    R.string.hijri_gregorian_subtitle,
                    uiState.gregorianMonth,
                    uiState.gregorianYear
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = stringResource(R.string.hijri_next_month)
            )
        }
    }
}

@Composable
private fun FallbackNotice() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.hijri_fallback_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
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
            .padding(vertical = 4.dp)
    ) {
        weekdays.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun MonthGrid(uiState: HijriCalendarUiState) {
    val firstDay = LocalDate.of(uiState.gregorianYear, uiState.gregorianMonth, 1)
    val leadingEmptyCells = firstDay.dayOfWeek.value - 1 // Monday=1 -> 0..6 leading blanks
    val daysInMonth = YearMonth.of(uiState.gregorianYear, uiState.gregorianMonth).lengthOfMonth()
    val totalCells = ((leadingEmptyCells + daysInMonth + 6) / 7) * 7
    val daysByDate = uiState.days.associateBy { it.gregorianDate }
    val offsetDays = uiState.offsetDays
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
                        ?: HijriCalendarUtils.hijriDayNumberFor(date, offsetDays)

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
    val background = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    val gregorianColor = when {
        isToday -> onPrimary
        isInMonth -> onSurface
        else -> onSurfaceVariant.copy(alpha = 0.35f)
    }
    val hijriColor = when {
        isToday -> onPrimary.copy(alpha = 0.75f)
        isInMonth -> onSurfaceVariant.copy(alpha = 0.8f)
        else -> onSurfaceVariant.copy(alpha = 0.35f)
    }

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (hasEvents && isInMonth && !isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                background
            })
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            color = gregorianColor
        )
        Text(
            text = hijriDay?.toString().orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = hijriColor,
            maxLines = 1
        )
        if (hasEvents && isInMonth) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(if (isToday) onPrimary else MaterialTheme.colorScheme.tertiary)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
@Composable
private fun UpcomingEventRow(upcoming: HijriUpcomingEvent) {
    val event = upcoming.event
    // Hàng phẳng kiểu danh sách Google: không Card, phân tách bằng divider mảnh
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${event.hijriMonth}/${event.hijriDay}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(event.nameResId),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = event.nameArabic,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            CountdownBadge(countdownDays = upcoming.countdownDays)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}

@Composable
private fun CountdownBadge(countdownDays: Long) {
    val label: String
    val container: Color
    val content: Color
    when {
        countdownDays == 0L -> {
            label = stringResource(R.string.hijri_event_today)
            container = MaterialTheme.colorScheme.primary
            content = MaterialTheme.colorScheme.onPrimary
        }
        countdownDays > 0L -> {
            label = stringResource(R.string.hijri_event_in, countdownDays)
            container = MaterialTheme.colorScheme.secondaryContainer
            content = MaterialTheme.colorScheme.onSecondaryContainer
        }
        else -> {
            label = stringResource(R.string.hijri_event_passed, -countdownDays)
            container = MaterialTheme.colorScheme.surfaceVariant
            content = MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = container
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun HijriOffsetDialog(
    offsetDays: Int,
    onOffsetChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.hijri_offset_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.hijri_offset_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                (HijriCalendarRepository.MIN_OFFSET_DAYS..HijriCalendarRepository.MAX_OFFSET_DAYS)
                    .forEach { offset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOffsetChange(offset) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = offset == offsetDays,
                                onClick = { onOffsetChange(offset) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (offset == 0) {
                                    stringResource(R.string.hijri_offset_none)
                                } else {
                                    stringResource(R.string.hijri_offset_days, offset)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.hijri_offset_close))
            }
        }
    )
}