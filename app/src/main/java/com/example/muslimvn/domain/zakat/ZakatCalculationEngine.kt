package com.example.muslimvn.domain.zakat

import com.example.muslimvn.domain.models.zakat.*
import java.math.BigDecimal
import java.math.RoundingMode

class ZakatCalculationEngine(
    private val ruleSet: ZakatRuleSet,
    private val goldPriceVndPerGram: BigDecimal,
    private val silverPriceVndPerGram: BigDecimal
) {
    fun calculate(
        assets: List<ZakatAsset>,
        liabilities: List<ZakatLiability>
    ): ZakatResult {
        val totalAssets = assets.sumOf { it.valueVnd }
        val totalLiabilities = liabilities
            .filter { it.isDeductible }
            .sumOf { it.valueVnd }

        val netWealth = (totalAssets - totalLiabilities).coerceAtLeast(BigDecimal.ZERO)
        
        val nisabThreshold = when (ruleSet.nisabMethod) {
            NisabMethod.GOLD -> ruleSet.goldNisabGrams.multiply(goldPriceVndPerGram)
            NisabMethod.SILVER -> ruleSet.silverNisabGrams.multiply(silverPriceVndPerGram)
            NisabMethod.MANUAL -> ruleSet.manualNisabVnd ?: BigDecimal.ZERO
        }

        val isEligible = netWealth >= nisabThreshold
        val zakatDue = if (isEligible) {
            netWealth.multiply(ruleSet.zakatRate).setScale(0, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }

        return ZakatResult(
            totalZakatableAssets = totalAssets,
            totalDeductibleLiabilities = totalLiabilities,
            netZakatableWealth = netWealth,
            nisabThreshold = nisabThreshold,
            zakatRate = ruleSet.zakatRate,
            zakatDue = zakatDue,
            isEligible = isEligible
        )
    }

    private fun Iterable<ZakatAsset>.sumOf(selector: (ZakatAsset) -> BigDecimal): BigDecimal {
        var sum = BigDecimal.ZERO
        for (element in this) {
            sum = sum.add(selector(element))
        }
        return sum
    }

    @JvmName("sumOfLiability")
    private fun Iterable<ZakatLiability>.sumOf(selector: (ZakatLiability) -> BigDecimal): BigDecimal {
        var sum = BigDecimal.ZERO
        for (element in this) {
            sum = sum.add(selector(element))
        }
        return sum
    }
}
