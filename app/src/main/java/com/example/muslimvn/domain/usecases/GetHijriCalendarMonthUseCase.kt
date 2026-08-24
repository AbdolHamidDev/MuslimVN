package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.models.HijriCalendarMonth
import com.example.muslimvn.domain.repository.HijriCalendarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHijriCalendarMonthUseCase @Inject constructor(
    private val repository: HijriCalendarRepository
) {
    operator fun invoke(gregorianMonth: Int, gregorianYear: Int): Flow<HijriCalendarMonth> =
        repository.observeCalendarMonth(gregorianMonth, gregorianYear)
}