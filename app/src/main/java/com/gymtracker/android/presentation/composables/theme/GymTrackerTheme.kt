package com.gymtracker.android.presentation.composables.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gymtracker.android.domain.model.Appearance

private val Green = Color(0xFF38C778)
private val LightColors = lightColorScheme(
    primary = Color(0xFF137840), onPrimary = Color.White,
    primaryContainer = Color(0xFFD9F8E4), background = Color(0xFFF3F5F4),
    surface = Color.White, surfaceVariant = Color(0xFFE8EEEA)
)
private val DarkColors = darkColorScheme(
    primary = Green, onPrimary = Color(0xFF00391C),
    primaryContainer = Color(0xFF17482D), background = Color(0xFF101512),
    surface = Color(0xFF1B221E), surfaceVariant = Color(0xFF29352D)
)
@Composable
fun GymTrackerTheme(appearance: Appearance = Appearance.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (appearance) {
        Appearance.SYSTEM -> isSystemInDarkTheme()
        Appearance.DARK -> true
        Appearance.LIGHT -> false
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
