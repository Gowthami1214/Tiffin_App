package com.example.tiffinapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tiffinapp.ui.navigation.AppNavigation
import com.example.tiffinapp.ui.theme.TiffinAppTheme

/**
 * Single Activity — hosts the entire Compose UI tree.
 *
 * All navigation, state management, and screen orchestration is handled by
 * [AppNavigation] and the shared [com.example.tiffinapp.ui.viewmodel.TiffinViewModel].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TiffinAppTheme {
                AppNavigation()
            }
        }
    }
}
