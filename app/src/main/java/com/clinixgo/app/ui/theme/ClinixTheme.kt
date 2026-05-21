package com.clinixgo.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val Teal = Color(0xFF0E7C86)
val TealDark = Color(0xFF07565D)
val Mint = Color(0xFFE7F6F3)
val Ink = Color(0xFF152326)
val Coral = Color(0xFFE95D4F)
val Gold = Color(0xFFF6C453)
val Sky = Color(0xFFEAF4FF)
val Surface = Color(0xFFF7FBFA)

private val ClinixColors: ColorScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = Mint,
    onPrimaryContainer = TealDark,
    secondary = Coral,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECE9),
    tertiary = Gold,
    onTertiary = Ink,
    background = Surface,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE8EFEE),
    onSurfaceVariant = Color(0xFF536466),
    error = Color(0xFFB3261E),
    outline = Color(0xFFB8C7C5)
)

@Composable
fun ClinixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClinixColors,
        typography = Typography(
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(6.dp),
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(12.dp),
            extraLarge = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}
