/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.ui.expanded

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.jero009.essentialisland.ui.IslandText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jero009.essentialisland.data.SmartIslandSettings
import com.jero009.essentialisland.model.IslandNotification
import com.jero009.essentialisland.ui.components.BatteryVisual
import com.jero009.essentialisland.ui.components.batteryVisualColor
import com.jero009.essentialisland.ui.components.rememberBatteryPulse
import com.jero009.essentialisland.ui.components.resolveBatteryVisual
import com.jero009.essentialisland.util.runCatchingLogged

/**
 * Minimal Nothing battery card: the percentage on the left, one muted status line, and a flat
 * stadium bar on the right. No badge, no battery glyph, no gradients — nothing theme.
 */
@Composable
fun BatteryExpanded(
    notification: IslandNotification,
    bottomPadding: Dp,
    settings: SmartIslandSettings
) {
    val context = LocalContext.current
    val pctText = notification.text?.replace("%", "")?.trim() ?: "49"
    val pct = pctText.toFloatOrNull() ?: 49f
    val progress = (pct / 100f).coerceIn(0f, 1f)

    val visual = resolveBatteryVisual(notification.category, notification.title, pct)
    val batteryColor = batteryVisualColor(visual, Color(settings.batteryColor))
    val pulse = rememberBatteryPulse(visual)

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
        )
    }

    val statusText = remember(pct, visual) {
        if (visual == BatteryVisual.PluggedIn) {
            "Charger connected"
        } else if (visual == BatteryVisual.PluggedOut) {
            "Running on battery"
        } else if (visual == BatteryVisual.Full) {
            "Battery full"
        } else if (visual == BatteryVisual.Saver) {
            "Power Saver active"
        } else if (visual == BatteryVisual.Low) {
            "Connect charger"
        } else {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val remainingMs = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                runCatchingLogged("BatteryExpanded", "computeChargeTimeRemaining failed") {
                    batteryManager?.computeChargeTimeRemaining() ?: -1L
                } ?: -1L
            } else {
                -1L
            }
            if (remainingMs > 0L) {
                val totalMins = remainingMs / 60000L
                val h = totalMins / 60L
                val m = totalMins % 60L
                if (h > 0) "${h} h ${m} m until full" else "${m} m until full"
            } else {
                val totalMins = ((100f - pct) * 1.5f).toInt()
                val h = totalMins / 60
                val m = totalMins % 60
                if (h > 0) "${h} h ${m} m until full" else "${m} m until full"
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 18.dp, top = 20.dp, end = 18.dp, bottom = bottomPadding)
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: the percentage, big and quiet; one muted status line under it.
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${pct.toInt()}%",
                color = LocalIslandPalette.current.text,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                maxLines = 1
            )
            Text(
                text = statusText,
                color = LocalIslandPalette.current.textMuted,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        // Right: one flat bar. Track = faint white, fill = battery colour (red when low).
        // Low battery pulses; everything else is still — mechanical, Nothing.
        Box(
            modifier = Modifier
                .width(88.dp)
                .height(6.dp)
                .background(LocalIslandPalette.current.tint(0.12f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress.value)
                    .height(6.dp)
                    .graphicsLayer { alpha = if (visual == BatteryVisual.Low) pulse.value else 1f }
                    .background(batteryColor, RoundedCornerShape(3.dp))
            )
        }
    }
}
