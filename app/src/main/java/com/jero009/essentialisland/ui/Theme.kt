/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.jero009.essentialisland.R
import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.ui.glass.GlassBackdropColors
import com.jero009.essentialisland.ui.glass.LiquidGlassBackdrop
import com.jero009.essentialisland.ui.glass.LocalGlassBackdropColors
import com.jero009.essentialisland.ui.glass.LocalGlassDark
import com.jero009.essentialisland.ui.glass.LocalLiquidGlass

// The app's theme: a Nothing OS colour system. One monochrome token set (measured from Nothing OS 5.0 —
// see docs in THIRD_PARTY_FONTS.md / README): true-black page, a single grey surface family, white-ink
// hierarchy, hairline borders. Active states are inversions (primary = white fill / black content), never
// coloured. Red exists only as the error/signal channel. The choosable accent (Nothing Yellow by default)
// travels outside the scheme in [LocalNothingAccent] and may colour progress / selected fills only.
// Dynamic colour (Material You) is intentionally not used: a wallpaper-derived palette would break the
// predominantly monochrome Nothing character.

/** The choosable expression accent (Nothing Yellow #FFC700 by default). Progress / selected fills only. */
val LocalNothingAccent = staticCompositionLocalOf { Color(0xFFFFC700) }

/** Convenience accessor for the accent in composable default arguments and bodies. */
@Composable
fun nothingAccent(): Color = LocalNothingAccent.current

/** Geist Mono — numerals / mono readouts (SIL OFL 1.1, bundled). A variable font (wght axis). */
val NothingMonoFontFamily = FontFamily(
    Font(R.font.geist_mono_variable, FontWeight.Normal),
    Font(R.font.geist_mono_variable, FontWeight.Medium),
    Font(R.font.geist_mono_variable, FontWeight.SemiBold),
    Font(R.font.geist_mono_variable, FontWeight.Bold)
)

// Nothing OS signal red: errors, destructive actions, needs-attention. Never chrome, never decoration.
internal val NothingRed = Color(0xFFD71921)

/**
 * The Nothing OS colour scheme. Dark (measured): page #000000, surfaces #1B1B1B / elevated #1F1F1F / #242424,
 * ink white at 100% / 55% / dim, hairline borders white 12% strong and 8% soft, switch-knob mid grey #929292.
 * Light: page #F1F1F1, cards #FCFCFC, ink #0A0A0A. Every "control" role is a greyscale inversion, so M3
 * switches, buttons and checkboxes render as Nothing inversions without per-screen work.
 */
internal fun nothingColorScheme(dark: Boolean): ColorScheme {
    return if (!dark) {
        lightColorScheme(
            primary = Color(0xFF0A0A0A),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE2E2E2),
            onPrimaryContainer = Color(0xFF0A0A0A),
            secondary = Color(0xFF0A0A0A),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE7E7E7),
            onSecondaryContainer = Color(0xFF0A0A0A),
            tertiary = Color(0xFF0A0A0A),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFE2E2E2),
            onTertiaryContainer = Color(0xFF0A0A0A),
            error = NothingRed,
            onError = Color.White,
            errorContainer = Color(0xFF3A0F12),
            onErrorContainer = Color(0xFFFFD9D9),
            background = Color(0xFFF1F1F1),
            onBackground = Color(0xFF0A0A0A),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF0A0A0A),
            surfaceVariant = Color(0xFFF1F1F1),
            onSurfaceVariant = Color.Black.copy(alpha = 0.55f),
            surfaceTint = Color(0xFFFCFCFC),
            outline = Color(0xFF737373),
            outlineVariant = Color.Black.copy(alpha = 0.08f),
            surfaceBright = Color.White,
            surfaceDim = Color(0xFFDCDCDC),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF4F4F4),
            surfaceContainer = Color(0xFFF1F1F1),
            surfaceContainerHigh = Color(0xFFE7E7E7),
            surfaceContainerHighest = Color(0xFFE2E2E2)
        )
    } else {
        darkColorScheme(
            // Active-state inversion: primary is the ON / selected fill (white) with black content.
            primary = Color.White,
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF242424),
            onPrimaryContainer = Color.White,
            secondary = Color.White,
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF242424),
            onSecondaryContainer = Color.White,
            tertiary = Color.White,
            onTertiary = Color.Black,
            tertiaryContainer = Color(0xFF242424),
            onTertiaryContainer = Color.White,
            error = NothingRed,
            onError = Color.White,
            errorContainer = Color(0xFF3A0F12),
            onErrorContainer = Color(0xFFFFD9D9),
            background = Color(0xFF000000),
            onBackground = Color.White,
            surface = Color(0xFF1B1B1B),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1B1B1B),
            onSurfaceVariant = Color.White.copy(alpha = 0.55f),
            surfaceTint = Color(0xFF1B1B1B),
            // Mid grey: the OFF switch knob and text-field borders (measured #929292).
            outline = Color(0xFF929292),
            outlineVariant = Color.White.copy(alpha = 0.08f),
            surfaceBright = Color(0xFF242424),
            surfaceDim = Color(0xFF0A0A0A),
            surfaceContainerLowest = Color(0xFF0F0F0F),
            surfaceContainerLow = Color(0xFF161616),
            surfaceContainer = Color(0xFF1B1B1B),
            surfaceContainerHigh = Color(0xFF1F1F1F),
            surfaceContainerHighest = Color(0xFF242424)
        )
    }
}

