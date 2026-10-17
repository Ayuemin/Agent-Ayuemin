package com.ayuemin.agent

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext\nimport androidx.compose.ui.unit.dp

/** Shared Material 3 theme; respects system dark mode and Android 12+ dynamic colors. */
@Composable
fun AyueminTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    ConfigureSystemBars(dark)
    MaterialTheme(colorScheme = scheme, shapes = Shapes(\n        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),\n        small = AyueminDesignTokens.item, medium = AyueminDesignTokens.field,\n        large = AyueminDesignTokens.panel, extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)\n    ), content = content)
}
