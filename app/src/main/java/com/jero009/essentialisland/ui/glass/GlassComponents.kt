/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui.glass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

// Drop-in replacements for Material 3's Card and Switch: the screens import these, so every card picks up the glass
// edge and the glass switch colours.

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalLiquidGlass.current) {
        androidx.compose.material3.Card(
            modifier = modifier.glassEdge(shape, glass = true, dark = LocalGlassDark.current),
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = null,
            content = content
        )
    } else {
        androidx.compose.material3.Card(
            modifier = modifier,
            shape = shape,
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier.glassEdge(shape, glass = LocalLiquidGlass.current, dark = LocalGlassDark.current),
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = if (LocalLiquidGlass.current) null else border,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Switch colours, measured from Nothing OS 5.0 (dark): ON = #F2F2F2 track with a black knob (inversion,
 * never coloured), OFF = a recessed dark track with the mid-grey #929292 knob. Light mode inverts the fill.
 */
@Composable
fun glassSwitchColors(): SwitchColors {
    val dark = LocalGlassDark.current
    return if (dark) {
        SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF000000),
            checkedTrackColor = Color(0xFFF2F2F2),
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color(0xFF929292),
            uncheckedTrackColor = Color.White.copy(alpha = 0.06f),
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedThumbColor = Color(0xFF000000).copy(alpha = 0.38f),
            disabledCheckedTrackColor = Color(0xFFF2F2F2).copy(alpha = 0.38f),
            disabledUncheckedThumbColor = Color(0xFF929292).copy(alpha = 0.38f)
        )
    } else {
        SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Color(0xFF1C1C1C),
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color(0xFF929292),
            uncheckedTrackColor = Color.Black.copy(alpha = 0.06f),
            uncheckedBorderColor = Color.Transparent
        )
    }
}

@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = glassSwitchColors(),
    interactionSource: MutableInteractionSource? = null
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource
    )
}
