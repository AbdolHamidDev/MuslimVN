package com.example.muslimvn.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HijriDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QuranDataStore

private val Context.hijriDataStore: DataStore<Preferences> by preferencesDataStore(name = "hijri_preferences")
private val Context.quranDataStore: DataStore<Preferences> by preferencesDataStore(name = "quran_settings")

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    @HijriDataStore
    fun provideHijriDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.hijriDataStore

    @Provides
    @Singleton
    @QuranDataStore
    fun provideQuranDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.quranDataStore
}
