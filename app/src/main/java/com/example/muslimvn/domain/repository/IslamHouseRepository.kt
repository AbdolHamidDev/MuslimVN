package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.ScholarDocument
import kotlinx.coroutines.flow.Flow

interface IslamHouseRepository {
    fun getAuthorDocuments(authorId: Long = 193689, page: Int = 1): Flow<Result<Pair<List<ScholarDocument>, Boolean>>>
    suspend fun loadMoreDocuments(authorId: Long = 193689, page: Int): Result<Pair<List<ScholarDocument>, Boolean>>
}
