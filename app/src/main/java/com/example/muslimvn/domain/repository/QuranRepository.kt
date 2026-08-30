package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.*
import kotlinx.coroutines.flow.Flow

interface QuranRepository {
    fun getSurahs(query: String = ""): Flow<List<Surah>>
    fun getAyahsBySurah(surahId: Int): Flow<List<Ayah>>
    suspend fun getSurahByNumber(number: Int): Surah?
    suspend fun toggleBookmark(ayahId: Int, isBookmarked: Boolean)
    fun getBookmarkedAyahs(): Flow<List<Ayah>>
    fun searchAyahs(query: String): Flow<List<Ayah>>
    suspend fun getVerseTiming(verseKey: String, recitationId: Int): VerseTiming?
    suspend fun fetchAndCacheSurahTiming(surahNumber: Int, recitationId: Int): List<VerseTiming>
    suspend fun prefetchSurahTiming(surahNumber: Int, recitationId: Int, onProgress: (Float) -> Unit)
    suspend fun getTafsir(verseKey: String, resourceId: Int = 169): Tafsir?
    suspend fun translateTafsir(verseKey: String, text: String): String?
    suspend fun initializeData()
    
    fun getDownloadedAyahPath(verseKey: String, reciterId: Int): Flow<String?>
    suspend fun getDownloadedAyahPathSync(verseKey: String, reciterId: Int): String?
    fun getDownloadedAyahsCount(surahNumber: Int, reciterId: Int): Flow<Int>
    fun startSurahDownload(surahNumber: Int, reciterId: Int)

    // Full Quran download
    fun getFullQuranDownloadProgress(reciterId: Int): Flow<Float?>
    fun startFullQuranDownload(reciterId: Int, reciterName: String)
    fun pauseDownload(reciterId: Int)
    fun clearDownloadedAudio(reciterId: Int)
    fun clearSurahAudio(surahNumber: Int, reciterId: Int)
    fun getDownloadStatus(reciterId: Int): Flow<DownloadStatus>
    fun getSurahDownloadStatus(surahNumber: Int, reciterId: Int): Flow<DownloadStatus>

    // Mushaf Mapping
    suspend fun getPageForAyah(surah: Int, ayah: Int): Int
    suspend fun getAyahsForPage(page: Int): List<String>

    // Mushaf Download
    fun startMushafDownload()
    fun pauseMushafDownload()
    fun getMushafDownloadProgress(): Flow<Float?>
    fun getMushafDownloadStatus(): Flow<DownloadStatus>
    fun isMushafOffline(): Flow<Boolean>
    fun clearMushafData()
    
    // Ayah coordinates for Highlighting
    suspend fun getAyahCoordinates(verseKey: String): List<List<Int>>
}
