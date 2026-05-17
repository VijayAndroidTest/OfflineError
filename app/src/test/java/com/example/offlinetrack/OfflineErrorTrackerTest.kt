package com.example.offlinetrack

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.Thread.UncaughtExceptionHandler

// We add RobolectricTestRunner here so we can safely construct a real Context on the JVM without an emulator
@RunWith(RobolectricTestRunner::class)
class OfflineErrorTrackerTest {

    // A lightweight Mock DAO implementation matching your precise interface signatures
    private class FakeErrorDao : LocalErrorDao {
        val capturedEntities = mutableListOf<LocalErrorEntity>()

        // FIXED: Removed 'suspend' to match your actual DAO function line signature
        override fun insertError(error: LocalErrorEntity): Long {
            capturedEntities.add(error)
            return capturedEntities.size.toLong()
        }

        override fun getAllErrorsFlow() = kotlinx.coroutines.flow.flowOf(capturedEntities)

        override fun clearAllErrors() {
            capturedEntities.clear()
        }
    }

    @Test
    fun trackerCorrectlyInterceptsExceptionAndParsesMetadata() = runTest {
        val fakeDao = FakeErrorDao()
        var systemHandlerWasInvoked = false

        val fakeSystemHandler = UncaughtExceptionHandler { _, _ ->
            systemHandlerWasInvoked = true
        }

        // FIXED: Fetch a valid sandbox context to fulfill the non-null requirement
        val context = ApplicationProvider.getApplicationContext<Context>()

        val tracker = OfflineErrorTracker(
            context = context,
            errorDao = fakeDao,
            defaultHandler = fakeSystemHandler
        )

        val targetThread = Thread.currentThread()
        val dummyException = RuntimeException("Crash triggered by unit test scenario")

        // Trigger the interception hook manually
        tracker.uncaughtException(targetThread, dummyException)

        // Verify the details were extracted cleanly
        assertEquals(1, fakeDao.capturedEntities.size)
        val capturedError = fakeDao.capturedEntities.first()

        assertEquals("Crash triggered by unit test scenario", capturedError.exceptionMessage)
        assertEquals(targetThread.name, capturedError.threadName)
        assertTrue(capturedError.stackTrace.contains("trackerCorrectlyInterceptsExceptionAndParsesMetadata"))
        assertTrue(systemHandlerWasInvoked)
    }
}