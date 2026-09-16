package com.example.muslimvn.core.di

import com.example.muslimvn.data.repository.*
import com.example.muslimvn.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(
        locationRepositoryImpl: LocationRepositoryImpl
    ): LocationRepository

    @Binds
    @Singleton
    abstract fun bindPrayerRepository(
        prayerRepositoryImpl: PrayerRepositoryImpl
    ): PrayerRepository

    @Binds
    @Singleton
    abstract fun bindQuranRepository(
        quranRepositoryImpl: QuranRepositoryImpl
    ): QuranRepository

    @Binds
    @Singleton
    abstract fun bindHijriCalendarRepository(
        hijriCalendarRepositoryImpl: HijriCalendarRepositoryImpl
    ): HijriCalendarRepository

    @Binds
    @Singleton
    abstract fun bindNameAllahRepository(
        nameAllahRepositoryImpl: NameAllahRepositoryImpl
    ): NameAllahRepository

    @Binds
    @Singleton
    abstract fun bindPodcastRepository(
        podcastRepositoryImpl: PodcastRepositoryImpl
    ): PodcastRepository

    @Binds
    @Singleton
    abstract fun bindZakatRepository(
        zakatRepositoryImpl: ZakatRepositoryImpl
    ): ZakatRepository
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAzkarRepository(
        azkarRepositoryImpl: AzkarRepositoryImpl
    ): AzkarRepository

    @Binds
    @Singleton
    abstract fun bindTrackerRepository(
        trackerRepositoryImpl: TrackerRepositoryImpl
    ): TrackerRepository

    @Binds
    @Singleton
    abstract fun bindYoutubeRepository(
        youtubeRepositoryImpl: YoutubeRepositoryImpl
    ): YoutubeRepository

    @Binds
    @Singleton
    abstract fun bindIslamHouseRepository(
        islamHouseRepositoryImpl: IslamHouseRepositoryImpl
    ): IslamHouseRepository

    @Binds
    @Singleton
    abstract fun bindMasjidRepository(
        localMasjidRepository: LocalMasjidRepository
    ): MasjidRepository

    @Binds
    @Singleton
    abstract fun bindHadithRepository(
        hadithRepositoryImpl: HadithRepositoryImpl
    ): HadithRepository
}
