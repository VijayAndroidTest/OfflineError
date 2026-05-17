package com.example.offlinetrack


import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocalErrorDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var errorDao: LocalErrorDao

    @Before
    fun createDb() {
        // Creates a transient, isolated database instance in system memory
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        errorDao = database.localErrorDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertAndReadErrorLogsUsingFlow() = runTest {
        val testEntity = LocalErrorEntity(
            id = 1,
            timestamp = 1715950000000L,
            threadName = "Main",
            exceptionMessage = "RuntimeException: Test Exception",
            stackTrace = "at MainActivity.onCreate(MainActivity.kt:15)"
        )

        errorDao.insertError(testEntity)

        // Turbine taps directly into the Room Flow channel emission sequence
        errorDao.getAllErrorsFlow().test {
            val itemList = awaitItem()
            assertEquals(1, itemList.size)
            assertEquals("RuntimeException: Test Exception", itemList[0].exceptionMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun clearDatabaseTriggersEmptyListEmission() = runTest {
        val testEntity = LocalErrorEntity(
            id = 5,
            timestamp = 1715950000000L,
            threadName = "DefaultDispatcher",
            exceptionMessage = "IllegalStateException",
            stackTrace = "Unknown Trace"
        )

        errorDao.insertError(testEntity)
        errorDao.clearAllErrors()

        errorDao.getAllErrorsFlow().test {
            val itemList = awaitItem()
            assertTrue(itemList.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }
}