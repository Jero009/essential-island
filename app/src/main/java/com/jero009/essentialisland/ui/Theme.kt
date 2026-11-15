/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.ColorUtils
import com.jero009.essentialisland.R
import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.ui.glass.GlassBackdropColors
import com.jero009.essentialisland.ui.glass.LiquidGlassBackdrop
import com.jero009.essentialisland.ui.glass.LocalGlassBackdropColors
import com.jero009.essentialisland.ui.glass.LocalGlassDark
import com.jero009.essentialisland.ui.glass.LocalLiquidGlass

// The app's theme: Material 3 colour roles, built from one accent colour (or from the wallpaper with Material You),
// drawn either flat (plain Material 3) or as liquid glass (translucent panels over a soft colour backdrop, see
// ui/glass).

private fun hsl(hue: Float, sat: Float, light: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color(ColorUtils.HSLToColor(floatArrayOf(h, sat.coerceIn(0f, 1f), light.coerceIn(0f, 1f))))
}

/**
 * A full Material 3 colour scheme grown from [seed]: primary, secondary (the same hue, calmer) and tertiary (a warm-
 * shifted hue) families plus neutrals that lean slightly towards the seed's hue, so surfaces never look grey next to
 * the accent.
 */
internal fun accentColorScheme(seed: Color, dark: Boolean): ColorScheme {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(seed.toArgb(), hsl)
    val h = hsl[0]
    val s = hsl[1].coerceIn(0.40f, 0.95f)
    val h3 = h + 58f
    // Nothing OS neutrals: surfaces and ink are fixed monochrome values (measured from Nothing OS 5.0); only the
    // primary / secondary / tertiary families stay derived from the accent seed.
    return if (!dark) {
        lightColorScheme(
            primary = hsl(h, s, 0.42f),
            onPrimary = Color.White,
            primaryContainer = hsl(h, s * 0.95f, 0.91f),
            onPrimaryContainer = hsl(h, s, 0.16f),
            secondary = hsl(h, s * 0.40f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h, s * 0.50f, 0.91f),
            onSecondaryContainer = hsl(h, s * 0.55f, 0.15f),
            tertiary = hsl(h3, s * 0.60f, 0.42f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(h3, s * 0.65f, 0.91f),
            onTertiaryContainer = hsl(h3, s * 0.70f, 0.15f),
            background = Color(0xFFF1F1F1),
            onBackground = Color(0xFF0A0A0A),
            surface = Color(0xFFFCFCFC),
            onSurface = Color(0xFF0A0A0A),
            surfaceVariant = Color(0xFFF1F1F1),
            onSurfaceVariant = Color.Black.copy(alpha = 0.5f),
            surfaceTint = hsl(h, s, 0.42f),
            outline = Color.Black.copy(alpha = 0.14f),
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
        // Nothing OS dark surfaces: true-black page, one grey surface family (#1B1B1B), white-ink hierarchy,
        // hairline borders (white 12% strong / 8% soft). Only the accent families stay colour-derived.
        darkColorScheme(
            primary = hsl(h, s, 0.74f),
            onPrimary = hsl(h, s, 0.14f),
            primaryContainer = hsl(h, s * 0.60f, 0.26f),
            onPrimaryContainer = hsl(h, s * 0.75f, 0.90f),
            secondary = hsl(h, s * 0.45f, 0.76f),
            onSecondary = hsl(h, s * 0.5f, 0.14f),
            secondaryContainer = hsl(h, s * 0.35f, 0.25f),
            onSecondaryContainer = hsl(h, s * 0.50f, 0.90f),
            tertiary = hsl(h3, s * 0.55f, 0.76f),
            onTertiary = hsl(h3, s * 0.5f, 0.14f),
            tertiaryContainer = hsl(h3, s * 0.40f, 0.26f),
            onTertiaryContainer = hsl(h3, s * 0.55f, 0.90f),
            background = Color(0xFF000000),
            onBackground = Color.White,
            surface = Color(0xFF1B1B1B),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF1B1B1B),
            onSurfaceVariant = Color.White.copy(alpha = 0.55f),
            surfaceTint = hsl(h, s, 0.74f),
            outline = Color.White.copy(alpha = 0.12f),
            outlineVariant = Color.White.copy(alpha = 0.08f),
            surfaceBright = Color(0xFF2A2A2A),
            surfaceDim = Color(0xFF0A0A0A),
            surfaceContainerLowest = Color(0xFF0F0F0F),
            surfaceContainerLow = Color(0xFF161616),
            surfaceContainer = Color(0xFF1B1B1B),
            surfaceContainerHigh = Color(0xFF242424),
            surfaceContainerHighest = Color(0xFF2C2C2C)
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
    /** Liquid glass look (translucent panels over a colour backdrop). Off gives plain Material 3. */
    liquidGlass: Boolean = SmartIslandSettings.Default.liquidGlassTheme,
    /** Colours from the phone's wallpaper (Android 12+). Off builds the palette from [accentColor]. */
    materialYou: Boolean = SmartIslandSettings.Default.materialYou,
    /** The accent colour (ARGB) the palette is built from when Material You is off. */
    accentColor: Long = SmartIslandSettings.Default.accentColor,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val useDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> darkTheme
    }
    val solid = remember(materialYou, accentColor, useDark, context) {
        if (materialYou && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            accentColorScheme(Color(accentColor.toInt()), useDark)
        }
    }
    val colorScheme = if (liquidGlass) solid.asGlass(useDark) else solid

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
            LocalLiquidGlass provides liquidGlass,
            LocalGlassDark provides useDark,
            LocalGlassBackdropColors provides remember(solid) {
                GlassBackdropColors(solid.background, solid.primary, solid.tertiary, solid.secondary)
            }
        ) {
            if (liquidGlass) {
                Box(Modifier.fillMaxSize()) {
                    LiquidGlassBackdrop(
                        base = solid.background,
                        glowA = solid.primary,
                        glowB = solid.tertiary,
                        glowC = solid.secondary,
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
