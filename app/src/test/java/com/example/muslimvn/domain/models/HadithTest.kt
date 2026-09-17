package com.example.muslimvn.domain.models

import org.junit.Assert.assertEquals
import org.junit.Test

class HadithTest {

    @Test
    fun cleanCurlyBraces_removesBracesAndNormalizesSpaces() {
        val input = "Thiên Sứ của Allah {sallallahu 'alaihi wa sallam} đã nói: {Thật vậy, mọi việc làm...}"
        val expected = "Thiên Sứ của Allah sallallahu 'alaihi wa sallam đã nói: Thật vậy, mọi việc làm..."
        assertEquals(expected, input.cleanCurlyBraces())
    }

    @Test
    fun cleanCurlyBraces_handlesEmptyBraces() {
        val input = "Đây là câu thử nghiệm {} không có nội dung."
        val expected = "Đây là câu thử nghiệm không có nội dung."
        assertEquals(expected, input.cleanCurlyBraces())
    }

    @Test
    fun cleanCurlyBraces_preservesLineBreaks() {
        val input = "Nabi {ﷺ} phán:\n\n{Sẽ không vào Thiên Đàng...}"
        val expected = "Nabi ﷺ phán:\n\nSẽ không vào Thiên Đàng..."
        assertEquals(expected, input.cleanCurlyBraces())
    }
}
