package com.example.muslimvn.data.local.dao

import androidx.room.*
import com.example.muslimvn.data.local.entities.AyahEntity
import com.example.muslimvn.data.local.entities.DownloadedAyahEntity
import com.example.muslimvn.data.local.entities.SurahEntity
import com.example.muslimvn.data.local.entities.TafsirEntity
import com.example.muslimvn.data.local.entities.VerseTimingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedAyah(downloadedAyah: DownloadedAyahEntity)

    @Query("SELECT * FROM downloaded_ayahs WHERE verseKey = :verseKey AND reciterId = :reciterId")
    suspend fun getDownloadedAyah(verseKey: String, reciterId: Int): DownloadedAyahEntity?

    @Query("SELECT localPath FROM downloaded_ayahs WHERE verseKey = :verseKey AND reciterId = :reciterId")
    fun getDownloadedAyahPathFlow(verseKey: String, reciterId: Int): Flow<String?>

    @Query("SELECT COUNT(*) FROM downloaded_ayahs WHERE verseKey LIKE :surahNumber || ':%' AND reciterId = :reciterId")
    suspend fun getDownloadedAyahsCount(surahNumber: Int, reciterId: Int): Int

    @Query("SELECT COUNT(*) FROM downloaded_ayahs WHERE verseKey LIKE :surahNumber || ':%' AND reciterId = :reciterId")
    fun getDownloadedAyahsCountFlow(surahNumber: Int, reciterId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloaded_ayahs WHERE reciterId = :reciterId")
    fun getTotalDownloadedAyahsCount(reciterId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM downloaded_ayahs WHERE reciterId = :reciterId")
    suspend fun getTotalDownloadedAyahsCountSync(reciterId: Int): Int

    @Query("DELETE FROM downloaded_ayahs WHERE reciterId = :reciterId")
    suspend fun deleteAllDownloadedAyahs(reciterId: Int)

    @Query("DELETE FROM downloaded_ayahs WHERE verseKey LIKE :surahNumber || ':%' AND reciterId = :reciterId")
    suspend fun deleteSurahDownloadedAyahs(surahNumber: Int, reciterId: Int)

    @Query("SELECT * FROM downloaded_ayahs WHERE verseKey LIKE :surahNumber || ':%' AND reciterId = :reciterId")
    suspend fun getDownloadedAyahsForSurah(surahNumber: Int, reciterId: Int): List<DownloadedAyahEntity>
    
    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE nameVietnamese LIKE '%' || :query || '%' OR nameArabic LIKE '%' || :query || '%' ORDER BY number ASC")
    fun searchSurahs(query: String): Flow<List<SurahEntity>>

    @Query("SELECT * FROM ayahs WHERE surahId = :surahId ORDER BY ayahNumber ASC")
    fun getAyahsBySurah(surahId: Int): Flow<List<AyahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :number")
    suspend fun getSurahByNumber(number: Int): SurahEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSurahs(surahs: List<SurahEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAyahs(ayahs: List<AyahEntity>)

    @Query("UPDATE ayahs SET isBookmarked = :isBookmarked WHERE id = :ayahId")
    suspend fun toggleBookmark(ayahId: Int, isBookmarked: Boolean)

    @Query("SELECT * FROM ayahs WHERE isBookmarked = 1")
    fun getBookmarkedAyahs(): Flow<List<AyahEntity>>
    
    @Transaction
    @Query("SELECT * FROM ayahs WHERE textVietnamese LIKE '%' || :query || '%'")
    fun searchAyahs(query: String): Flow<List<AyahEntity>>

    @Query("SELECT * FROM verse_timings WHERE verseKey = :verseKey AND reciterId = :reciterId")
    suspend fun getVerseTiming(verseKey: String, reciterId: Int): VerseTimingEntity?

    @Query("SELECT * FROM verse_timings WHERE verseKey LIKE :surahNumber || ':%' AND reciterId = :reciterId")
    suspend fun getVerseTimingsForSurah(surahNumber: Int, reciterId: Int): List<VerseTimingEntity>

    @Query("SELECT COUNT(*) FROM verse_timings WHERE verseKey LIKE :surahNumber || ':%' AND reciterId = :reciterId")
    suspend fun getVerseTimingCountForSurah(surahNumber: Int, reciterId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerseTiming(timing: VerseTimingEntity)

    @Query("SELECT * FROM tafsirs WHERE verseKey = :verseKey AND resourceId = :resourceId")
    suspend fun getTafsir(verseKey: String, resourceId: Int): TafsirEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTafsir(tafsir: TafsirEntity)
}
