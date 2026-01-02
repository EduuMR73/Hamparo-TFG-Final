package com.example.hamparo.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Esquema Oscuro
private val DarkColorScheme = darkColorScheme(
    primary = HamparoPrimaryDark,
    secondary = HamparoSecondaryDark,
    tertiary = HamparoPrimaryContainer // Reusamos para consistencia
)

// Esquema Claro (El principal para nuestros usuarios)
private val LightColorScheme = lightColorScheme(
    primary = HamparoPrimary,
    onPrimary = HamparoOnPrimary,
    primaryContainer = HamparoPrimaryContainer,
    secondary = HamparoSecondary,
    onSecondary = HamparoOnSecondary,
    error = HamparoError,
    background = HamparoBackground,
    surface = HamparoSurface
)

@Composable
fun HamparoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color está disponible en Android 12+.
    // Lo desactivamos por defecto para mantener NUESTRA identidad de marca (Colores Hamparo),
    // pero puedes ponerlo a true si prefieres que se adapte al fondo de pantalla del usuario.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Configuración de la barra de estado (Status Bar) para que coincida con la app
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}