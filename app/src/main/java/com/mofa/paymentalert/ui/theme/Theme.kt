package com.mofa.paymentalert.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E9F6E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9F5E7),
    onPrimaryContainer = Color(0xFF064E36),
    secondary = Color(0xFF59788A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE1EDF3),
    onSecondaryContainer = Color(0xFF1F313B),
    tertiary = Color(0xFF7A6FB0),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEDEAF9),
    onTertiaryContainer = Color(0xFF2C2454),
    background = Color(0xFFF5F8F9),
    onBackground = Color(0xFF1B2A32),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B2A32),
    surfaceVariant = Color(0xFFECF1F3),
    onSurfaceVariant = Color(0xFF5C6E77),
    outline = Color(0xFFD0DDE3),
    outlineVariant = Color(0xFFE3ECEF),
    error = Color(0xFFBF3B2B),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFBE4E1),
    onErrorContainer = Color(0xFF5F1408),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PaymentAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        shapes = AppShapes,
        content = content,
    )
}
