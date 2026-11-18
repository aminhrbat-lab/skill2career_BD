package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NearMeDarkColorScheme = darkColorScheme(
    primary = NearMePrimaryBlue,
    onPrimary = NearMeTextMain,
    primaryContainer = NearMeSurfaceElevated,
    onPrimaryContainer = NearMeElectricBlue,
    secondary = NearMeElectricBlue,
    onSecondary = NearMeBackground,
    secondaryContainer = NearMeSecondaryBg,
    onSecondaryContainer = NearMeTextMain,
    tertiary = NearMeAccentPurple,
    onTertiary = NearMeTextMain,
    background = NearMeBackground,
    onBackground = NearMeTextMain,
    surface = NearMeSecondaryBg,
    onSurface = NearMeTextMain,
    surfaceVariant = NearMeSurfaceElevated,
    onSurfaceVariant = NearMeTextSecondary,
    outline = NearMeBorder,
    outlineVariant = NearMeBorder,
    error = NearMeDanger,
    onError = NearMeTextMain
)

val NearMeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NearMeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NearMeDarkColorScheme,
        typography = NearMeTypography,
        shapes = NearMeShapes,
        content = content
    )
}
