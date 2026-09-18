package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.masjid.Masjid
import com.example.muslimvn.domain.models.masjid.MasjidType

interface MasjidRepository {
    suspend fun getMasjids(provinceId: String): List<Masjid>

    suspend fun getMasjidById(id: String): Masjid?

    suspend fun getMasjidsByType(
        provinceId: String,
        type: MasjidType
    ): List<Masjid>
}
