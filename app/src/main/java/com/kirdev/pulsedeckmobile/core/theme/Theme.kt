package com.kirdev.pulsedeckmobile.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colour sets from KiroX Fusion (kirdev07/KiroX-Client-TG, ui/theme/FusionTheme.kt). */
enum class AppTheme(val title: String, val black: Color, val surface: Color, val surface2: Color, val border: Color, val track: Color) {
    AMOLED("AMOLED", Color(0xFF000000), Color(0xFF101010), Color(0xFF191919), Color(0xFF242424), Color(0xFF292929)),
    GRAPHITE("Graphite", Color(0xFF121214), Color(0xFF1C1C1F), Color(0xFF26262A), Color(0xFF323236), Color(0xFF38383D)),
}

/**
 * Monochrome tokens. Backed by Compose state, so switching the theme in Settings recolours
 * every screen at once without restarting the activity.
 */
object Palette {
    var theme by mutableStateOf(AppTheme.AMOLED)
    /** Highlight for the active tab, progress and switches; white by default (monochrome design). */
    var accent by mutableStateOf(Color(0xFFFFFFFF))

    val Black get() = theme.black
    val Surface get() = theme.surface
    val Surface2 get() = theme.surface2
    val Border get() = theme.border
    val Track get() = theme.track
    val Text = Color(0xFFFFFFFF)
    val Text2 = Color(0xFFAAAAAA)
    val Text3 = Color(0xFF888888)
    val Disabled = Color(0xFF454545)
}

/** Accent choices shown in Appearance: whites and greys only, like the mockups. */
val Accents = listOf(Color(0xFFFFFFFF), Color(0xFFE0E0E0), Color(0xFFB8B8B8), Color(0xFF8E8E8E))

private val Type = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Palette.Text),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Palette.Text),
    bodySmall = TextStyle(fontSize = 12.sp, color = Palette.Text2),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp, color = Palette.Text3),
)

/** Monospace digits keep numbers from jumping while they change. */
val Numbers = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Palette.Text)

@Composable
fun PulseDeskTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Palette.accent, onPrimary = Palette.Black,
        secondary = Palette.Text2, onSecondary = Palette.Black,
        background = Palette.Black, onBackground = Palette.Text,
        surface = Palette.Black, onSurface = Palette.Text,
        surfaceVariant = Palette.Surface2, onSurfaceVariant = Palette.Text2,
        surfaceContainer = Palette.Surface, surfaceContainerHigh = Palette.Surface2,
        outline = Palette.Border, error = Palette.Text, onError = Palette.Black,
    )
    MaterialTheme(colorScheme = colors, typography = Type, content = content)
}
