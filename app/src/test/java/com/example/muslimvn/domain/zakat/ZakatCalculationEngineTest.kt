package com.example.muslimvn.domain.zakat

import com.example.muslimvn.domain.models.zakat.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ZakatCalculationEngineTest {

    private val goldPrice = BigDecimal("2000000") // 2M VND/g
    private val silverPrice = BigDecimal("20000") // 20k VND/g
    private val ruleSet = ZakatRuleSet() // Default: Gold Nisab (85g)

    private val engine = ZakatCalculationEngine(ruleSet, goldPrice, silverPrice)

    @Test
    fun `test below nisab returns zero zakat`() {
        val assets = listOf(
            ZakatAsset("Cash", BigDecimal("100000000"), AssetCategory.MONEY) // 100M
        )
        // Nisab = 85 * 2M = 170M
        
        val result = engine.calculate(assets, emptyList())
        
        assertFalse(result.isEligible)
        assertEquals(BigDecimal.ZERO, result.zakatDue)
    }

    @Test
    fun `test exactly nisab returns 2-5 percent zakat`() {
        val assets = listOf(
            ZakatAsset("Cash", BigDecimal("170000000"), AssetCategory.MONEY)
        )
        
        val result = engine.calculate(assets, emptyList())
        
        assertTrue(result.isEligible)
        // 170M * 0.025 = 4.25M
        assertEquals(BigDecimal("4250000"), result.zakatDue)
    }

    @Test
    fun `test above nisab with liabilities`() {
        val assets = listOf(
            ZakatAsset("Cash", BigDecimal("200000000"), AssetCategory.MONEY)
        )
        val liabilities = listOf(
            ZakatLiability("Debt", BigDecimal("50000000"), true) // 50M
        )
        // Net = 150M < 170M (Nisab)
        
        val result = engine.calculate(assets, liabilities)
        
        assertFalse(result.isEligible)
        assertEquals(BigDecimal.ZERO, result.zakatDue)
    }

    @Test
    fun `test silver nisab method`() {
        val silverRuleSet = ZakatRuleSet(nisabMethod = NisabMethod.SILVER)
        val silverEngine = ZakatCalculationEngine(silverRuleSet, goldPrice, silverPrice)
        
        // Silver Nisab = 595 * 20k = 11.9M
        val assets = listOf(
            ZakatAsset("Cash", BigDecimal("15000000"), AssetCategory.MONEY)
        )
        
        val result = silverEngine.calculate(assets, emptyList())
        
        assertTrue(result.isEligible)
        // 15M * 0.025 = 375k
        assertEquals(BigDecimal("375000"), result.zakatDue)
    }

    @Test
    fun `test small decimals in gold weight`() {
        // Gold Nisab = 85g. User has 84.9g -> 0 Zakat. User has 85.1g -> Zakat.
        val assets = listOf(
            ZakatAsset("Gold", BigDecimal("84.9").multiply(goldPrice), AssetCategory.GOLD_SILVER)
        )
        val result = engine.calculate(assets, emptyList())
        assertFalse(result.isEligible)
        
        val assets2 = listOf(
            ZakatAsset("Gold", BigDecimal("85.1").multiply(goldPrice), AssetCategory.GOLD_SILVER)
        )
        val result2 = engine.calculate(assets2, emptyList())
        assertTrue(result2.isEligible)
    }
}
