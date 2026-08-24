package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.models.Surah
import com.example.muslimvn.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSurahsUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(query: String = ""): Flow<List<Surah>> {
        return repository.getSurahs(query)
    }
}
