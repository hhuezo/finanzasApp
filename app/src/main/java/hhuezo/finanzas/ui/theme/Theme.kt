package hhuezo.finanzas.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightGlassColorScheme = lightColorScheme(
    primary = ElegantBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E2F5),
    onPrimaryContainer = ElegantBlueDeep,
    secondary = ElegantBlueMid,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EAF5),
    onSecondaryContainer = ElegantBlueDeep,
    tertiary = IosTeal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD4F0E2),
    onTertiaryContainer = Color(0xFF0A2E1C),
    background = AppBackground,
    onBackground = OnGlass,
    surface = Color.White,
    onSurface = OnGlass,
    surfaceVariant = Color(0xFFF0F3F8),
    onSurfaceVariant = OnGlassSecondary,
    outline = Color(0xFFC7CCD6),
    outlineVariant = GlassStroke,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainerLowest = Color.White,
)

@Composable
fun FinanzasPersonalesTheme(
    content: @Composable () -> Unit
) {
    // Modo oscuro deshabilitado: siempre esquema claro
    val colorScheme = LightGlassColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            WindowCompat.getInsetsController(activity.window, view).apply {
                // Header azul oscuro: iconos de status bar claros
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
