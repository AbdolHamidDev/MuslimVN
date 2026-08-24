package com.example.muslimvn.domain.usecases

import com.example.muslimvn.domain.models.Ayah
import com.example.muslimvn.domain.models.Surah
import com.example.muslimvn.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

data class SurahDetail(
    val surah: Surah,
    val ayahs: List<Ayah>
)

class GetSurahDetailUseCase @Inject constructor(
    private val repository: QuranRepository
) {
    operator fun invoke(surahNumber: Int): Flow<SurahDetail?> {
        return flow {
            val surah = repository.getSurahByNumber(surahNumber)
            if (surah != null) {
                repository.getAyahsBySurah(surahNumber).collect { ayahs ->
                    emit(SurahDetail(surah, ayahs))
                }
            } else {
                emit(null)
            }
        }
    }
}
