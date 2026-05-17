package com.example.offlinetrack

import android.content.Context
import androidx.room.Room
import com.example.offlinetrack.AppDatabase
import com.example.offlinetrack.LocalErrorDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "offline_track_db"
        ).build()
    }

    @Provides
    fun provideLocalErrorDao(database: AppDatabase): LocalErrorDao {
        return database.localErrorDao()
    }
}