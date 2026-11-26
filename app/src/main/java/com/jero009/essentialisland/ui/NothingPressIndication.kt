/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU General Public License v3 - see LICENSE
 */

package com.jero009.essentialisland.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The Nothing OS press treatment as one global indication (provided through [LocalIndication] in
 * [SmartIslandTheme] and in the overlay composition): press feedback is scale 0.97 + opacity 0.85
 * over 140 ms with the standard mechanical ease — transform and opacity only, never a ripple
 * colour flash (nothing-os-design spec, components.md "Press feedback").
 *
 * Used by every `Modifier.clickable` that does not pass its own indication, in the app screens and
 * on the island's expanded cards, so rows, tiles, chips and mode buttons share one press language.
 */
object NothingPressIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        NothingPressNode(interactionSource)

    override fun equals(other: Any?): Boolean = other === this
    override fun hashCode(): Int = javaClass.hashCode()
}

/** Press scale (0.97) and opacity (0.85) from the spec's press-feedback rule. */
private const val PRESS_SCALE = 0.97f
private const val PRESS_ALPHA = 0.85f
private const val PRESS_MS = 140
private val PressEase = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val PressSpec = tween<Float>(durationMillis = PRESS_MS, easing = PressEase)

private class NothingPressNode(
    private val interactionSource: InteractionSource
) : Modifier.Node(), DrawModifierNode {

    private val scale = Animatable(1f)
    private val alpha = Animatable(1f)
    private val layerPaint = Paint()
    private var pressed = 0
    private var collect: Job? = null

    override fun onAttach() {
        collect = coroutineScope.launch {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> pressed++
                    is PressInteraction.Release, is PressInteraction.Cancel -> pressed--
                    else -> {}
                }
                pressed = pressed.coerceAtLeast(0)
                val down = pressed > 0
                // The node scope carries the composition's frame clock; if it ever does not,
                // snap instead of crashing — a missing press animation must never break a tap.
                try {
                    scale.animateTo(if (down) PRESS_SCALE else 1f, PressSpec)
                    alpha.animateTo(if (down) PRESS_ALPHA else 1f, PressSpec)
                } catch (expected: IllegalStateException) {
                    scale.snapTo(if (down) PRESS_SCALE else 1f)
                    alpha.snapTo(if (down) PRESS_ALPHA else 1f)
                }
            }
        }
    }

    override fun onDetach() {
        collect?.cancel()
        collect = null
    }

    override fun ContentDrawScope.draw() {
        val s = scale.value
        val a = alpha.value
        if (s >= 0.999f && a >= 0.999f) {
            drawContent()
            return
        }
        // Alpha through a saveLayer (this Compose version has no drawContext.alpha);
        // scale around the centre. Transform + opacity only, per the spec.
        layerPaint.alpha = a
        drawContext.canvas.saveLayer(Rect(0f, 0f, size.width, size.height), layerPaint)
        drawContext.transform.scale(
            scaleX = s,
            scaleY = s,
            pivot = Offset(size.width / 2f, size.height / 2f)
        )
        drawContent()
        drawContext.canvas.restore()
    }
}
