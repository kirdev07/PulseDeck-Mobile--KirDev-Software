package com.kirdev.pulsedeckmobile.core.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.kirdev.pulsedeckmobile.data.local.Prefs

/**
 * Material 3 colour roles of PulseDesk, the same palette as PulseDeck for Windows
 * (pulse_pc/ui/theme/tokens.py): purple by default, a black-and-white style, dark / light,
 * AMOLED black, and on Android 12+ the colours of the wallpaper (Material You).
 */
data class Roles(
    val dark: Boolean,
    val bg: Color, val s1: Color, val s2: Color, val s3: Color, val s4: Color,
    val on: Color, val onv: Color, val out: Color, val ov: Color,
    val p: Color, val op: Color, val pc: Color, val opc: Color, val sc: Color, val osc: Color,
    val err: Color, val onErr: Color, val ec: Color, val oec: Color,
)

/** Accent choices of «Оформление» (key, name, swatch). «wallpaper» = Material You. */
val AccentChoices = listOf(
    Triple("purple", "Фиолетовый", Color(0xFF7E57C2)), Triple("blue", "Голубой", Color(0xFF1E88E5)),
    Triple("green", "Зелёный", Color(0xFF2E9E57)), Triple("orange", "Оранжевый", Color(0xFFE8833A)),
    Triple("pink", "Розовый", Color(0xFFD9467E)),
)

private data class Tones(val p: Long, val op: Long, val pc: Long, val opc: Long, val sc: Long, val osc: Long)

private val ACCENTS: Map<String, Pair<Tones, Tones>> = mapOf(          // dark, light
    "purple" to (Tones(0xFFD0BCFF, 0xFF381E72, 0xFF4F378B, 0xFFEADDFF, 0xFF4A4458, 0xFFE8DEF8) to
        Tones(0xFF6750A4, 0xFFFFFFFF, 0xFFEADDFF, 0xFF21005D, 0xFFE8DEF8, 0xFF1D192B)),
    "blue" to (Tones(0xFFA8C7FA, 0xFF062E6F, 0xFF0842A0, 0xFFD3E3FD, 0xFF3D4758, 0xFFD7E3F8) to
        Tones(0xFF0B57D0, 0xFFFFFFFF, 0xFFD3E3FD, 0xFF041E49, 0xFFDCE3F2, 0xFF151C27)),
    "green" to (Tones(0xFF8CD9A0, 0xFF00391C, 0xFF00522B, 0xFFA7F5BA, 0xFF384B3C, 0xFFD3E8D6) to
        Tones(0xFF1E6C3B, 0xFFFFFFFF, 0xFFA7F5BA, 0xFF00210E, 0xFFD3E8D6, 0xFF0F1F14)),
    "orange" to (Tones(0xFFFFB77C, 0xFF4D2700, 0xFF6E3900, 0xFFFFDCC2, 0xFF51443A, 0xFFF8DECD) to
        Tones(0xFF8B5000, 0xFFFFFFFF, 0xFFFFDCC2, 0xFF2D1600, 0xFFF5DED0, 0xFF261A12)),
    "pink" to (Tones(0xFFFFB0CB, 0xFF5E1133, 0xFF7B2949, 0xFFFFD9E3, 0xFF5C3F48, 0xFFFFD9E3) to
        Tones(0xFF984061, 0xFFFFFFFF, 0xFFFFD9E2, 0xFF3E001D, 0xFFFFD9E2, 0xFF31111D)),
    "mono" to (Tones(0xFFFFFFFF, 0xFF000000, 0xFF3A3A3A, 0xFFFFFFFF, 0xFF3A3A3A, 0xFFEDEDED) to
        Tones(0xFF111111, 0xFFFFFFFF, 0xFFE2E2E2, 0xFF111111, 0xFFD4D4D4, 0xFF111111)),
)

fun buildRoles(prefs: Prefs, systemDark: Boolean, context: Context?): Roles {
    val dark = when (prefs.mode) { "light" -> false; "system" -> systemDark; else -> true }
    val mono = prefs.style == "mono"
    val wallpaper = !mono && prefs.accentKey == "wallpaper" && Build.VERSION.SDK_INT >= 31 && context != null
    var r = if (wallpaper) fromScheme(if (dark) dynamicDarkColorScheme(context!!) else dynamicLightColorScheme(context!!), dark)
    else {
        val (d, l) = ACCENTS[if (mono) "mono" else prefs.accentKey] ?: ACCENTS.getValue("purple")
        val t = if (dark) d else l
        val n = when {
            dark && mono -> longArrayOf(0xFF121212, 0xFF1A1A1A, 0xFF1F1F1F, 0xFF292929, 0xFF333333, 0xFFEDEDED, 0xFFBDBDBD, 0xFF8E8E8E, 0xFF444444)
            dark -> longArrayOf(0xFF141218, 0xFF1D1B20, 0xFF211F26, 0xFF2B2930, 0xFF36343B, 0xFFE6E0E9, 0xFFCAC4D0, 0xFF938F99, 0xFF49454F)
            mono -> longArrayOf(0xFFFFFFFF, 0xFFF6F6F6, 0xFFF0F0F0, 0xFFE9E9E9, 0xFFE2E2E2, 0xFF111111, 0xFF4A4A4A, 0xFF767676, 0xFFCFCFCF)
            else -> longArrayOf(0xFFFEF7FF, 0xFFF7F2FA, 0xFFF3EDF7, 0xFFECE6F0, 0xFFE6E0E9, 0xFF1D1B20, 0xFF49454F, 0xFF79747E, 0xFFCAC4D0)
        }.map { Color(it) }
        Roles(dark, n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8],
            Color(t.p), Color(t.op), Color(t.pc), Color(t.opc), Color(t.sc), Color(t.osc),
            Color(if (dark) 0xFFFFB4AB else 0xFFB3261E), Color(if (dark) 0xFF690005 else 0xFFFFFFFF),
            Color(if (dark) 0xFF93000A else 0xFFF9DEDC), Color(if (dark) 0xFFFFDAD6 else 0xFF410E0B))
    }
    if (dark && prefs.amoled) {
        r = r.copy(bg = Color.Black, s1 = Color(0xFF0D0D0F), s2 = Color(0xFF141316), s3 = Color(0xFF1C1B1F), s4 = Color(0xFF26252A))
    }
    return r
}

