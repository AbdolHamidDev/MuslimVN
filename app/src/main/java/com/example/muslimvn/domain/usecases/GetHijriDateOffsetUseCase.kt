package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.repository.HijriCalendarRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetHijriDateOffsetUseCase @Inject constructor(
    private val repository: HijriCalendarRepository
) {
    operator fun invoke(): Flow<Int> = repository.observeDateOffset()
}