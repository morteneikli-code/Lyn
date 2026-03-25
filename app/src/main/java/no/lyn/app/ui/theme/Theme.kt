package no.lyn.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = LightningYellow,
    onPrimary = StormBlack,
    secondary = ElectricBlue,
    onSecondary = StormBlack,
    tertiary = ElectricPurple,
    background = StormBlack,
    onBackground = TextPrimary,
    surface = StormCard,
    onSurface = TextPrimary,
    surfaceVariant = StormDeep,
    onSurfaceVariant = TextSecondary,
    outline = StormBorder,
    error = DangerRed,
    onError = Color.White,
)

@Composable
fun LynTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
