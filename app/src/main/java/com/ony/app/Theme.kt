package com.ony.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Colours by role, so light and dark can differ where they must and match where they can. */
data class OnyPalette(
    val page: Color,
    val onPage: Color,      // text on the page
    val pageMuted: Color,
    val outline: Color,     // borders of light rows
    val block: Color,       // the ink blocks
    val blockRaised: Color, // fields and chosen items inside a block
    val onBlock: Color,
    val blockMuted: Color,
    val accent: Color,      // lavender
    val onAccent: Color,
)

/** Ink blocks on a pale page, joined by small bridges, with one soft lavender accent. */
val LightPalette = OnyPalette(
    page = Color(0xFFF8F9F6),
    onPage = Color(0xFF10262A),
    pageMuted = Color(0xFF5F7174),
    outline = Color(0xFF10262A),
    block = Color(0xFF10262A),
    blockRaised = Color(0xFF1C3539),
    onBlock = Color(0xFFEEF2F1),
    blockMuted = Color(0xFF8A9EA3),
    accent = Color(0xFFD3CCF6),
    onAccent = Color(0xFF10262A),
)

/** The same idea at night: a deep ink page, blocks lifted just enough to read as blocks. */
val DarkPalette = OnyPalette(
    page = Color(0xFF0B1517),
    onPage = Color(0xFFEEF2F1),
    pageMuted = Color(0xFF8A9C9F),
    outline = Color(0xFF4F676B),
    block = Color(0xFF1A2E31),
    blockRaised = Color(0xFF26403F),
    onBlock = Color(0xFFEEF2F1),
    blockMuted = Color(0xFF93A7AB),
    accent = Color(0xFFD3CCF6),
    onAccent = Color(0xFF10262A),
)

private val LocalPalette = staticCompositionLocalOf { LightPalette }

/** The current palette's colours, light or dark with the system. */
object Ony {
    val Page: Color @Composable get() = LocalPalette.current.page
    val OnPage: Color @Composable get() = LocalPalette.current.onPage
    val PageMuted: Color @Composable get() = LocalPalette.current.pageMuted
    val Outline: Color @Composable get() = LocalPalette.current.outline
    val Block: Color @Composable get() = LocalPalette.current.block
    val BlockRaised: Color @Composable get() = LocalPalette.current.blockRaised
    val OnBlock: Color @Composable get() = LocalPalette.current.onBlock
    val BlockMuted: Color @Composable get() = LocalPalette.current.blockMuted
    val Accent: Color @Composable get() = LocalPalette.current.accent
    val OnAccent: Color @Composable get() = LocalPalette.current.onAccent
}

// Variable-font weight settings are still marked experimental in Compose.
@OptIn(ExperimentalTextApi::class)
private fun manrope(weight: Int) = Font(
    R.font.manrope,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

private val Manrope = FontFamily(manrope(300), manrope(400), manrope(500), manrope(600), manrope(700))

private val OnyType = Typography(
    // The wordmark on the setup screen: big and thin, the type itself is the header.
    displaySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-1.2).sp),
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    // Small spaced caps for buttons and pills, as in the reference design.
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
)

/** The appearance choice on the setup screen. AUTO follows the phone. */
enum class ThemeMode(val label: String) {
    AUTO("Auto"), LIGHT("Light"), DARK("Dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        AUTO -> systemDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromSaved(name: String?): ThemeMode = entries.firstOrNull { it.name == name } ?: AUTO
    }
}

@Composable
fun OnyTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val p = if (dark) DarkPalette else LightPalette
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.block,
        onPrimary = p.onBlock,
        background = p.page,
        onBackground = p.onPage,
        surface = p.page,
        onSurface = p.onPage,
        onSurfaceVariant = p.pageMuted,
        outline = p.outline,
    )
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(colorScheme = scheme, typography = OnyType, content = content)
    }
}

/** The saved appearance resolved against the phone's own setting. */
@Composable
fun rememberDarkTheme(): Boolean {
    val context = LocalContext.current
    val mode by remember { Prefs.themeMode(context) }.collectAsState(initial = ThemeMode.AUTO)
    return mode.isDark(isSystemInDarkTheme())
}
