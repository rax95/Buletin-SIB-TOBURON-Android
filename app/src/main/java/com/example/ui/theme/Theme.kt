package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = SanctuaryPine,
    onPrimary = Color.White,
    primaryContainer = SanctuaryPineContainer,
    onPrimaryContainer = OnSanctuaryPineContainer,
    secondary = HarvestGold,
    onSecondary = Color.White,
    secondaryContainer = HarvestGoldContainer,
    onSecondaryContainer = OnHarvestGoldContainer,
    tertiary = TerracottaClay,
    onTertiary = Color.White,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = SanctuaryCream,
    onBackground = EspressoInk,
    surface = SanctuaryCream,
    onSurface = EspressoInk,
    surfaceVariant = SanctuaryLinen,
    onSurfaceVariant = StoneMuted,
    surfaceContainerLowest = SanctuaryCardLight,
    surfaceContainerLow = Color(0xFFF6F3EC),
    surfaceContainer = SanctuaryLinen,
    surfaceContainerHigh = SanctuarySurfaceHighLight,
    outline = WarmOutlineLight,
    outlineVariant = Color(0xFFE1DCD0),
    error = DangerCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = SanctuaryPineLight,
    onPrimary = SanctuaryPineDeep,
    primaryContainer = SanctuaryPine,
    onPrimaryContainer = SanctuaryPineContainer,
    secondary = HarvestGoldLight,
    onSecondary = OnHarvestGoldContainer,
    secondaryContainer = Color(0xFF5A3712),
    onSecondaryContainer = HarvestGoldContainer,
    tertiary = Color(0xFFF4B1A1),
    onTertiary = OnTerracottaContainer,
    tertiaryContainer = Color(0xFF63291C),
    onTertiaryContainer = TerracottaContainer,
    background = CedarNight,
    onBackground = IvoryInk,
    surface = CedarNight,
    onSurface = IvoryInk,
    surfaceVariant = CedarSurface,
    onSurfaceVariant = SageMuted,
    surfaceContainerLowest = Color(0xFF0A100E),
    surfaceContainerLow = CedarSurface,
    surfaceContainer = CedarCardDark,
    surfaceContainerHigh = CedarSurfaceHighDark,
    outline = WarmOutlineDark,
    outlineVariant = Color(0xFF2B3A35),
    error = DangerCrimsonDark,
    onError = Color(0xFF601410)
)

val BulletinShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun BuletinSibTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = BulletinShapes,
        content = content
    )
}