private fun fromScheme(c: ColorScheme, dark: Boolean) = Roles(
    dark, c.surface, c.surfaceContainerLow, c.surfaceContainer, c.surfaceContainerHigh, c.surfaceContainerHighest,
    c.onSurface, c.onSurfaceVariant, c.outline, c.outlineVariant,
    c.primary, c.onPrimary, c.primaryContainer, c.onPrimaryContainer, c.secondaryContainer, c.onSecondaryContainer,
    c.error, c.onError, c.errorContainer, c.onErrorContainer,
)

/**
 * The roles for every screen, backed by Compose state: a change in «Оформление» recolours the
 * app at once. The old names stay ([Black] = the screen, [Text] = the main ink …) so every
 * screen speaks Material 3 without being rewritten.
 */
object Palette {
    var roles by mutableStateOf(buildRoles(Prefs(), true, null))

    val Black get() = roles.bg
    val Surface get() = roles.s2
    val Surface2 get() = roles.s3
    val Border get() = roles.ov
    val Track get() = roles.s4
    val Text get() = roles.on
    val Text2 get() = roles.onv
    val Text3 get() = roles.onv.copy(alpha = 0.78f)
    val Disabled get() = roles.on.copy(alpha = 0.38f)
    val accent get() = roles.p
    val onAccent get() = roles.op
    val Container get() = roles.pc
    val OnContainer get() = roles.opc
    val Tonal get() = roles.sc
    val OnTonal get() = roles.osc
    val Outline get() = roles.out
    val Error get() = roles.err
    val ErrorContainer get() = roles.ec
    val OnErrorContainer get() = roles.oec
    val Ok get() = if (roles.dark) Color(0xFF7BD88F) else Color(0xFF1E7A35)
}

/** Monospace digits keep numbers from jumping while they change. */
val Numbers get() = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Palette.Text)

@Composable
fun PulseDeskTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val roles = remember(prefs.style, prefs.mode, prefs.accentKey, prefs.amoled, systemDark) { buildRoles(prefs, systemDark, context) }
    // Written here so the screens below read the new roles in this same pass; compared without
    // observing, so the theme does not recompose itself once more.
    if (androidx.compose.runtime.snapshots.Snapshot.withoutReadObservation { Palette.roles } != roles) Palette.roles = roles
    val view = LocalView.current
    SideEffect {                                   // dark icons on the status bar in the light theme
        (view.context as? Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !roles.dark
                isAppearanceLightNavigationBars = !roles.dark
            }
        }
    }
    val r = roles
    val scheme = if (r.dark) darkColorScheme(
        primary = r.p, onPrimary = r.op, primaryContainer = r.pc, onPrimaryContainer = r.opc,
        secondary = r.onv, onSecondary = r.bg, secondaryContainer = r.sc, onSecondaryContainer = r.osc,
        background = r.bg, onBackground = r.on, surface = r.bg, onSurface = r.on,
        surfaceVariant = r.s4, onSurfaceVariant = r.onv, surfaceContainerLowest = r.bg, surfaceContainerLow = r.s1,
        surfaceContainer = r.s2, surfaceContainerHigh = r.s3, surfaceContainerHighest = r.s4,
        outline = r.out, outlineVariant = r.ov, error = r.err, onError = r.onErr, errorContainer = r.ec, onErrorContainer = r.oec,
        inverseSurface = r.on, inverseOnSurface = r.bg,
    ) else lightColorScheme(
        primary = r.p, onPrimary = r.op, primaryContainer = r.pc, onPrimaryContainer = r.opc,
        secondary = r.onv, onSecondary = r.bg, secondaryContainer = r.sc, onSecondaryContainer = r.osc,
        background = r.bg, onBackground = r.on, surface = r.bg, onSurface = r.on,
        surfaceVariant = r.s4, onSurfaceVariant = r.onv, surfaceContainerLowest = r.bg, surfaceContainerLow = r.s1,
        surfaceContainer = r.s2, surfaceContainerHigh = r.s3, surfaceContainerHighest = r.s4,
        outline = r.out, outlineVariant = r.ov, error = r.err, onError = r.onErr, errorContainer = r.ec, onErrorContainer = r.oec,
        inverseSurface = r.on, inverseOnSurface = r.bg,
    )
    val type = Typography(
        headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, color = r.on),
        titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, color = r.on),
        titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = r.on),
        bodyLarge = TextStyle(fontSize = 16.sp, color = r.on),
        bodyMedium = TextStyle(fontSize = 14.sp, color = r.on),
        bodySmall = TextStyle(fontSize = 12.sp, color = r.onv),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp, color = r.onv),
    )
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
