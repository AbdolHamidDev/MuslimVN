package com.example.muslimvn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE podcast_episodes ADD COLUMN lastPlayedAt INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE podcast_episodes ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
    }
}

@Database(
    entities = [ScholarEntity::class, PodcastEpisodeEntity::class],
    version = 7,
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
