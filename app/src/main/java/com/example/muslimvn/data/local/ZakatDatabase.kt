package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.muslimvn.data.local.dao.ZakatDao
import com.example.muslimvn.data.local.entities.ZakatHistoryEntity
import java.math.BigDecimal
import java.time.LocalDate

class ZakatConverters {
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? = value?.toString()

    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? = value?.let { BigDecimal(it) }

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }
}

@Database(entities = [ZakatHistoryEntity::class], version = 1, exportSchema = false)
@TypeConverters(ZakatConverters::class)
abstract class ZakatDatabase : RoomDatabase() {
    abstract val zakatDao: ZakatDao

    companion object {
        const val DATABASE_NAME = "zakat_db"
    }
}
