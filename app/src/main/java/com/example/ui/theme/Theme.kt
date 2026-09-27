package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
  lightColorScheme(
    primary = PosPrimary,
    onPrimary = PosOnPrimary,
    primaryContainer = PosPrimaryContainer,
    onPrimaryContainer = PosOnPrimaryContainer,
    secondary = PosSecondary,
    onSecondary = PosOnSecondary,
    secondaryContainer = PosSecondaryContainer,
    onSecondaryContainer = PosOnSecondaryContainer,
    tertiary = PosTertiary,
    tertiaryContainer = PosTertiaryContainer,
    onTertiaryContainer = PosOnTertiaryContainer,
    background = PosBackground,
    onBackground = PosOnBackground,
    surface = PosSurface,
    onSurface = PosOnSurface,
    surfaceVariant = PosSurfaceVariant,
    onSurfaceVariant = PosOnSurfaceVariant,
    outline = PosOutline,
    error = PosError
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false, // Set to Light Mode as requested
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = LightColorScheme,
    typography = Typography,
    content = content
  )
}
