package com.example.muslimvn.data.repository

import com.example.muslimvn.data.remote.IslamHouseApiService
import com.example.muslimvn.domain.models.ScholarDocument
import com.example.muslimvn.domain.repository.IslamHouseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class IslamHouseRepositoryImpl @Inject constructor(
    private val apiService: IslamHouseApiService
) : IslamHouseRepository {

    // In-memory cache to avoid redundant API calls within the app session
    private val documentCache = mutableMapOf<String, Pair<List<ScholarDocument>, Boolean>>()

    override fun getAuthorDocuments(authorId: Long, page: Int): Flow<Result<Pair<List<ScholarDocument>, Boolean>>> = flow {
        val cacheKey = "${authorId}_$page"
        documentCache[cacheKey]?.let {
            emit(Result.success(it))
            return@flow
        }

        try {
            val response = apiService.getAuthorItems(authorId = authorId, page = page)
            val items = response.data.orEmpty().map { item ->
                val attachments = item.attachments.orEmpty()
                val audioAttachment = attachments.find {
                    val ext = it.extensionType?.lowercase()
                    ext == "mp3" || ext == "wav" || ext == "m4a" || ext == "aac"
                } ?: attachments.firstOrNull()

                ScholarDocument(
                    id = item.id,
                    title = item.title ?: "Không có tiêu đề",
                    type = item.type ?: "document",
                    description = item.description,
                    addDate = item.addDate,
                    fileExtension = audioAttachment?.extensionType ?: item.type,
                    fileSize = audioAttachment?.size,
                    downloadUrl = audioAttachment?.url,
                    detailUrl = item.apiUrl,
                    imageUrl = item.image
                )
            }
            val totalPages = response.links?.pagesNumber ?: 1
            val hasNext = page < totalPages
            val result = Pair(items, hasNext)
            
            documentCache[cacheKey] = result
            emit(Result.success(result))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun loadMoreDocuments(authorId: Long, page: Int): Result<Pair<List<ScholarDocument>, Boolean>> {
        val cacheKey = "${authorId}_$page"
        documentCache[cacheKey]?.let {
            return Result.success(it)
        }

        return try {
            val response = apiService.getAuthorItems(authorId = authorId, page = page)
            val items = response.data.orEmpty().map { item ->
                val attachments = item.attachments.orEmpty()
                val audioAttachment = attachments.find {
                    val ext = it.extensionType?.lowercase()
                    ext == "mp3" || ext == "wav" || ext == "m4a" || ext == "aac"
                } ?: attachments.firstOrNull()

                ScholarDocument(
                    id = item.id,
                    title = item.title ?: "Không có tiêu đề",
                    type = item.type ?: "document",
                    description = item.description,
                    addDate = item.addDate,
                    fileExtension = audioAttachment?.extensionType ?: item.type,
                    fileSize = audioAttachment?.size,
                    downloadUrl = audioAttachment?.url,
                    detailUrl = item.apiUrl,
                    imageUrl = item.image
                )
            }
            val totalPages = response.links?.pagesNumber ?: 1
            val hasNext = page < totalPages
            val result = Pair(items, hasNext)
            
            documentCache[cacheKey] = result
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
