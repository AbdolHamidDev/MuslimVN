package com.example.muslimvn.core.di

import com.example.muslimvn.BuildConfig
import com.example.muslimvn.data.remote.AladhanApiService
import com.example.muslimvn.data.remote.QuranApiService
import com.example.muslimvn.data.remote.HadeethEncApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AladhanRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class QuranRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IslamHouseRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HadeethEncRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val ALADHAN_BASE_URL = "https://api.aladhan.com/"
    private const val QURAN_COM_BASE_URL = "https://api.quran.com/api/v4/"
    private const val ISLAMHOUSE_BASE_URL = "https://api3.islamhouse.com/"
    private const val HADEETH_ENC_BASE_URL = "https://hadeethenc.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @AladhanRetrofit
    fun provideAladhanRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(ALADHAN_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @QuranRetrofit
    fun provideQuranRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(QURAN_COM_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @IslamHouseRetrofit
    fun provideIslamHouseRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(ISLAMHOUSE_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    @HadeethEncRetrofit
    fun provideHadeethEncRetrofit(client: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(HADEETH_ENC_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideAladhanApiService(@AladhanRetrofit retrofit: Retrofit): AladhanApiService =
        retrofit.create(AladhanApiService::class.java)

    @Provides
    @Singleton
    fun provideQuranApiService(@QuranRetrofit retrofit: Retrofit): QuranApiService =
        retrofit.create(QuranApiService::class.java)

    @Provides
    @Singleton
    fun provideIslamHouseApiService(@IslamHouseRetrofit retrofit: Retrofit): com.example.muslimvn.data.remote.IslamHouseApiService =
        retrofit.create(com.example.muslimvn.data.remote.IslamHouseApiService::class.java)

    @Provides
    @Singleton
    fun provideHadeethEncApiService(@HadeethEncRetrofit retrofit: Retrofit): HadeethEncApiService =
        retrofit.create(HadeethEncApiService::class.java)
}
