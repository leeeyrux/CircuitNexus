package com.circuitnexus.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

private val CircuitColorScheme = darkColorScheme(
    primary = CircuitColors.Emerald,
    onPrimary = CircuitColors.EmeraldOn,
    primaryContainer = CircuitColors.EmeraldContainer,
    onPrimaryContainer = CircuitColors.OnSurface,
    secondary = CircuitColors.Cyan,
    onSecondary = CircuitColors.CyanOn,
    secondaryContainer = CircuitColors.CyanContainer,
    onSecondaryContainer = CircuitColors.OnSurface,
    tertiary = CircuitColors.Amber,
    onTertiary = CircuitColors.AmberOn,
    tertiaryContainer = CircuitColors.AmberContainer,
    onTertiaryContainer = CircuitColors.OnSurface,
    background = CircuitColors.Background,
    onBackground = CircuitColors.OnBackground,
    surface = CircuitColors.Surface,
    onSurface = CircuitColors.OnSurface,
    surfaceVariant = CircuitColors.SurfaceVariant,
    onSurfaceVariant = CircuitColors.OnSurfaceVariant,
    outline = CircuitColors.Outline,
    error = CircuitColors.Coral,
    onError = CircuitColors.CoralOn,
    errorContainer = CircuitColors.CoralContainer,
    onErrorContainer = CircuitColors.OnSurface
)

/**
 * Tema de CircuitNexus: siempre oscuro (OLED) para cuidar la vista
 * y hacer que los instrumentos se vean bonitos.
 */
@Composable
fun CircuitNexusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.insetsController?.isAppearanceLightStatusBars = false
                window.insetsController?.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = CircuitColorScheme,
        typography = CircuitTypographyInstance,
        shapes = CircuitShapesInstance,
        content = content
    )
}
