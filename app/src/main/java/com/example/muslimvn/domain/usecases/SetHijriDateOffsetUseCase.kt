package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.repository.HijriCalendarRepository
import javax.inject.Inject

class SetHijriDateOffsetUseCase @Inject constructor(
    private val repository: HijriCalendarRepository
) {
    /** Values outside the supported -2..+2 range are clamped by the repository. */
    suspend operator fun invoke(offsetDays: Int) {
        repository.setDateOffset(offsetDays)
    }
}