package com.example.muslimvn.data.repository

import android.content.Context
import com.example.muslimvn.data.local.dao.AzkarDao
import com.example.muslimvn.data.local.entities.AzkarEntity
import com.example.muslimvn.domain.repository.AzkarRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class AzkarRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val azkarDao: AzkarDao
) : AzkarRepository {

    override fun getAllAzkar(): Flow<List<AzkarEntity>> = azkarDao.getAllAzkar()

    override fun getAzkarByCategory(category: String): Flow<List<AzkarEntity>> = 
        azkarDao.getAzkarByCategory(category)

    override fun getFavoriteAzkar(): Flow<List<AzkarEntity>> = azkarDao.getFavoriteAzkar()

    override fun getCategories(): Flow<List<String>> = azkarDao.getCategories()

    override suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        azkarDao.toggleFavorite(id, isFavorite)
    }

    override suspend fun updateAzkar(azkar: AzkarEntity) {
        azkarDao.updateAzkar(azkar)
    }

    override suspend fun preloadAzkarIfNeeded() {
        try {
            if (azkarDao.getAzkarCount() == 0) {
                val jsonString = context.assets.open("azkar_vi.json").bufferedReader().use { it.readText() }
                val type = object : TypeToken<List<AzkarEntity>>() {}.type
                val azkarList: List<AzkarEntity> = Gson().fromJson(jsonString, type)
                if (azkarList.isNotEmpty()) {
                    azkarDao.insertAzkarList(azkarList)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
