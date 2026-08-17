package ir.khoshkshooyi.assistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val AppColorScheme = lightColorScheme(
    primary = Brass,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Gold,
    onPrimaryContainer = Ink,
    secondary = Teal,
    background = BgColor,
    surface = PanelColor,
    onBackground = Ink,
    onSurface = Ink,
    outline = LineColor,
    error = Red
)

// Softer, more contemporary rounded corners across every Material3 component
// (buttons, text fields, dropdowns, dialogs) instead of the sharp defaults.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun KhoshkshooyiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = MaterialTheme.typography,
        shapes = AppShapes,
        content = content
    )
}
