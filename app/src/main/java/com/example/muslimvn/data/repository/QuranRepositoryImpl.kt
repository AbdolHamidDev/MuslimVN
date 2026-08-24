package com.example.muslimvn.data.repository

import com.example.muslimvn.data.local.dao.QuranDao
import com.example.muslimvn.data.local.entities.AyahEntity
import com.example.muslimvn.data.local.entities.SurahEntity
import com.example.muslimvn.data.util.QuranJsonParser
import com.example.muslimvn.domain.models.Ayah
import com.example.muslimvn.domain.models.Surah
import com.example.muslimvn.domain.repository.QuranRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuranRepositoryImpl @Inject constructor(
    private val dao: QuranDao,
    private val jsonParser: QuranJsonParser
) : QuranRepository {

    override fun getSurahs(query: String): Flow<List<Surah>> {
        return if (query.isBlank()) {
            dao.getAllSurahs().map { entities -> entities.map { it.toDomain() } }
        } else {
            dao.searchSurahs(query).map { entities -> entities.map { it.toDomain() } }
        }
    }

    override fun getAyahsBySurah(surahId: Int): Flow<List<Ayah>> {
        return dao.getAyahsBySurah(surahId).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getSurahByNumber(number: Int): Surah? {
        return dao.getSurahByNumber(number)?.toDomain()
    }

    override suspend fun toggleBookmark(ayahId: Int, isBookmarked: Boolean) {
        dao.toggleBookmark(ayahId, isBookmarked)
    }

    override fun getBookmarkedAyahs(): Flow<List<Ayah>> {
        return dao.getBookmarkedAyahs().map { entities -> entities.map { it.toDomain() } }
    }

    override fun searchAyahs(query: String): Flow<List<Ayah>> {
        return dao.searchAyahs(query).map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun initializeData() {
        val existingSurahs = dao.getAllSurahs().first()
        if (existingSurahs.isEmpty()) {
            val (surahs, ayahs) = jsonParser.parseQuranData()
            dao.insertSurahs(surahs)
            dao.insertAyahs(ayahs)
        }
    }

    private fun SurahEntity.toDomain() = Surah(
        number = number,
        nameArabic = nameArabic,
        nameVietnamese = nameVietnamese,
        totalAyahs = totalAyahs,
        revelationType = revelationType
    )

    private fun AyahEntity.toDomain() = Ayah(
        id = id,
        surahId = surahId,
        ayahNumber = ayahNumber,
        textArabic = textArabic,
        textVietnamese = textVietnamese,
        isBookmarked = isBookmarked
    )
}
