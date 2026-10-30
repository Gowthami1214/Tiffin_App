package com.example.tiffinapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TiffinLightColors = lightColorScheme(
    primary          = TiffinOrange,
    onPrimary        = androidx.compose.ui.graphics.Color.White,
    primaryContainer = TiffinGold,
    secondary        = TiffinOrangeDk,
    surface          = TiffinSurface,
    background       = TiffinSurface
)

private val TiffinDarkColors = darkColorScheme(
    primary          = TiffinGold,
    onPrimary        = androidx.compose.ui.graphics.Color.Black,
    primaryContainer = TiffinOrange,
    secondary        = TiffinOrangeDk
)

@Composable
fun TiffinAppTheme(
    darkTheme:    Boolean = isSystemInDarkTheme(),
    // Dynamic colour disabled so Tiffin brand orange is always applied
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> TiffinDarkColors
        else      -> TiffinLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Use WindowCompat insets controller — avoids the deprecated statusBarColor API
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
