package com.example.offlinetrack

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier


@Composable
fun MyCustomSplashDesign() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary), // Customize with your brand's background color
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        // Place your custom application logo layout or branded designs right here
        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
    }
}

fun shareCrashLogs(context: Context, logText: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, logText)
        type = "text/plain"
    }
    val chooserTitle = context.getString(R.string.share_chooser_title)
    val shareIntent = Intent.createChooser(sendIntent, chooserTitle)
    context.startActivity(shareIntent)
}

