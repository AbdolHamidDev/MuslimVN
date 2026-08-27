package com.example.muslimvn.domain.repository

import com.example.muslimvn.data.local.entities.AzkarEntity
import kotlinx.coroutines.flow.Flow

interface AzkarRepository {
    fun getAllAzkar(): Flow<List<AzkarEntity>>
    fun getAzkarByCategory(category: String): Flow<List<AzkarEntity>>
    fun getFavoriteAzkar(): Flow<List<AzkarEntity>>
    fun getCategories(): Flow<List<String>>
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)
    suspend fun updateAzkar(azkar: AzkarEntity)
    suspend fun preloadAzkarIfNeeded()
}
