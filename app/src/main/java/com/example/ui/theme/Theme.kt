package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkDefaultScheme = darkColorScheme(
    primary = AccentDefault,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    background = ElegantDarkBackground,
    onBackground = Color(0xFFE6E1E5),
    surface = ElegantDarkBackground,
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = ElegantDarkSurface,
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFF49454F)
)

private val LightDefaultScheme = lightColorScheme(
    primary = Color(0xFF00AA50),
    secondary = Color(0xFF00B0FF),
    tertiary = Color(0xFF7E57C2),
    background = Color(0xFFFAFAFA),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColorName: String = "Default",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Base color schemes
    val baseScheme = if (darkTheme) DarkDefaultScheme else LightDefaultScheme

    // Inject user selected custom accent
    val finalScheme = when (accentColorName) {
        "Blue" -> baseScheme.copy(primary = AccentBlue, secondary = AccentBlue.copy(alpha = 0.8f))
        "Green" -> baseScheme.copy(primary = AccentGreen, secondary = AccentGreen.copy(alpha = 0.8f))
        "Red" -> baseScheme.copy(primary = AccentCoral, secondary = AccentCoral.copy(alpha = 0.8f))
        "Purple" -> baseScheme.copy(primary = AccentPurple, secondary = AccentPurple.copy(alpha = 0.8f))
        "Orange" -> baseScheme.copy(primary = AccentOrange, secondary = AccentOrange.copy(alpha = 0.8f))
        else -> { // Default / Pixel
            if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                baseScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = Typography,
        content = content
    )
}
