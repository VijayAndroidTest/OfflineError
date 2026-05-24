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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.offlinetrack.ui.theme.OfflineTrackTheme
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.Firebase
import com.google.firebase.appdistribution.appDistribution
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppScreenState {
    SplashCanvas,
    Dashboard
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val TAG = "SplashDebugLog"

    @Inject
    lateinit var errorDao: LocalErrorDao

    // 🛠️ FIX: Using 'by remember' inside the composition layer is ideal,
    // but since these control the Activity level flow, we declare them cleanly as standard observables:
    private var isAppReady by mutableStateOf(true)
    private val currentScreenState = mutableStateOf(AppScreenState.SplashCanvas)

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        Log.d(TAG, "Permission dialog callback triggered. isGranted = $isGranted")
        if (isGranted) {
            Toast.makeText(this, getString(R.string.toast_permission_granted), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_LONG).show()
        }
        checkForUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "─── NEW COLD BOOT START ───")

        // Tells Android to complete the native window setup
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Dismisses the system launcher icon overlay immediately since isAppReady is true
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        setContent {
            Log.d(TAG, "3. setContent composition container starting setup")

            LaunchedEffect(Unit) {
                Log.d(TAG, "4. Compose rendered -> Showing custom splash screen layout...")

                // FIX: Holds your custom splash design on the screen for exactly 3 seconds
                delay(3000)

                Log.d(TAG, "5. Custom delay complete. Executing initialization tasks...")
                handleStartupPermissions()
            }

            val isShowingSplash = currentScreenState.value == AppScreenState.SplashCanvas
            Log.d(TAG, "6. Theme wrapping layout engine profile match. isShowingSplash = $isShowingSplash")

            OfflineTrackTheme(dynamicColor = !isShowingSplash) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    // FIX: Dynamically match the splash design's background color during the transition
                    color = if (isShowingSplash) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Crossfade(
                        targetState = currentScreenState.value,
                        animationSpec = tween(durationMillis = 600), // Clean transition fade
                        label = "ScreenTransition"
                    ) { screen ->
                        when (screen) {
                            AppScreenState.SplashCanvas -> {
                                Log.d(TAG, "-> Crossfade: Displaying custom splash canvas")
                                MyCustomSplashDesign()
                            }
                            AppScreenState.Dashboard -> {
                                Log.d(TAG, "-> Crossfade: Navigating to Dashboard view")
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
            Log.d(TAG, "Execution Pass: Requesting system runtime permission layout handles.")
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            checkForUpdates()
        }
    }

    private fun checkForUpdates() {
        Log.d(TAG, "Execution Pass: Checking Firebase app targets...")
        Firebase.appDistribution.updateIfNewReleaseAvailable()
            .addOnCompleteListener { task ->
                Log.d(TAG, "Execution Pass: Task loop finalized.")
                navigateToDashboard()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Execution Failure: Task skipped.", exception)
                navigateToDashboard()
            }
    }

    private fun navigateToDashboard() {
        Log.d(TAG, "8. Swapping final view layout profiles to Dashboard state targets.")
        currentScreenState.value = AppScreenState.Dashboard
    }
}

// Junior Dashboard UI Layout Draft
