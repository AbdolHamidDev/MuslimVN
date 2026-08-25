package com.example.muslimvn.core.utils

import java.math.BigDecimal

object VietnameseNumberReader {
    private val digits = arrayOf("không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín")

    fun readNumber(amount: BigDecimal): String {
        if (amount == BigDecimal.ZERO) return "Không đồng"
        
        val value = amount.toLong().toString()
        var result = ""
        val groups = mutableListOf<String>()
        
        var tempValue = value
        while (tempValue.isNotEmpty()) {
            val len = tempValue.length
            val part = if (len >= 3) tempValue.substring(len - 3) else tempValue
            groups.add(part)
            tempValue = if (len >= 3) tempValue.substring(0, len - 3) else ""
        }

        val units = arrayOf("", "ngàn", "triệu", "tỷ", "ngàn tỷ", "triệu tỷ")
        
        for (i in groups.indices.reversed()) {
            val groupValue = groups[i].toInt()
            if (groupValue > 0) {
                val groupText = readGroup(groups[i], i == groups.size - 1)
                result += "$groupText ${units[i]} "
            }
        }

        return result.trim().replace("\\s+".toRegex(), " ").replaceFirstChar { it.uppercase() } + " đồng"
    }

    private fun readGroup(group: String, isFirstGroup: Boolean): String {
        val n = group.toInt()
        val hundreds = n / 100
        val tens = (n % 100) / 10
        val units = n % 10
        
        var res = ""
        
        // Hundreds
        if (!isFirstGroup || hundreds > 0) {
            res += "${digits[hundreds]} trăm "
        }
        
        // Tens
        if (tens > 1) {
            res += "${digits[tens]} mươi "
        } else if (tens == 1) {
            res += "mười "
        } else if (hundreds > 0 && units > 0) {
            res += "lẻ "
        }
        
        // Units
        if (units > 0) {
            if (units == 1 && tens > 1) {
                res += "mốt"
            } else if (units == 5 && tens >= 1) {
                res += "lăm"
            } else if (units == 4 && tens > 1) {
                res += "tư"
            } else {
                res += digits[units]
            }
        }
        
        return res
    }
}
