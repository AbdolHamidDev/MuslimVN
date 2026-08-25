package com.example.muslimvn.core.utils

import java.math.BigDecimal
import java.math.RoundingMode

object GoldUtils {
    // 1 lượng = 10 chỉ
    // 1 chỉ = 3.75g
    // 1 lượng = 37.5g
    private val GRAMS_PER_CHI = BigDecimal("3.75")
    private val CHI_PER_LUONG = BigDecimal("10")
    private val GRAMS_PER_LUONG = BigDecimal("37.5")

    fun chiToGrams(chi: BigDecimal): BigDecimal {
        return chi.multiply(GRAMS_PER_CHI)
    }

    fun luongToGrams(luong: BigDecimal): BigDecimal {
        return luong.multiply(GRAMS_PER_LUONG)
    }

    fun gramsToChi(grams: BigDecimal): BigDecimal {
        return grams.divide(GRAMS_PER_CHI, 4, RoundingMode.HALF_UP)
    }

    fun gramsToLuong(grams: BigDecimal): BigDecimal {
        return grams.divide(GRAMS_PER_LUONG, 4, RoundingMode.HALF_UP)
    }
}
