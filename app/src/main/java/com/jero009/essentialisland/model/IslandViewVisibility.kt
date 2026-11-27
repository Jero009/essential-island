/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.jero009.essentialisland.model

import com.jero009.essentialisland.data.SmartIslandSettings

/**
 * Pure visibility decision for the overlay's island view, shared between window creation
 * ([ensureCollapsedWindow]) and every layout update ([updateWindowLayoutParams]).
 *
 * Lock-screen hide and exempt-app Off hide everything, exactly as before. The pure landscape
 * case is the only one that changed: with [SmartIslandSettings.showInLandscape] off, the idle
 * pill/cutout stays hidden, but while [SmartIslandSettings.showNotificationCardsInLandscape]
 * is on and at least one waking card is on the island, the view stays visible so the card(s)
 * can show — and hides again once the last one is gone. Waking cards are app notifications and
 * incoming calls only ([LandscapeSurfacing.wakeCount]); device-state cards never surface the
 * hidden island. The count is ignored in portrait.
 */
fun isIslandViewHidden(
    settings: SmartIslandSettings,
    isLocked: Boolean,
    isLandscape: Boolean,
    landscapeWakeCount: Int,
    exemptMode: ExemptMode
): Boolean {
    if (!settings.showOnLockScreen && isLocked) return true
    if (exemptMode == ExemptMode.Off) return true
    if (isLandscape && !settings.showInLandscape) {
        return !(settings.showNotificationCardsInLandscape && landscapeWakeCount > 0)
    }
    return false
}
