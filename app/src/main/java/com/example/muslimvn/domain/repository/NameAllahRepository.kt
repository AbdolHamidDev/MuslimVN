package com.example.muslimvn.domain.repository

import com.example.muslimvn.domain.models.AllahName

/**
 * Kho dữ liệu 99 Danh Xưng của Allah (Asmaul Husna), đọc từ asset cục bộ
 * nên hoạt động hoàn toàn offline.
 */
interface NameAllahRepository {

    /**
     * Trả về đầy đủ 99 danh xưng theo đúng thứ tự trong file JSON.
     * Dữ liệu được đọc từ assets đúng MỘT lần rồi giữ trong bộ nhớ đệm;
     * các lần gọi sau trả kết quả tức thì.
     */
    suspend fun getAllNames(): List<AllahName>
}
