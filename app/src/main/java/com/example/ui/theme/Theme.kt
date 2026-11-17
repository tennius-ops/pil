package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SeniorIvoryColorScheme =
  lightColorScheme(
    primary = MedGreen,
    onPrimary = Color.White,
    primaryContainer = MedGreenLightSurface,
    onPrimaryContainer = MedGreenDark,
    secondary = WaterBlue,
    onSecondary = Color.White,
    secondaryContainer = WaterBlueSurface,
    onSecondaryContainer = WaterBlueDark,
    background = IvoryBackground,
    onBackground = InkPrimary,
    surface = CreamSurface,
    onSurface = InkPrimary,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = InkSecondary,
    outline = WarmBorder,
  )

@Composable
fun SeniorHealthTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = SeniorIvoryColorScheme,
    typography = SeniorTypography,
    content = content,
  )
}
