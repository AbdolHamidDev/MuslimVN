package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.muslimvn.data.local.dao.PodcastEpisodeDao
import com.example.muslimvn.data.local.dao.ScholarDao
import com.example.muslimvn.data.local.entities.PodcastEpisodeEntity
import com.example.muslimvn.data.local.entities.ScholarEntity

/** Converter cho trường tags: List<String> <-> chuỗi CSV đơn giản. */
class PodcastConverters {

    @TypeConverter
    fun tagsToString(tags: List<String>): String = tags.joinToString(",")

    @TypeConverter
    fun stringToTags(csv: String): List<String> =
        if (csv.isBlank()) emptyList() else csv.split(',')
}

@Database(
    entities = [ScholarEntity::class, PodcastEpisodeEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(PodcastConverters::class)
abstract class PodcastDatabase : RoomDatabase() {
    abstract val scholarDao: ScholarDao
    abstract val podcastEpisodeDao: PodcastEpisodeDao

    companion object {
        const val DATABASE_NAME = "podcast_db"
    }
}
