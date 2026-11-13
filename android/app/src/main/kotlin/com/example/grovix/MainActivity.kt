package com.example.grovix

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.grovix.data.GrovixRepository
import com.example.grovix.ui.screens.LoginScreen
import com.example.grovix.ui.screens.MainContainerScreen
import com.example.grovix.ui.theme.GrovixTheme
import com.example.grovix.util.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        GrovixRepository.init(applicationContext)
        NotificationHelper.initChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        setContent {
            GrovixTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by GrovixRepository.currentUser.collectAsState()

                    if (currentUser == null) {
                        LoginScreen(onLoginSuccess = {})
                    } else {
                        MainContainerScreen(onLogout = {})
                    }
                }
            }
        }
    }
}
