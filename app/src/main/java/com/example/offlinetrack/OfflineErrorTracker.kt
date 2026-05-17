package com.example.offlinetrack

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class OfflineErrorTracker(
    private val context: Context,
    private val errorDao: LocalErrorDao,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))
        val stackTraceString = stringWriter.toString()

        // 1. Run database insertion on a separate thread to avoid Room's MainThread violation
        val dbThread = Thread {
            try {
                errorDao.insertError(
                    LocalErrorEntity(
                        timestamp = System.currentTimeMillis(),
                        exceptionMessage = exception.localizedMessage ?: "Unknown Exception",
                        stackTrace = stackTraceString,
                        threadName = thread.name
                    )
                )
                Log.d("OfflineTracker", "Crash log written to Room successfully.")
            } catch (e: Exception) {
                Log.e("OfflineTracker", "Failed to save crash log to Room", e)
            }
        }

        dbThread.start()
        try {
            dbThread.join() // Force execution block to complete before moving to restart setup
        } catch (e: InterruptedException) {
            Log.e("OfflineTracker", "DB thread interrupted", e)
        }

        // 2. Set up the Automatic Relaunch Intent
        val restartIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Schedule the AlarmManager to wake the app up in 100 milliseconds
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.set(
                AlarmManager.RTC,
                System.currentTimeMillis() + 100,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e("OfflineTracker", "Failed to schedule app auto-restart alarm", e)
        }

        // 4. Clean up the current system state and kill the dead process cleanly
        defaultHandler?.uncaughtException(thread, exception) ?: exitProcess(2)
    }
}