package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.zakat.ZakatHistory
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

interface ZakatRepository {
    fun getGoldPrice(): Flow<BigDecimal>
    fun getSilverPrice(): Flow<BigDecimal>
    fun getExchangeRate(from: String, to: String): Flow<BigDecimal>
    
    fun getAllHistory(): Flow<List<ZakatHistory>>
    suspend fun getLatestRecord(): ZakatHistory?
    suspend fun saveRecord(record: ZakatHistory)
}
