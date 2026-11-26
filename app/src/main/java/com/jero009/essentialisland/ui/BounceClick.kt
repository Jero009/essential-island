/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnClick by rememberUpdatedState(onClick)
    // Nothing OS press feedback (spec): scale 0.97 + opacity 0.85 over 140 ms with the standard
    // mechanical ease — transform and opacity only, no spring overshoot, no colour flash.
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 140,
            easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        ),
        label = "pressScale"
    )
    val pressAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 140,
            easing = androidx.compose.animation.core.CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
        ),
        label = "pressAlpha"
    )

    this
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            alpha = pressAlpha
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    currentOnClick()
                }
            )
        }
}
