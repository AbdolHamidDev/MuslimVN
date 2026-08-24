package com.example.muslimvn.core.di

import android.content.Context
import androidx.room.Room
import com.example.muslimvn.data.local.HijriCalendarDatabase
import com.example.muslimvn.data.local.PodcastDatabase
import com.example.muslimvn.data.local.QuranDatabase
import com.example.muslimvn.data.local.dao.HijriCalendarDao
import com.example.muslimvn.data.local.dao.PodcastEpisodeDao
import com.example.muslimvn.data.local.dao.QuranDao
import com.example.muslimvn.data.local.dao.ScholarDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideQuranDatabase(
        @ApplicationContext context: Context
    ): QuranDatabase {
        return Room.databaseBuilder(
            context,
            QuranDatabase::class.java,
            QuranDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideQuranDao(database: QuranDatabase): QuranDao {
        return database.quranDao
    }

    @Provides
    @Singleton
    fun provideHijriCalendarDatabase(
        @ApplicationContext context: Context
    ): HijriCalendarDatabase {
        return Room.databaseBuilder(
            context,
            HijriCalendarDatabase::class.java,
            HijriCalendarDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideHijriCalendarDao(database: HijriCalendarDatabase): HijriCalendarDao {
        return database.hijriCalendarDao
    }

    @Provides
    @Singleton
    fun providePodcastDatabase(
        @ApplicationContext context: Context
    ): PodcastDatabase {
        return Room.databaseBuilder(
            context,
            PodcastDatabase::class.java,
            PodcastDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideScholarDao(database: PodcastDatabase): ScholarDao = database.scholarDao

    @Provides
    fun providePodcastEpisodeDao(database: PodcastDatabase): PodcastEpisodeDao =
        database.podcastEpisodeDao
}
