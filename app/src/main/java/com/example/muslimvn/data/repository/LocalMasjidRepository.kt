package com.example.muslimvn.data.repository

import com.example.muslimvn.data.local.masjid.MasjidJsonDataSource
import com.example.muslimvn.domain.models.masjid.Masjid
import com.example.muslimvn.domain.models.masjid.MasjidType
import com.example.muslimvn.domain.repository.MasjidRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMasjidRepository @Inject constructor(
    private val dataSource: MasjidJsonDataSource
) : MasjidRepository {

    private val cacheMutex = Mutex()
    private val provinceCache = mutableMapOf<String, List<Masjid>>()

    override suspend fun getMasjids(provinceId: String): List<Masjid> =
        cacheMutex.withLock {
            provinceCache.getOrPut(provinceId) {
                dataSource.loadProvince(provinceId)?.masjids.orEmpty()
            }
        }

    override suspend fun getMasjidById(id: String): Masjid? {
        provinceCache.values
            .asSequence()
            .flatten()
            .firstOrNull { it.id == id }
            ?.let { return it }

        return searchableProvinceIds().firstNotNullOfOrNull { provinceId ->
            getMasjids(provinceId).firstOrNull { it.id == id }
        }
    }

    override suspend fun getMasjidsByType(
        provinceId: String,
        type: MasjidType
    ): List<Masjid> =
        getMasjids(provinceId).filter { it.type == type }

    private suspend fun searchableProvinceIds(): List<String> {
        val assetProvinceIds = dataSource.getAvailableProvinceIds()
        return (provinceCache.keys + assetProvinceIds + DEFAULT_PROVINCE_IDS).distinct()
    }

    private companion object {
        val DEFAULT_PROVINCE_IDS = listOf("ho-chi-minh")
    }
}