/**
 * The app's UI typeface: Geist (SIL OFL 1.1, bundled), the Nothing OS stand-in for system Roboto.
 * A variable font — Compose applies the wght axis for each declared weight.
 */
private val GeistUiFamily = FontFamily(
    Font(R.font.geist_variable, FontWeight.Normal),
    Font(R.font.geist_variable, FontWeight.Medium),
    Font(R.font.geist_variable, FontWeight.SemiBold),
    Font(R.font.geist_variable, FontWeight.Bold)
)

/** Material 3 type scale with every role set to Geist. */
private fun geistTypography(): Typography {
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = GeistUiFamily),
        displayMedium = base.displayMedium.copy(fontFamily = GeistUiFamily),
        displaySmall = base.displaySmall.copy(fontFamily = GeistUiFamily),
        headlineLarge = base.headlineLarge.copy(fontFamily = GeistUiFamily),
        headlineMedium = base.headlineMedium.copy(fontFamily = GeistUiFamily),
        headlineSmall = base.headlineSmall.copy(fontFamily = GeistUiFamily),
        titleLarge = base.titleLarge.copy(fontFamily = GeistUiFamily),
        titleMedium = base.titleMedium.copy(fontFamily = GeistUiFamily),
        titleSmall = base.titleSmall.copy(fontFamily = GeistUiFamily),
        bodyLarge = base.bodyLarge.copy(fontFamily = GeistUiFamily),
        bodyMedium = base.bodyMedium.copy(fontFamily = GeistUiFamily),
        bodySmall = base.bodySmall.copy(fontFamily = GeistUiFamily),
        labelLarge = base.labelLarge.copy(fontFamily = GeistUiFamily),
        labelMedium = base.labelMedium.copy(fontFamily = GeistUiFamily),
        labelSmall = base.labelSmall.copy(fontFamily = GeistUiFamily)
    )
}

/**
 * The glass version of a scheme: the same colours, but the page background is clear (the backdrop is drawn behind
 * it), the panels (surface, surfaceVariant, surfaceContainer) are see-through so the backdrop tints them, and the
 * roles that dialogs, menus and sheets sit on stay nearly solid, so text over a dimmed screen stays readable.
 * The scheme is monochrome, so the frost stays neutral grey — Nothing OS 5 never tints app pages.
 */
internal fun ColorScheme.asGlass(dark: Boolean): ColorScheme = copy(
    background = Color.Transparent,
    surface = surface.copy(alpha = if (dark) 0.58f else 0.62f),
    surfaceVariant = surfaceVariant.copy(alpha = if (dark) 0.50f else 0.55f),
    surfaceContainerLowest = surfaceContainerLowest.copy(alpha = 0.50f),
    surfaceContainerLow = surfaceContainerLow.copy(alpha = 0.58f),
    surfaceContainer = surfaceContainer.copy(alpha = if (dark) 0.72f else 0.78f),
    surfaceContainerHigh = surfaceContainerHigh.copy(alpha = 0.95f),
    surfaceContainerHighest = surfaceContainerHighest.copy(alpha = 0.96f),
    outline = if (dark) Color.White.copy(alpha = 0.34f) else Color.Black.copy(alpha = 0.22f),
    outlineVariant = if (dark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.10f)
)

@Composable
fun SmartIslandTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** "system" follows [darkTheme]; "light" / "dark" force one. */
    themeMode: String = "system",
    /** Liquid glass look (translucent panels over a neutral backdrop). Off gives the flat Nothing look. */
    liquidGlass: Boolean = SmartIslandSettings.Default.liquidGlassTheme,
    /** Unused: dynamic (wallpaper) colour is disabled — the Nothing scheme is monochrome by design. */
    @Suppress("UNUSED_PARAMETER") materialYou: Boolean = SmartIslandSettings.Default.materialYou,
    /** The choosable accent (ARGB) for progress / selected fills. Nothing Yellow by default. */
    accentColor: Long = SmartIslandSettings.Default.accentColor,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    val colorScheme = remember(useDark) { nothingColorScheme(useDark).let { if (liquidGlass) it.asGlass(useDark) else it } }
    val accent = remember(accentColor) { Color(accentColor.toInt()) }

    // Keep the status / navigation bar icons readable when the chosen theme differs from the phone's.
    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (context as? android.app.Activity)?.window ?: return@SideEffect
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !useDark
            controller.isAppearanceLightNavigationBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = remember { geistTypography() }
    ) {
        CompositionLocalProvider(
            LocalNothingAccent provides accent,
            LocalLiquidGlass provides liquidGlass,
            LocalGlassDark provides useDark,
            // Neutral frost: the glows are plain white, so the glass backdrop stays monochrome.
            LocalGlassBackdropColors provides remember(colorScheme) {
                GlassBackdropColors(
                    base = if (useDark) Color(0xFF000000) else Color(0xFFF1F1F1),
                    glowA = Color.White.copy(alpha = 0.10f),
                    glowB = Color.White.copy(alpha = 0.06f),
                    glowC = Color.White.copy(alpha = 0.08f)
                )
            }
        ) {
            if (liquidGlass) {
                Box(Modifier.fillMaxSize()) {
                    LiquidGlassBackdrop(
                        base = if (useDark) Color(0xFF000000) else Color(0xFFF1F1F1),
                        glowA = Color.White.copy(alpha = 0.10f),
                        glowB = Color.White.copy(alpha = 0.06f),
                        glowC = Color.White.copy(alpha = 0.08f),
                        dark = useDark
                    )
                    content()
                }
            } else {
                content()
            }
        }
    }
}
