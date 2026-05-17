package com.example.offlinetrack

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var errorDao: LocalErrorDao



    // 👈 Launcher registration to handle user choice when permission prompt pops up
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Notification permission allowed! LeakCanary alerts active.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission denied. LeakCanary can only log to Logcat.", Toast.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


// 👈 Trigger the Runtime Permission Dialogue for Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }


        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OfflineDashboard(errorDao = errorDao)
                }
            }
        }
    }
}

// Global Helper function to trigger the standard Android Share Intent
fun shareCrashLogs(context: Context, logText: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, logText)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Export Offline Diagnostics")
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineDashboard(errorDao: LocalErrorDao) {
    val loggedErrors by errorDao.getAllErrorsFlow().collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current // Grab instance context safely inside Compose tree

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Telemetry Logger", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(text = "Diagnostic Controls", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { throw RuntimeException("Main Thread Crash triggered by user action click!") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Crash Main UI")
                }

                Button(
                    onClick = {
                        CoroutineScope(Dispatchers.IO).launch {
                            throw IllegalStateException("Background worker coroutine task processing error!")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Crash Worker Thread")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // NEW BUTTON: Formats the Room data array and passes it off to the OS Share Tray
            Button(
                onClick = {
                    if (loggedErrors.isEmpty()) {
                        Toast.makeText(context, "No error logs found to export!", Toast.LENGTH_SHORT).show()
                    } else {
                        val simpleDateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                        val logDump = loggedErrors.joinToString(separator = "\n\n====================\n\n") { error ->
                            "CRASH OCCURRED AT: ${simpleDateFormat.format(Date(error.timestamp))}\n" +
                                    "THREAD: ${error.threadName}\n" +
                                    "EXCEPTION: ${error.exceptionMessage}\n" +
                                    "STACK TRACE:\n${error.stackTrace}"
                        }
                        shareCrashLogs(context, logDump)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)), // Deep Teal styling
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export & Share Logs (${loggedErrors.size} Found)")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        errorDao.clearAllErrors()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Saved Log Records")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(text = "Local Persisted Failures (${loggedErrors.size})", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            if (loggedErrors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No errors captured yet. App is operating normally.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(loggedErrors) { item ->
                        ErrorLogItem(error = item)
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorLogItem(error: LocalErrorEntity) {
    val dateString = remember(error.timestamp) {
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS", Locale.getDefault()).format(Date(error.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Time: $dateString", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(text = "Thread: ${error.threadName}", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onErrorContainer)
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(text = error.exceptionMessage, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = error.stackTrace,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.heightIn(max = 120.dp)
            )
        }
    }
}