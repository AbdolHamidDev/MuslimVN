package com.example.muslimvn.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.muslimvn.domain.models.HijriCalendarMonth
import com.example.muslimvn.domain.models.HijriDay
import com.example.muslimvn.domain.models.HijriUpcomingEvent
import com.example.muslimvn.domain.repository.HijriCalendarRepository
import com.example.muslimvn.domain.usecases.GetHijriCalendarMonthUseCase
import com.example.muslimvn.domain.usecases.GetHijriDateOffsetUseCase
import com.example.muslimvn.domain.usecases.RefreshHijriCalendarUseCase
import com.example.muslimvn.domain.usecases.SetHijriDateOffsetUseCase
import com.example.muslimvn.domain.util.HijriCalendarUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HijriCalendarUiState(
    val gregorianMonth: Int = LocalDate.now().monthValue,
    val gregorianYear: Int = LocalDate.now().year,
    val days: List<HijriDay> = emptyList(),
    val hijriMonthNumber: Int = 1,
    val hijriYear: Int = LocalDate.now().year - 622, // approximate until first load
    val offsetDays: Int = 0,
    val isUsingFallback: Boolean = false,
    val isLoading: Boolean = true,
    val showOffsetDialog: Boolean = false,
    val upcomingEvents: List<HijriUpcomingEvent> = emptyList()
)

/**
 * Drives the [HijriCalendarScreen]. The selected Gregorian month is kept as a
 * [YearMonth]; offset changes are reflected live because they flow through the
 * repository (Room cache + DataStore) and re-trigger the UI state emission.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HijriCalendarViewModel @Inject constructor(
    private val getHijriCalendarMonth: GetHijriCalendarMonthUseCase,
    private val getHijriDateOffset: GetHijriDateOffsetUseCase,
    private val setHijriDateOffset: SetHijriDateOffsetUseCase,
    private val refreshHijriCalendar: RefreshHijriCalendarUseCase
) : ViewModel() {

    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    private val _showOffsetDialog = MutableStateFlow(false)

    val uiState: StateFlow<HijriCalendarUiState> =
        combine(
            _displayedMonth.flatMapLatest { month ->
                getHijriCalendarMonth(month.monthValue, month.year)
            },
            getHijriDateOffset(),
            _showOffsetDialog
        ) { calendarMonth, offsetDays, showDialog ->
            calendarMonth.toUiState(offsetDays, showDialog)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HijriCalendarUiState()
        )

    init {
        refresh()
    }

    fun previousMonth() {
        _displayedMonth.update { it.minusMonths(1) }
        refresh()
    }

    fun nextMonth() {
        _displayedMonth.update { it.plusMonths(1) }
        refresh()
    }

    fun gotoMonth(month: YearMonth) {
        _displayedMonth.value = month
        refresh()
    }

    /** Background refresh (Aladhan -> Room cache) for the currently displayed month. */
    fun refresh() {
        val month = _displayedMonth.value
        viewModelScope.launch {
            refreshHijriCalendar(month.monthValue, month.year)
        }
    }

    /** Clamped to the supported -2.+2 range by the repository. */
    fun setOffset(offsetDays: Int) {
        viewModelScope.launch {
            setHijriDateOffset(
                offsetDays.coerceIn(
                    HijriCalendarRepository.MIN_OFFSET_DAYS,
                    HijriCalendarRepository.MAX_OFFSET_DAYS
                )
            )
        }
    }

    fun openOffsetDialog() {
        _showOffsetDialog.value = true
    }

    fun closeOffsetDialog() {
        _showOffsetDialog.value = false
    }

    private fun HijriCalendarMonth.toUiState(
        offsetDays: Int,
        showDialog: Boolean
    ): HijriCalendarUiState = HijriCalendarUiState(
        gregorianMonth = gregorianMonth,
        gregorianYear = gregorianYear,
        days = days,
        hijriMonthNumber = hijriMonthNumber,
        hijriYear = hijriYear,
        offsetDays = offsetDays,
        isUsingFallback = isUsingFallback,
        isLoading = false,
        showOffsetDialog = showDialog,
        upcomingEvents = if (hijriYear > 0) HijriCalendarUtils.eventsForHijriYear(hijriYear) else emptyList()
    )
}