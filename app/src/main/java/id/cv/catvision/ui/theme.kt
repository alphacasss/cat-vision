package id.cv.catvision.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val CatDarkScheme = darkColorScheme(
    primary = Color(0xFFFF7A1A),
    onPrimary = Color(0xFF0F1115),
    primaryContainer = Color(0xFFFFD090),
    onPrimaryContainer = Color(0xFF0F1115),
    secondary = Color(0xFFC9D1DE),
    onSecondary = Color(0xFF0F1115),
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFF2F4F8),
    surface = Color(0xFF1A1D24),
    onSurface = Color(0xFFF2F4F8),
    surfaceVariant = Color(0xFF232732),
    onSurfaceVariant = Color(0xFF9AA4B2),
)

private val CatLightScheme = lightColorScheme(
    primary = Color(0xFFE86A10),
    onPrimary = Color.White,
    secondary = Color(0xFF4A5568),
    onSecondary = Color.White,
    background = Color(0xFFF7F5F0),
    onBackground = Color(0xFF1A1D24),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1D24),
)

@Composable
fun CatVisionTheme(
    content: @Composable () -> Unit
) {
    val colors = if (isSystemInDarkTheme()) CatDarkScheme else CatLightScheme
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
