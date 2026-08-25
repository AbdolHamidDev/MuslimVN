package com.example.muslimvn.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.time.LocalDate

@Entity(tableName = "zakat_history")
data class ZakatHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: LocalDate,
    val totalAssets: BigDecimal,
    val totalLiabilities: BigDecimal,
    val netWealth: BigDecimal,
    val zakatDue: BigDecimal,
    val goldPrice: BigDecimal,
    val silverPrice: BigDecimal
)
