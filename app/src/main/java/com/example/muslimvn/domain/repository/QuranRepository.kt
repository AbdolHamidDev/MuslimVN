package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.Ayah
import com.example.muslimvn.domain.models.Surah
import com.example.muslimvn.domain.models.VerseTiming
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getSurahs(query: String = ""): Flow<List<Surah>>
    fun getAyahsBySurah(surahId: Int): Flow<List<Ayah>>
    suspend fun getSurahByNumber(number: Int): Surah?
    suspend fun toggleBookmark(ayahId: Int, isBookmarked: Boolean)
    fun getBookmarkedAyahs(): Flow<List<Ayah>>
    fun searchAyahs(query: String): Flow<List<Ayah>>
    suspend fun getVerseTiming(verseKey: String, recitationId: Int): VerseTiming?
    suspend fun prefetchSurahTiming(surahNumber: Int, recitationId: Int, onProgress: (Float) -> Unit)
    suspend fun initializeData()
}
