package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.Hadith

interface HadithRepository {
    fun cachedHadiths(): List<Hadith>
    /** Returns only a page-sized batch. Already loaded items are served from cache. */
    suspend fun loadNextBatch(batchSize: Int = 20): Result<List<Hadith>>
}
