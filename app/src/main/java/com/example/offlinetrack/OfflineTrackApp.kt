package com.example.offlinetrack

import android.app.Application
import androidx.room.Room
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OfflineTrackApp : Application() {

    // 👈 DELETED: Manual properties and builders are completely gone!

    override fun onCreate() {
        super.onCreate()

        /* NOTE: For your uncaught exception handler setup, you can keep
           it manual for a moment, or clean it up nicely using entry points.
           To prevent crashing while we finish migrating, let's build a temporary bridge:
        */
        val database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "offline_track_db"
        ).build()

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val customTracker = OfflineErrorTracker(
            context = this,
            errorDao = database.localErrorDao(),
            defaultHandler = defaultHandler
        )
        Thread.setDefaultUncaughtExceptionHandler(customTracker)
    }
}