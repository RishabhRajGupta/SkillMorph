package com.example.skillmorph.di

import com.example.skillmorph.data.remote.SkillMorphApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ⚠️ REPLACE THIS WITH YOUR CURRENT PINGGY/NGROK URL
    // MUST END WITH A SLASH /
    private const val BASE_URL = "https://cathleen-unchipping-elmer.ngrok-free.dev/"

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideSkillMorphApi(retrofit: Retrofit): SkillMorphApi {
        return retrofit.create(SkillMorphApi::class.java)
    }
}