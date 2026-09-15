package com.example.muslimvn.core.di

import android.content.Context
import androidx.room.Room
import com.example.muslimvn.data.local.AzkarDatabase
import com.example.muslimvn.data.local.HijriCalendarDatabase
import com.example.muslimvn.data.local.PodcastDatabase
import com.example.muslimvn.data.local.QuranDatabase
import com.example.muslimvn.data.local.TrackerDatabase
import com.example.muslimvn.data.local.ZakatDatabase
import com.example.muslimvn.data.local.dao.AzkarDao
import com.example.muslimvn.data.local.dao.HijriCalendarDao
import com.example.muslimvn.data.local.dao.PodcastEpisodeDao
import com.example.muslimvn.data.local.dao.QuranDao
import com.example.muslimvn.data.local.dao.ScholarDao
import com.example.muslimvn.data.local.dao.TrackerDao
import com.example.muslimvn.data.local.dao.ZakatDao
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @androidx.media3.common.util.UnstableApi
    @Provides
    @Singleton
    fun provideMediaCache(
        @ApplicationContext context: Context
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024) // 200MB cache
        val databaseProvider: DatabaseProvider = StandaloneDatabaseProvider(context)
        return SimpleCache(cacheDir, evictor, databaseProvider)
    }

    @Provides
    @Singleton
    fun provideTrackerDatabase(
        @ApplicationContext context: Context
    ): TrackerDatabase {
        return Room.databaseBuilder(
            context,
            TrackerDatabase::class.java,
            TrackerDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideTrackerDao(database: TrackerDatabase): TrackerDao {
        return database.trackerDao
    }

    @Provides
    @Singleton
    fun provideQuranDatabase(
        @ApplicationContext context: Context
    ): QuranDatabase {
        return Room.databaseBuilder(
            context,
            QuranDatabase::class.java,
            QuranDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
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
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideScholarDao(database: PodcastDatabase): ScholarDao = database.scholarDao

    @Provides
    fun providePodcastEpisodeDao(database: PodcastDatabase): PodcastEpisodeDao =
        database.podcastEpisodeDao

    @Provides
    @Singleton
    fun provideZakatDatabase(
        @ApplicationContext context: Context
    ): ZakatDatabase {
        return Room.databaseBuilder(
            context,
            ZakatDatabase::class.java,
            ZakatDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideZakatDao(database: ZakatDatabase): ZakatDao {
        return database.zakatDao
    }

    @Provides
    @Singleton
    fun provideAzkarDatabase(
        @ApplicationContext context: Context
    ): AzkarDatabase {
        return Room.databaseBuilder(
            context,
            AzkarDatabase::class.java,
            AzkarDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideAzkarDao(database: AzkarDatabase): AzkarDao {
        return database.azkarDao
    }

    @Provides
    @Singleton
    fun provideDownloadedVideoDatabase(
        @ApplicationContext context: Context
    ): com.example.muslimvn.data.local.DownloadedVideoDatabase {
        return androidx.room.Room.databaseBuilder(
            context,
            com.example.muslimvn.data.local.DownloadedVideoDatabase::class.java,
            com.example.muslimvn.data.local.DownloadedVideoDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideDownloadedVideoDao(database: com.example.muslimvn.data.local.DownloadedVideoDatabase): com.example.muslimvn.data.local.dao.DownloadedVideoDao {
        return database.downloadedVideoDao
    }
}
