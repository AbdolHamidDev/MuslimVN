package com.example.muslimvn.core.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val vndLocale = Locale("vi", "VN")
    private val vndFormat = NumberFormat.getCurrencyInstance(vndLocale).apply {
        maximumFractionDigits = 0
    }
    
    private val dotFormat = (NumberFormat.getInstance(vndLocale) as DecimalFormat).apply {
        val symbols = DecimalFormatSymbols(vndLocale)
        symbols.groupingSeparator = '.'
        decimalFormatSymbols = symbols
    }

    fun formatVnd(amount: BigDecimal): String {
        return vndFormat.format(amount)
    }

    fun formatVnd(amount: Double): String {
        return vndFormat.format(BigDecimal.valueOf(amount))
    }

    fun formatWithDots(amount: BigDecimal): String {
        return dotFormat.format(amount)
    }
}

fun BigDecimal.toVndString(): String = CurrencyUtils.formatVnd(this)
fun BigDecimal.toDotString(): String = CurrencyUtils.formatWithDots(this)
