package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// PISCES Strictly Monochrome Dark Color Scheme
private val PiscesMonochromeColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    primaryContainer = SurfaceSecondary,
    onPrimaryContainer = PureWhite,
    secondary = TextSecondary,
    onSecondary = PureBlack,
    secondaryContainer = SurfacePrimary,
    onSecondaryContainer = PureWhite,
    tertiary = PureWhite,
    onTertiary = PureBlack,
    background = PureBlack,
    onBackground = PureWhite,
    surface = SurfacePrimary,
    onSurface = PureWhite,
    surfaceVariant = SurfaceSecondary,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorderSubtle,
    error = PureWhite,
    onError = PureBlack,
    errorContainer = SurfaceSecondary,
    onErrorContainer = PureWhite
)

@Composable
fun PiscesTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PiscesMonochromeColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun TradeMirrorTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    PiscesTheme(content = content)
}
