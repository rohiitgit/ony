package com.ony.app

import android.content.Context
import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Ink blocks on a pale page, joined by small bridges, with one soft lavender accent.
 * One palette whatever the system theme: the dark blocks carry the contrast.
 */
object Ony {
    val Page = Color(0xFFF8F9F6)
    val Ink = Color(0xFF10262A)
    val InkRaised = Color(0xFF1C3539)
    val OnInk = Color(0xFFEEF2F1)
    val InkMuted = Color(0xFF8A9EA3)
    val PageMuted = Color(0xFF6B7D80)
    val Mist = Color(0xFFD8DEDC)
    val Lavender = Color(0xFFD3CCF6)
    // Stat values on ink: lavender pale enough to read as light.
    val LavenderLight = Color(0xFFDAD6F6)
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
    headlineSmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp),
    // The thin wordmark.
    titleLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Light, fontSize = 25.sp, lineHeight = 30.sp, letterSpacing = (-0.6).sp),
    titleMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    // Small spaced caps for buttons and pills, as in the reference design.
    labelLarge = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.6.sp),
)

private val OnyScheme = lightColorScheme(
    primary = Ony.Ink,
    onPrimary = Ony.OnInk,
    background = Ony.Page,
    onBackground = Ony.Ink,
    surface = Ony.Page,
    onSurface = Ony.Ink,
    onSurfaceVariant = Ony.PageMuted,
    outline = Ony.Ink,
)

@Composable
fun OnyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = OnyScheme, typography = OnyType, content = content)
}

/** True when the user switched animations off in Android settings. */
fun motionReduced(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
