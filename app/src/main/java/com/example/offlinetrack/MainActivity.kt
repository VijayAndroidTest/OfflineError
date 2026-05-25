package com.example.offlinetrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.offlinetrack.ui.theme.OfflineTrackTheme
import com.google.firebase.Firebase
import com.google.firebase.appdistribution.appDistribution
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

enum class AppScreenState {
    SplashCanvas,
    Dashboard
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "SplashDebugLog"

    @Inject
    lateinit var errorDao: LocalErrorDao

    private var isAppReady by mutableStateOf(true)
    private val currentScreenState = mutableStateOf(AppScreenState.SplashCanvas)

    private var isCheckingForUpdates by mutableStateOf(true)
    private var blockErrorMessage by mutableStateOf<String?>(null)

    private val sharedPrefs by lazy { getSharedPreferences("app_dist_prefs", Context.MODE_PRIVATE) }
    private var isUpdateDownloading by mutableStateOf(false)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        checkForUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "─── NEW COLD BOOT START ───")

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        isUpdateDownloading = sharedPrefs.getBoolean("is_downloading", false)
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        setContent {
            LaunchedEffect(Unit) {
                delay(3000)
                handleStartupPermissions()
            }

            val isShowingSplash = currentScreenState.value == AppScreenState.SplashCanvas

            OfflineTrackTheme(dynamicColor = !isShowingSplash) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isShowingSplash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Crossfade(targetState = currentScreenState.value, label = "ScreenTransition") { screen ->
                        when (screen) {
                            AppScreenState.SplashCanvas -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    MyCustomSplashDesign()

                                    if (blockErrorMessage != null) {
                                        StrictBlockOverlay(
                                            title = "Access Blocked",
                                            message = blockErrorMessage!!,
                                            showRetryButton = true,
                                            onRetry = {
                                                blockErrorMessage = null
                                                isCheckingForUpdates = true
                                                checkForUpdates()
                                            }
                                        )
                                    } else if (isCheckingForUpdates) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 64.dp),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                            AppScreenState.Dashboard -> {
                                OfflineDashboard(errorDao = errorDao)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleStartupPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            checkForUpdates()
        }
    }

    private fun checkForUpdates() {
// 🌟 ALTERNATIVE FIX: Detects local debug builds using application info flags directly
//        val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
//
//        if (isDebuggable) {
//            Log.d(TAG, "🔧 LOCAL DEBUG DETECTION: Bypassing Firebase App Distribution checks to avoid loop.")
//            isCheckingForUpdates = false
//            navigateToDashboard()
//            return
//        }

        if (isUpdateDownloading) {
            Log.d(TAG, "Break Loop: Session triggered a download update task. Navigating to Dashboard.")
            sharedPrefs.edit().putBoolean("is_downloading", false).apply()
            isUpdateDownloading = false
            navigateToDashboard()
            return
        }

        Log.d(TAG, "Execution Pass: Checking Firebase App Distribution for new tester builds...")

        if (!Firebase.appDistribution.isTesterSignedIn) {
            Firebase.appDistribution.signInTester()
                .addOnSuccessListener { checkForUpdates() }
                .addOnFailureListener { e ->
                    isCheckingForUpdates = false
                    blockErrorMessage = "Authentication Required: You must log into Firebase App Distribution to verify tester validation status."
                }
            return
        }

        Firebase.appDistribution.updateIfNewReleaseAvailable()
            .addOnSuccessListener { release ->
                isCheckingForUpdates = false
                if (release != null) {
                    Log.d(TAG, "🎉 SUCCESS: New release found! Holding screen for Firebase Update UI...")
                    sharedPrefs.edit().putBoolean("is_downloading", true).apply()
                    isUpdateDownloading = true
                } else {
                    Log.d(TAG, "ℹ️ INFO: No updates available. Moving to dashboard.")
                    navigateToDashboard()
                }
            }
            .addOnFailureListener { exception ->
                isCheckingForUpdates = false
                if (exception.message?.contains("canceled") == true) {
                    if (isUpdateDownloading) {
                        Log.d(TAG, "User accepted update. System download screen active.")
                    } else {
                        blockErrorMessage = "Update Canceled: You cannot bypass mandatory distribution builds. Please update the application to continue."
                    }
                } else {
                    blockErrorMessage = "Network Failure: Unable to verify distribution version clearance profiles. Check your internet connection."
                }
            }
    }

    private fun navigateToDashboard() {
        currentScreenState.value = AppScreenState.Dashboard
    }

    @Composable
    fun StrictBlockOverlay(title: String, message: String, showRetryButton: Boolean, onRetry: () -> Unit) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)).padding(32.dp), contentAlignment = Alignment.Center) {
            Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = message, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    if (showRetryButton) {
                        Button(onClick = onRetry) { Text(text = "Retry System Check") }
                    }
                }
            }
        }
    }
}