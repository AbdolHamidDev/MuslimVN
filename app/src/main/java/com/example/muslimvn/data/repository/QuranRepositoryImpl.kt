package com.example.muslimvn.data.repository

import com.example.muslimvn.data.local.dao.QuranDao
import com.example.muslimvn.data.local.entities.AyahEntity
import com.example.muslimvn.data.local.entities.SurahEntity
import com.example.muslimvn.data.local.entities.TafsirEntity
import com.example.muslimvn.data.local.entities.VerseTimingEntity
import com.example.muslimvn.data.remote.QuranApiService
import com.example.muslimvn.data.util.QuranJsonParser
import com.example.muslimvn.data.util.TafsirTranslator
import com.example.muslimvn.domain.models.*
import com.example.muslimvn.domain.repository.QuranRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuranRepositoryImpl @Inject constructor(
    private val dao: QuranDao,
    private val apiService: QuranApiService,
    private val jsonParser: QuranJsonParser,
    private val translator: TafsirTranslator,
    private val gson: Gson
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

    override suspend fun getVerseTiming(verseKey: String, recitationId: Int): VerseTiming? {
        // 1. Check local cache first
        val cached = dao.getVerseTiming(verseKey, recitationId)
        if (cached != null) {
            val type = object : TypeToken<List<WordSegment>>() {}.type
            val segments: List<WordSegment> = gson.fromJson(cached.segmentsJson, type)
            return VerseTiming(verseKey, segments)
        }

        // 2. Fetch from remote if not cached
        return try {
            val response = apiService.getVerseWithAudio(verseKey = verseKey, recitationId = recitationId)
            val segments = response.verse.audio.segments.map { segment ->
                WordSegment(
                    wordIndex = segment[0].toInt(),
                    startTimeMs = segment[2].toLong(),
                    endTimeMs = segment[3].toLong()
                )
            }
            // 3. Save to cache
            val entity = VerseTimingEntity(
                verseKey = verseKey,
                reciterId = recitationId,
                segmentsJson = gson.toJson(segments)
            )
            dao.insertVerseTiming(entity)
            
            VerseTiming(verseKey, segments)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun prefetchSurahTiming(surahNumber: Int, recitationId: Int, onProgress: (Float) -> Unit) {
        val surah = getSurahByNumber(surahNumber) ?: return
        val totalAyahs = surah.totalAyahs
        
        for (i in 1..totalAyahs) {
            val verseKey = "$surahNumber:$i"
            getVerseTiming(verseKey, recitationId)
            onProgress(i.toFloat() / totalAyahs)
        }
    }

    override suspend fun getTafsir(verseKey: String, resourceId: Int): Tafsir? {
        val cached = dao.getTafsir(verseKey, resourceId)
        if (cached != null) {
            return Tafsir(cached.verseKey, cached.resourceId, cached.text, cached.translatedText)
        }

        return try {
            val response = apiService.getTafsir(resourceId, verseKey)
            val entity = TafsirEntity(
                verseKey = verseKey,
                resourceId = resourceId,
                text = response.tafsir.text
            )
            dao.insertTafsir(entity)
            Tafsir(entity.verseKey, entity.resourceId, entity.text)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun translateTafsir(verseKey: String, text: String): String? {
        // 1. Dịch văn bản
        val translated = translator.translate(text) ?: return null
        
        // 2. Cập nhật vào DB để lần sau dùng luôn
        val cached = dao.getTafsir(verseKey, 169)
        if (cached != null) {
            dao.insertTafsir(cached.copy(translatedText = translated))
        }
        
        return translated
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

    private fun AyahEntity.toDomain(): Ayah {
        var cleanText = textArabic
        // Nếu không phải Surah 1 (Fatiha) và là câu số 1 -> Loại bỏ Bismillah prefix nếu có
        // để khớp với dữ liệu timing từ Quran.com
        if (surahId != 1 && ayahNumber == 1) {
            val bismillahPrefix = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ "
            if (cleanText.startsWith(bismillahPrefix)) {
                cleanText = cleanText.removePrefix(bismillahPrefix)
            } else if (cleanText.startsWith("بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ")) {
                // Phân biệt một số biến thể unicode nếu có
                cleanText = cleanText.substringAfter("الرَّحِيْمِ").trim()
            }
        }
        
        return Ayah(
            id = id,
            surahId = surahId,
            ayahNumber = ayahNumber,
            textArabic = cleanText,
            textVietnamese = textVietnamese,
            isBookmarked = isBookmarked
        )
    }
}
