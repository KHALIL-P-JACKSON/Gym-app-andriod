package com.gymtracker.android.presentation.composables.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color
import com.gymtracker.android.domain.model.Appearance

private val LightColors = lightColorScheme(
    primary = Color(0xFFAD1457), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E5), onPrimaryContainer = Color(0xFF3E001D),
    secondary = Color(0xFF875065), onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E5), onSecondaryContainer = Color(0xFF351020),
    tertiary = Color(0xFF96583E), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCC), onTertiaryContainer = Color(0xFF351000),
    background = Color(0xFFFFF5F8), onBackground = Color(0xFF281820),
    surface = Color(0xFFFFF8FA), onSurface = Color(0xFF281820),
    surfaceVariant = Color(0xFFF4DEE6), onSurfaceVariant = Color(0xFF58414B),
    surfaceTint = Color(0xFFAD1457),
    outline = Color(0xFF8A707B), outlineVariant = Color(0xFFDCC0CC),
    inverseSurface = Color(0xFF3D2C34), inverseOnSurface = Color(0xFFFFECF2),
    inversePrimary = Color(0xFFFFAFCE)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFAFCE), onPrimary = Color(0xFF62002D),
    primaryContainer = Color(0xFF890C43), onPrimaryContainer = Color(0xFFFFD9E5),
    secondary = Color(0xFFF4B6CD), onSecondary = Color(0xFF4F2537),
    secondaryContainer = Color(0xFF6B3A4D), onSecondaryContainer = Color(0xFFFFD9E5),
    tertiary = Color(0xFFFFB596), onTertiary = Color(0xFF57200B),
    tertiaryContainer = Color(0xFF753820), onTertiaryContainer = Color(0xFFFFDBCC),
    background = Color(0xFF1B1116), onBackground = Color(0xFFF5DEE8),
    surface = Color(0xFF251A20), onSurface = Color(0xFFF5DEE8),
    surfaceVariant = Color(0xFF49333E), onSurfaceVariant = Color(0xFFDCC0CC),
    surfaceTint = Color(0xFFFFAFCE),
    outline = Color(0xFFA58A96), outlineVariant = Color(0xFF58414B),
    inverseSurface = Color(0xFFF5DEE8), inverseOnSurface = Color(0xFF3D2C34),
    inversePrimary = Color(0xFFAD1457)
)
@Composable
fun GymTrackerTheme(appearance: Appearance = Appearance.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (appearance) {
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.DARK -> true
        Appearance.LIGHT -> false
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
