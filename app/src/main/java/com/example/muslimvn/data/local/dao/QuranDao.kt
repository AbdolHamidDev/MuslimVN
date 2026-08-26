package com.example.muslimvn.data.local.dao

import androidx.room.*
import com.example.muslimvn.data.local.entities.AyahEntity
import com.example.muslimvn.data.local.entities.SurahEntity
import com.example.muslimvn.data.local.entities.TafsirEntity
import com.example.muslimvn.data.local.entities.VerseTimingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuranDao {
    // ... existing methods ...
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerseTiming(timing: VerseTimingEntity)

    @Query("SELECT * FROM tafsirs WHERE verseKey = :verseKey AND resourceId = :resourceId")
    suspend fun getTafsir(verseKey: String, resourceId: Int): TafsirEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTafsir(tafsir: TafsirEntity)
}
