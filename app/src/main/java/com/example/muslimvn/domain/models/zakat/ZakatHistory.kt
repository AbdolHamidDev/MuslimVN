package com.example.muslimvn.domain.models.zakat

import java.math.BigDecimal
import java.time.LocalDate

data class ZakatHistory(
    val id: Int = 0,
    val date: LocalDate,
    val totalAssets: BigDecimal,
    val totalLiabilities: BigDecimal,
    val netWealth: BigDecimal,
    val zakatDue: BigDecimal,
    val goldPrice: BigDecimal,
    val silverPrice: BigDecimal
)
