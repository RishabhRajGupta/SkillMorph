
package com.example.skillmorph.di

import android.content.Context
import androidx.room.Room
import com.example.skillmorph.data.local.SkillMorphDao
import com.example.skillmorph.data.local.SkillMorphDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // This makes the dependencies available throughout the app's lifecycle.
object DatabaseModule {

    @Provides
    @Singleton // Ensures that only one instance of the database is created.
    fun provideDatabase(
        @ApplicationContext context: Context
    ): SkillMorphDatabase {
        return Room.databaseBuilder(
            context,
            SkillMorphDatabase::class.java,
            SkillMorphDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton // Ensures that only one instance of the DAO is created.
    fun provideDao(database: SkillMorphDatabase): SkillMorphDao {
        return database.skillMorphDao()
    }
}
