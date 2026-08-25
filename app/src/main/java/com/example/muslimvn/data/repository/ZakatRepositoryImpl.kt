package com.example.muslimvn.data.repository

import com.example.muslimvn.data.local.dao.ZakatDao
import com.example.muslimvn.data.local.entities.ZakatHistoryEntity
import com.example.muslimvn.domain.models.zakat.ZakatHistory
import com.example.muslimvn.domain.repository.ZakatRepository
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class ZakatRepositoryImpl @Inject constructor(
    private val zakatDao: ZakatDao
) : ZakatRepository {
    // Default prices for Vietnam (Mock data, should be replaced with real API later)
    // Gold SJC ~ 80,000,000 VND / luong = 2,133,333 VND / gram
    override fun getGoldPrice(): Flow<BigDecimal> = flowOf(BigDecimal("2133000"))
    
    // Silver ~ 25,000 VND / gram
    override fun getSilverPrice(): Flow<BigDecimal> = flowOf(BigDecimal("25000"))

    override fun getExchangeRate(from: String, to: String): Flow<BigDecimal> {
        return flowOf(
            when (from) {
                "USD" -> BigDecimal("25000")
                "EUR" -> BigDecimal("27000")
                "SGD" -> BigDecimal("18500")
                "MYR" -> BigDecimal("5500")
                else -> BigDecimal.ONE
            }
        )
    }

    override fun getAllHistory(): Flow<List<ZakatHistory>> {
        return zakatDao.getAllHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getLatestRecord(): ZakatHistory? {
        return zakatDao.getLatestRecord()?.toDomain()
    }

    override suspend fun saveRecord(record: ZakatHistory) {
        zakatDao.insertRecord(record.toEntity())
    }

    private fun ZakatHistoryEntity.toDomain() = ZakatHistory(
        id = id,
        date = date,
        totalAssets = totalAssets,
        totalLiabilities = totalLiabilities,
        netWealth = netWealth,
        zakatDue = zakatDue,
        goldPrice = goldPrice,
        silverPrice = silverPrice
    )

    private fun ZakatHistory.toEntity() = ZakatHistoryEntity(
        id = id,
        date = date,
        totalAssets = totalAssets,
        totalLiabilities = totalLiabilities,
        netWealth = netWealth,
        zakatDue = zakatDue,
        goldPrice = goldPrice,
        silverPrice = silverPrice
    )
}
