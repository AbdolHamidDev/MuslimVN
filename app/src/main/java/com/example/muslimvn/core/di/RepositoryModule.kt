package com.example.muslimvn.core.di

import com.example.muslimvn.data.repository.HijriCalendarRepositoryImpl
import com.example.muslimvn.data.repository.LocationRepositoryImpl
import com.example.muslimvn.data.repository.NameAllahRepositoryImpl
import com.example.muslimvn.data.repository.PodcastRepositoryImpl
import com.example.muslimvn.data.repository.PrayerRepositoryImpl
import com.example.muslimvn.data.repository.QuranRepositoryImpl
import com.example.muslimvn.domain.repository.HijriCalendarRepository
import com.example.muslimvn.domain.repository.LocationRepository
import com.example.muslimvn.domain.repository.NameAllahRepository
import com.example.muslimvn.domain.repository.PodcastRepository
import com.example.muslimvn.domain.repository.PrayerRepository
import com.example.muslimvn.domain.repository.QuranRepository
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
}
