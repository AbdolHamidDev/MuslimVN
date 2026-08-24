package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.repository.HijriCalendarRepository
import javax.inject.Inject

class RefreshHijriCalendarUseCase @Inject constructor(
    private val repository: HijriCalendarRepository
) {
    /** Silently updates the Room cache from Aladhan when online and the cache is stale/missing. */
    suspend operator fun invoke(gregorianMonth: Int, gregorianYear: Int) {
        repository.refreshCalendarMonth(gregorianMonth, gregorianYear)
    }
}