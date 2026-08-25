package com.example.muslimvn.domain.models.zakat

import java.math.BigDecimal

data class ZakatRuleSet(
    val nisabMethod: NisabMethod = NisabMethod.GOLD,
    val zakatRate: BigDecimal = BigDecimal("0.025"), // 2.5% for Lunar year
    val goldNisabGrams: BigDecimal = BigDecimal("85"),
    val silverNisabGrams: BigDecimal = BigDecimal("595"),
    val manualNisabVnd: BigDecimal? = null
)

enum class NisabMethod {
    GOLD, SILVER, MANUAL
}

data class ZakatAsset(
    val name: String,
    val valueVnd: BigDecimal,
    val category: AssetCategory
)

enum class AssetCategory {
    MONEY, GOLD_SILVER, INVESTMENT, BUSINESS
}

data class ZakatLiability(
    val name: String,
    val valueVnd: BigDecimal,
    val isDeductible: Boolean = true
)

data class ZakatResult(
    val totalZakatableAssets: BigDecimal,
    val totalDeductibleLiabilities: BigDecimal,
    val netZakatableWealth: BigDecimal,
    val nisabThreshold: BigDecimal,
    val zakatRate: BigDecimal,
    val zakatDue: BigDecimal,
    val isEligible: Boolean
)
