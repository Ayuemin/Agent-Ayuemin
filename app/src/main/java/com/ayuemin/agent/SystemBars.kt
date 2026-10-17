package com.ayuemin.agent

import android.app.Activity
import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.material3.MaterialTheme
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/** Dedicated Android system-bar policy for the Compose UI. */
@Composable
fun ConfigureSystemBars(darkTheme: Boolean) {
    val view = LocalView.current
    val background = MaterialTheme.colorScheme.background.toArgb()
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = background
        window.navigationBarColor = background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
